/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.docker.provider;

import io.fabric8.api.Container;
import io.fabric8.api.ContainerAutoScaler;
import io.fabric8.api.ContainerAutoScalerFactory;
import io.fabric8.api.ContainerProvider;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.CreationStateListener;
import io.fabric8.api.EnvironmentVariables;
import io.fabric8.api.FabricRequirements;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileRequirements;
import io.fabric8.api.ProfileService;
import io.fabric8.api.Profiles;
import io.fabric8.api.Version;
import io.fabric8.api.ZkDefs;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.common.util.Strings;
import io.fabric8.container.process.JavaContainerConfig;
import io.fabric8.container.process.JolokiaAgentHelper;
import io.fabric8.container.process.ZooKeeperPublishConfig;
import io.fabric8.docker.api.Docker;
import io.fabric8.docker.api.DockerApiConnectionException;
import io.fabric8.docker.api.DockerFactory;
import io.fabric8.docker.api.Dockers;
import io.fabric8.docker.api.container.ContainerConfig;
import io.fabric8.docker.api.container.ContainerCreateStatus;
import io.fabric8.docker.api.container.HostConfig;
import io.fabric8.docker.provider.javacontainer.JavaContainerOptions;
import io.fabric8.docker.provider.javacontainer.JavaDockerContainerImageBuilder;
import io.fabric8.service.child.ChildConstants;
import io.fabric8.service.child.ChildContainers;
import io.fabric8.zookeeper.utils.ZooKeeperMasterCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
@Component(name = "io.fabric8.container.provider.docker", label = "Fabric8 Docker Container Provider", policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = true)
@Service(ContainerProvider.class)
@Properties(
        @Property(name = "fabric.container.protocol", value = DockerConstants.SCHEME)
)
public final class DockerContainerProvider extends AbstractComponent implements ContainerProvider<CreateDockerContainerOptions, CreateDockerContainerMetadata>, ContainerAutoScalerFactory {

    private static final transient Logger LOG = LoggerFactory.getLogger(DockerContainerProvider.class);


    @Reference
    private Configurer configurer;

    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    @Reference(referenceInterface = CuratorFramework.class, bind = "bindCurator", unbind = "unbindCurator")
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

    @Reference(referenceInterface = MBeanServer.class, bind = "bindMBeanServer", unbind = "unbindMBeanServer")
    private MBeanServer mbeanServer;

    @Property(name = "jolokiaKeepAlivePollTime", longValue = 10000,
            label = "The Jolokia Keep Alive Timer Poll Period", description = "The number of milliseconds after which the jolokia agents for any docker containers which expose jolokia will be polled to check for the container status and discover any container resources.")
    private long jolokiaKeepAlivePollTime = 10000;

    private ZooKeeperMasterCache zkMasterCache;

    private ObjectName objectName;
    private DockerFacade mbean;
    private DockerFactory dockerFactory = new DockerFactory();
    private Docker docker;
    private int externalPortCounter;
    private final Object portLock = new Object();

    private final ExecutorService downloadExecutor = Executors.newSingleThreadExecutor();
    private Timer keepAliveTimer;
    private Map<String,CreateDockerContainerMetadata> jolokiaKeepAliveContainers = new ConcurrentHashMap<String, CreateDockerContainerMetadata>();

    public static CreateDockerContainerMetadata newInstance(ContainerConfig containerConfig, ContainerCreateStatus status) {
        List<String> warnings = new ArrayList<String>();
        String[] warningArray = status.getWarnings();
        if (warningArray != null) {
            Collections.addAll(warnings, warningArray);
        }
        return new CreateDockerContainerMetadata(status.getId(), warnings);
    }


    @Activate
    void activate(Map<String, ?> configuration) throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
        updateConfiguration(configuration);
        activateComponent();
        if (mbeanServer != null) {
            objectName = new ObjectName("io.fabric8:type=Docker");
            mbean = new DockerFacade(this);
            if (!mbeanServer.isRegistered(objectName)) {
                mbeanServer.registerMBean(mbean, objectName);
            }
        } else {
            LOG.warn("No MBeanServer!");
        }
    }

    @Modified
    void modified(Map<String, ?> configuration) {
        updateConfiguration(configuration);
    }

    @Deactivate
    void deactivate() throws MBeanRegistrationException, InstanceNotFoundException {
        if (mbeanServer != null) {
            if (mbeanServer.isRegistered(objectName)) {
                mbeanServer.unregisterMBean(objectName);
            }
        }
        if (zkMasterCache != null) {
            zkMasterCache = null;
        }
        deactivateComponent();
    }

    private void updateConfiguration(Map<String, ?> configuration) {
        Object url = configuration.get("url");
        if (url != null) {
            dockerFactory.setAddress(url.toString());
        }
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            // Resteasy uses the TCCL to load the API
            Thread.currentThread().setContextClassLoader(Docker.class.getClassLoader());
            this.docker = dockerFactory.createDocker();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    FabricService getFabricService() {
        return fabricService.get();
    }

    @Override
    public CreateDockerContainerOptions.Builder newBuilder() {
        return CreateDockerContainerOptions.builder();
    }

    @Override
    public CreateDockerContainerMetadata create(CreateDockerContainerOptions options, CreationStateListener listener) throws Exception {
        assertValid();

        String containerId = options.getName();
        ContainerConfig containerConfig = createContainerConfig(options);

        // allow values to be extracted from the profile configuration
        // such as the image
        Set<String> profileIds = options.getProfiles();
        String versionId = options.getVersion();
        FabricService service = fabricService.get();
        Map<String, String> configOverlay = new HashMap<>();
        Map<String, String> ports = null;
        Map<String, String> dockerProviderConfig = new HashMap<>();


        List<Profile> profileOverlays = new ArrayList<>();
        Version version = null;
        if (profileIds != null && versionId != null) {
            ProfileService profileService = fabricService.get().adapt(ProfileService.class);
            version = profileService.getVersion(versionId);
            if (version != null) {
                for (String profileId : profileIds) {
                    Profile profile = version.getRequiredProfile(profileId);
                    if (profile != null) {
                        Profile overlay = profileService.getOverlayProfile(profile);
                        profileOverlays.add(overlay);
                        Map<String, String> dockerConfig = overlay.getConfiguration(DockerConstants.DOCKER_PROVIDER_PID);
                        if (dockerConfig != null) {
                            configOverlay.putAll(dockerConfig);
                        }
                        if (ports == null || ports.size() == 0) {
                            ports = overlay.getConfiguration(ChildConstants.PORTS_PID);
                        }
                    }
                }
                if (version.hasProfile(DockerConstants.DOCKER_PROVIDER_PROFILE_ID)) {
                    Profile profile = version.getRequiredProfile(DockerConstants.DOCKER_PROVIDER_PROFILE_ID);
                    if (profile != null) {
                        Profile overlay = profileService.getOverlayProfile(profile);
                        Map<String, String> dockerConfig = overlay.getConfiguration(DockerConstants.DOCKER_PROVIDER_PID);
                        if (dockerConfig != null) {
                            dockerProviderConfig.putAll(dockerConfig);
                        }
                    }
                }
            }
        }
        if (ports == null || ports.size() == 0) {
            // lets find the defaults from the docker profile
            if (version == null) {
                version = service.getRequiredDefaultVersion();
            }
            Profile dockerProfile = version.getRequiredProfile("docker");
            ports = dockerProfile.getConfiguration(ChildConstants.PORTS_PID);
            if (ports == null || ports.size() == 0) {
                LOG.warn("Could not a docker ports configuration for: " + ChildConstants.PORTS_PID);
                ports = new HashMap<String, String>();
            }
        }
        LOG.info("Got port configuration: " + ports);

        Map<String, String> environmentVariables = ChildContainers.getEnvironmentVariables(service, options);

        DockerProviderConfig configOverlayDockerProvider = createDockerProviderConfig(configOverlay, environmentVariables);

        String image = JolokiaAgentHelper.substituteVariableExpression(containerConfig.getImage(), environmentVariables, service, curator.getOptional(), true);

        if (Strings.isNullOrBlank(image)) {
            image = configOverlayDockerProvider.getImage();
            if (Strings.isNullOrBlank(image)) {
                DockerProviderConfig dockerProviderConfigObject = createDockerProviderConfig(dockerProviderConfig, environmentVariables);
                image = dockerProviderConfigObject.getImage();
            }
            if (Strings.isNullOrBlank(image)) {
                image = System.getenv(DockerConstants.EnvironmentVariables.FABRIC8_DOCKER_DEFAULT_IMAGE);
            }
            if (Strings.isNullOrBlank(image)) {
                image = DockerConstants.DEFAULT_IMAGE;
            }
            containerConfig.setImage(image);
        }
        String containerType = "docker " + image;
        Container container = service.getContainer(containerId);
        if (container != null) {
            container.setType(containerType);
        }


        String[] cmd = containerConfig.getCmd();
        if (cmd == null || cmd.length == 0) {
            String value = configOverlayDockerProvider.getCmd();
            if (Strings.isNullOrBlank(value)) {
                cmd = null;
            } else {
                cmd = new String[]{value};
            }
            containerConfig.setCmd(cmd);
        }

        Map<String, Integer> internalPorts = options.getInternalPorts();
        Map<String, Integer> externalPorts = options.getExternalPorts();

        Map<String, Object> exposedPorts = new HashMap<>();
        Set<Integer> usedPortByHost = findUsedPortByHostAndDocker();
        Map<String, String> emptyMap = new HashMap<>();

        SortedMap<Integer, String> sortedInternalPorts = new TreeMap<>();
        for (Map.Entry<String, String> portEntry : ports.entrySet()) {
            String portName = portEntry.getKey();
            String portText = portEntry.getValue();
            if (portText != null && !Strings.isNullOrBlank(portText)) {
                Integer port = null;
                try {
                    port = Integer.parseInt(portText);
                } catch (NumberFormatException e) {
                    LOG.warn("Ignoring bad port number for " + portName + " value '" + portText + "' in PID: " + ChildConstants.PORTS_PID);
                }
                if (port != null) {
                    sortedInternalPorts.put(port, portName);
                    internalPorts.put(portName, port);
                    exposedPorts.put(portText + "/tcp", emptyMap);
                } else {
                    LOG.info("No port for " + portName);
                }
            }
        }

        String dockerHost = dockerFactory.getDockerHost();
        String jolokiaUrl = null;

        Map<String, String> javaContainerConfig = Profiles.getOverlayConfiguration(service, profileIds, versionId, ChildConstants.JAVA_CONTAINER_PID);
        JavaContainerConfig javaConfig = new JavaContainerConfig();
        configurer.configure(javaContainerConfig, javaConfig);

        boolean isJavaContainer = ChildContainers.isJavaContainer(getFabricService(), options);

        // lets create the ports in sorted order
        for (Map.Entry<Integer, String> entry : sortedInternalPorts.entrySet()) {
            Integer port = entry.getKey();
            String portName = entry.getValue();
            int externalPort = createExternalPort(containerId, portName, usedPortByHost, options);
            externalPorts.put(portName, externalPort);
            environmentVariables.put("FABRIC8_" + portName + "_PORT", "" + port);
            environmentVariables.put("FABRIC8_" + portName + "_PROXY_PORT", "" + externalPort);

            if (portName.equals(JolokiaAgentHelper.JOLOKIA_PORT_NAME)) {
                jolokiaUrl = "http://" + dockerHost + ":" + externalPort + "/jolokia/";
                LOG.info("Found Jolokia URL: " + jolokiaUrl);

                JolokiaAgentHelper.substituteEnvironmentVariables(javaConfig, environmentVariables, isJavaContainer, JolokiaAgentHelper.getJolokiaPortOverride(port),  JolokiaAgentHelper.getJolokiaAgentIdOverride(getFabricService().getEnvironment()));
            } else {
                JolokiaAgentHelper.substituteEnvironmentVariables(javaConfig, environmentVariables, isJavaContainer, JolokiaAgentHelper.getJolokiaAgentIdOverride(getFabricService().getEnvironment()));

            }
        }
        javaConfig.updateEnvironmentVariables(environmentVariables, isJavaContainer);


        LOG.info("Passing in manual ip: " + dockerHost);
        environmentVariables.put(EnvironmentVariables.FABRIC8_MANUALIP, dockerHost);
        if (container != null) {
            container.setManualIp(dockerHost);
        }
        environmentVariables.put(EnvironmentVariables.FABRIC8_GLOBAL_RESOLVER, ZkDefs.MANUAL_IP);
        environmentVariables.put(EnvironmentVariables.FABRIC8_FABRIC_ENVIRONMENT, DockerConstants.SCHEME);

        // now the environment variables are all set lets see if we need to make a custom image
        String libDir = configOverlayDockerProvider.getJavaLibraryPath();
        String homeDir = configOverlayDockerProvider.getHomePath();
        if (!Strings.isNullOrBlank(libDir)) {
            if (container != null) {
                container.setProvisionResult("preparing");
                container.setAlive(true);
            }
            String imageRepository = configOverlayDockerProvider.getImageRepository();
            String entryPoint = configOverlayDockerProvider.getImageEntryPoint();
            List<String> names = new ArrayList<String>(profileIds);
            names.add(versionId);
            String tag = "fabric8-" + Strings.join(names, "-").replace('.', '-');

            JavaDockerContainerImageBuilder builder = new JavaDockerContainerImageBuilder();
            JavaContainerOptions javaContainerOptions = new JavaContainerOptions(image, imageRepository, tag, libDir, homeDir, entryPoint);

            String actualImage = builder.generateContainerImage(service, container, profileOverlays, docker, javaContainerOptions, javaConfig, options, downloadExecutor, environmentVariables);
            containerConfig.setImage(actualImage);
        }


        List<String> env = containerConfig.getEnv();
        if (env == null) {
            env = new ArrayList<>();
        }
        Dockers.addEnvironmentVariablesToList(env, environmentVariables);
        containerConfig.setExposedPorts(exposedPorts);
        containerConfig.setEnv(env);

        String name = options.getName();

        LOG.info("Creating container on docker: " + getDockerAddress() + " name: " + name + " env vars: " + env);
        LOG.info("Creating container with config: " + containerConfig);

        ContainerCreateStatus status = null;
        CreateDockerContainerMetadata metadata = null;
        try {
            status = docker.containerCreate(containerConfig, name);
            LOG.info("Got status: " + status);
            options = options.updateManualIp(dockerHost);

            metadata = newInstance(containerConfig, status);
            metadata.setContainerName(containerId);
            metadata.setContainerType(containerType);
            metadata.setOverridenResolver(ZkDefs.MANUAL_IP);
            metadata.setCreateOptions(options);

            publishZooKeeperValues(options, environmentVariables);

            if (jolokiaUrl != null) {
                metadata.setJolokiaUrl(jolokiaUrl);
                startJolokiaKeepAlive(metadata);
            }
        } catch (Exception e) {
            LOG.info("Failed to create container " + name + " from config " + containerConfig
                    + ": " + e + Dockers.dockerErrorMessage(e), e);
            throw e;
        }
        startDockerContainer(status.getId(), options);
        return metadata;
    }

    protected DockerProviderConfig createDockerProviderConfig(Map<String, String> dockerProviderConfig, Map<String, String> environmentVariables) throws Exception {
        FabricService service = fabricService.get();
        JolokiaAgentHelper.substituteEnvironmentVariableExpressions(dockerProviderConfig, environmentVariables, service, curator.getOptional());
        DockerProviderConfig dockerProviderConfigObject = new DockerProviderConfig();
        configurer.configure(dockerProviderConfig, dockerProviderConfigObject);
        return dockerProviderConfigObject;
    }

    protected void publishZooKeeperValues(CreateDockerContainerOptions options, Map<String, String> environmentVariables) {
        Map<String, Map<String, String>> publishConfigurations = Profiles.getOverlayFactoryConfigurations(fabricService.get(), options.getProfiles(), options.getVersion(), ZooKeeperPublishConfig.PROCESS_CONTAINER_ZK_PUBLISH_PID);
        Set<Map.Entry<String, Map<String, String>>> entries = publishConfigurations.entrySet();
        for (Map.Entry<String, Map<String, String>> entry : entries) {
            String configName = entry.getKey();
            Map<String, String> exportConfig = entry.getValue();

            if (exportConfig != null && !exportConfig.isEmpty()) {
                JolokiaAgentHelper.substituteEnvironmentVariableExpressions(exportConfig, environmentVariables, fabricService.get(), curator.get(), true);
                ZooKeeperPublishConfig config = new ZooKeeperPublishConfig();
                try {
                    configurer.configure(exportConfig, config);
                    config.publish(curator.get(), null, null, null, environmentVariables);
                } catch (Exception e) {
                    LOG.warn("Failed to publish configuration " + configName + " of " + config + " due to: " + e, e);
                }
            }
        }
    }

    @Override
    public void start(Container container) {
        assertValid();
        CreateDockerContainerMetadata containerMetadata = getContainerMetadata(container);
        CreateDockerContainerOptions options = containerMetadata.getCreateOptions();

        String id = getDockerContainerId(container);
        startDockerContainer(id, options);
    }

    protected ContainerConfig createContainerConfig(CreateDockerContainerOptions options) {
        ContainerConfig containerConfig = new ContainerConfig();
        containerConfig.setImage(options.getImage());
        List<String> cmdList = options.getCmd();
        if (cmdList != null && cmdList.size() > 0) {
            containerConfig.setCmd(cmdList.toArray(new String[cmdList.size()]));
        }
        containerConfig.setEntrypoint(options.getEntrypoint());
        String workingDir = options.getWorkingDir();
        if (workingDir != null) {
            containerConfig.setWorkingDir(workingDir);
        }
        containerConfig.setAttachStdout(true);
        containerConfig.setAttachStderr(true);
        containerConfig.setTty(true);
        return containerConfig;
    }

    protected int createExternalPort(String containerId, String portKey, Set<Integer> usedPortByHost, CreateDockerContainerOptions options) {
        synchronized (portLock) {
            while (true) {
                if (externalPortCounter <= 0) {
                    externalPortCounter = options.getMinimumPort();
                    if (externalPortCounter == 0) {
                        externalPortCounter = DockerConstants.DEFAULT_EXTERNAL_PORT;
                    }
                } else {
                    externalPortCounter++;
                }
                if (!usedPortByHost.contains(externalPortCounter)) {
                    Container container = getFabricService().getCurrentContainer();
                    String pid = ChildConstants.PORTS_PID;
                    String key = containerId + "-" + portKey;
                    getFabricService().getPortService().registerPort(container, pid, key, externalPortCounter);
                    return externalPortCounter;
                }
            }
        }
    }

    protected void startDockerContainer(String id, CreateDockerContainerOptions options) {
        if (!Strings.isNullOrBlank(id)) {
            HostConfig hostConfig = new HostConfig();

            Map<String, Integer> externalPorts = options.getExternalPorts();
            Map<String, Integer> internalPorts = options.getInternalPorts();

            SortedMap<Integer, List<Map<String, String>>> sortedPortsToBinding = new TreeMap<>();
            for (Map.Entry<String, Integer> entry : internalPorts.entrySet()) {
                String portName = entry.getKey();
                Integer internalPort = entry.getValue();
                Integer externalPort = externalPorts.get(portName);
                if (internalPort != null && externalPort != null) {
                    sortedPortsToBinding.put(internalPort, createNewPortConfig(externalPort));
                }
            }

            // now lets add the bindings in port order
            Map<String, List<Map<String, String>>> portBindings = new LinkedHashMap<>();
            for (Map.Entry<Integer, List<Map<String, String>>> entry : sortedPortsToBinding.entrySet()) {
                Integer internalPort = entry.getKey();
                List<Map<String, String>> value = entry.getValue();
                portBindings.put("" + internalPort + "/tcp", value);
            }

            hostConfig.setPortBindings(portBindings);
            LOG.info("starting container " + id + " with " + hostConfig);
            try {
                docker.containerStart(id, hostConfig);
            } catch (Exception e) {
                LOG.error("Failed to start container " + id + " with " + hostConfig + " " + e + Dockers.dockerErrorMessage(e), e);
                throw e;
            }
        }
    }

    protected List<Map<String, String>> createNewPortConfig(int port) {
        List<Map<String, String>> answer = new ArrayList<Map<String, String>>();
        Map<String, String> map = new HashMap<String, String>();
        answer.add(map);
        map.put("HostPort", "" + port);
        return answer;
    }

    protected Set<Integer> findUsedPortByHostAndDocker() {
        try {
            FabricService fabric = getFabricService();
            Container currentContainer = fabric.getCurrentContainer();
            Set<Integer> usedPorts;
            Set<Integer> dockerPorts;
            synchronized (portLock) {
                usedPorts = fabric.getPortService().findUsedPortByHost(currentContainer);
                dockerPorts = Dockers.getUsedPorts(docker);
            }
            usedPorts.addAll(dockerPorts);
            return usedPorts;
        } catch (DockerApiConnectionException e) {
            String suggestion = String.format("Can't connect to the Docker server. Are you sure a Docker server is running at %s?", dockerFactory.getAddress());
            throw new DockerApiConnectionException(suggestion, e.getCause());
        }
    }

    @Override
    public void stop(Container container) {
        assertValid();
        String id = getDockerContainerId(container);
        if (!Strings.isNullOrBlank(id)) {
            LOG.info("stopping container " + id);
            CreateDockerContainerMetadata metadata = getContainerMetadata(container);
            if (metadata != null) {
                stopJolokiaKeepAlive(metadata);
            }

            Integer timeToWait = null;
            try {
                docker.containerStop(id, timeToWait);
            } catch (final Exception e) {
                LOG.info("Could not stop container " + id + ": " + e + Dockers.dockerErrorMessage(e), e);
                throw e;
            }
            container.setProvisionResult(Container.PROVISION_STOPPED);
        }
    }

    @Override
    public void destroy(Container container) {
        assertValid();
        String id = getDockerContainerId(container);
        if (!Strings.isNullOrBlank(id)) {
            LOG.info("destroying container " + id);
            Integer removeVolumes = 1;
            try {
                docker.containerRemove(id, removeVolumes);
            } catch (Exception e) {
                LOG.info("Docker container probably does not exist: " + e + Dockers.dockerErrorMessage(e), e);
            }
        }
    }


    protected synchronized void startJolokiaKeepAlive(CreateDockerContainerMetadata metadata) {
        LOG.info("Starting Jolokia Keep Alive for " + metadata.getId());
        jolokiaKeepAliveContainers.put(metadata.getId(), metadata);
        if (keepAliveTimer == null) {
            keepAliveTimer = new Timer("fabric8-docker-container-keepalive");

            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    List<CreateDockerContainerMetadata> list = new ArrayList<>(jolokiaKeepAliveContainers.values());
                    for (CreateDockerContainerMetadata containerMetadata : list) {
                        try {
                            String jolokiaUrl = containerMetadata.getJolokiaUrl();
                            String containerName = containerMetadata.getContainerName();
                            JolokiaAgentHelper.jolokiaKeepAliveCheck(zkMasterCache, getFabricService(), jolokiaUrl, containerName);

                        } catch (Exception e) {
                            LOG.warn("Jolokia keep alive check failed for container " + containerMetadata.getId() + ". " + e, e);
                        }
                    }
                }
            };
            keepAliveTimer.schedule(timerTask, jolokiaKeepAlivePollTime, jolokiaKeepAlivePollTime);
        }
    }

    protected void stopJolokiaKeepAlive(CreateDockerContainerMetadata metadata) {
        LOG.info("Stopping Jolokia Keep Alive for " + metadata.getId());
        jolokiaKeepAliveContainers.remove(metadata.getId());
    }

    @Override
    public String getScheme() {
        assertValid();
        return DockerConstants.SCHEME;
    }

    @Override
    public Class<CreateDockerContainerOptions> getOptionsType() {
        assertValid();
        return CreateDockerContainerOptions.class;
    }

    @Override
    public Class<CreateDockerContainerMetadata> getMetadataType() {
        assertValid();
        return CreateDockerContainerMetadata.class;
    }

    CuratorFramework getCuratorFramework() {
        return curator.get();
    }

    public Docker getDocker() {
        return docker;
    }

    protected String getDockerContainerId(Container container) {
        CreateDockerContainerMetadata containerMetadata = getContainerMetadata(container);
        if (containerMetadata != null) {
            return containerMetadata.getId();
        }
        return container.getId();
    }

    protected static CreateDockerContainerMetadata getContainerMetadata(Container container) {
        CreateContainerMetadata<?> value = container.getMetadata();
        if (value instanceof CreateDockerContainerMetadata) {
            return (CreateDockerContainerMetadata) value;
        } else {
            return null;
        }
    }

    @Override
    public ContainerAutoScaler createAutoScaler(FabricRequirements requirements, ProfileRequirements profileRequirements) {
        return new DockerAutoScaler(this);
    }


    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
        zkMasterCache = new ZooKeeperMasterCache(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }

    void bindConfigurer(Configurer configurer) {
        this.configurer = configurer;
    }

    void unbindConfigurer(Configurer configurer) {
        this.configurer = null;
    }

    void bindMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    void unbindMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = null;
    }


    public String getDockerAddress() {
        return dockerFactory.getAddress();
    }
}
