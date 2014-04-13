/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.fabric8.docker.provider;

import io.fabric8.api.Container;
import io.fabric8.api.ContainerAutoScaler;
import io.fabric8.api.ContainerAutoScalerFactory;
import io.fabric8.api.ContainerProvider;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.CreationStateListener;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.common.util.Objects;
import io.fabric8.docker.api.Docker;
import io.fabric8.docker.api.DockerFactory;
import io.fabric8.docker.api.Dockers;
import io.fabric8.docker.api.container.ContainerConfig;
import io.fabric8.docker.api.container.ContainerCreateStatus;
import io.fabric8.docker.api.container.HostConfig;
import io.fabric8.docker.provider.javacontainer.JavaContainerOptions;
import io.fabric8.docker.provider.javacontainer.javaContainerImageBuilder;
import io.fabric8.utils.PasswordEncoder;
import io.fabric8.utils.Strings;
import io.fabric8.zookeeper.ZkDefs;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.codehaus.jackson.map.ObjectMapper;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMX;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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

@ThreadSafe
@Component(name = "io.fabric8.container.provider.docker", label = "Fabric8 Docker Container Provider", policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = true)
@Service(ContainerProvider.class)
public final class DockerContainerProvider extends AbstractComponent implements ContainerProvider<CreateDockerContainerOptions, CreateDockerContainerMetadata>, ContainerAutoScalerFactory {

    private static final transient Logger LOG = LoggerFactory.getLogger(DockerContainerProvider.class);

    @Reference(referenceInterface = FabricService.class, bind = "bindFabricService", unbind = "unbindFabricService")
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    @Reference(referenceInterface = MBeanServer.class)
    private MBeanServer mbeanServer;

    @Property(name = "jolokiaKeepAlivePollTime", longValue = 10000,
            label = "The Jolokia Keep Alive Timer Poll Period", description = "The number of milliseconds after which the jolokia agents for any docker containers which expose jolokia will be polled to check for the container status and discover any container resources.")
    private long jolokiaKeepAlivePollTime = 10000;


    private ObjectName objectName;
    private DockerFacade mbean;
    private DockerFactory dockerFactory = new DockerFactory();
    private Docker docker;
    private int externalPortCounter;

    private final ExecutorService downloadExecutor = Executors.newSingleThreadExecutor();
    private Timer keepAliveTimer;
    private Map<String,CreateDockerContainerMetadata> jolokiaKeepAliveContainers = new ConcurrentHashMap<String, CreateDockerContainerMetadata>();
    private ObjectMapper jolokiaMapper = new ObjectMapper();

    public static CreateDockerContainerMetadata newInstance(ContainerConfig containerConfig, ContainerCreateStatus status) {
        List<String> warnings = new ArrayList<String>();
        String[] warningArray = status.getWarnings();
        if (warningArray != null) {
            for (String warning : warningArray) {
                warnings.add(warning);
            }
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
        Set<String> profiles = options.getProfiles();
        String versionId = options.getVersion();
        FabricService service = fabricService.get();
        Map<String, String> configOverlay = new HashMap<String, String>();
        Map<String, String> envVarsOverlay = new HashMap<String, String>();
        Map<String, String> ports = null;
        Map<String, String> dockerProviderConfig = new HashMap<String, String>();


        List<Profile> profileOverlays = new ArrayList<Profile>();
        Version version = null;
        if (profiles != null && versionId != null) {
            version = service.getVersion(versionId);
            if (version != null) {
                for (String profileId : profiles) {
                    Profile profile = version.getProfile(profileId);
                    if (profile != null) {
                        Profile overlay = profile.getOverlay();
                        profileOverlays.add(overlay);
                        Map<String, String> dockerConfig = overlay.getConfiguration(DockerConstants.DOCKER_PROVIDER_PID);
                        if (dockerConfig != null) {
                            configOverlay.putAll(dockerConfig);
                        }
                        Map<String, String> envVars = overlay.getConfiguration(DockerConstants.ENVIRONMENT_VARIABLES_PID);
                        if (envVars != null) {
                            envVarsOverlay.putAll(envVars);
                        }
                        if (ports == null || ports.size() == 0) {
                            ports = overlay.getConfiguration(DockerConstants.PORTS_PID);
                        }
                    }
                }
                if (version.hasProfile(DockerConstants.DOCKER_PROVIDER_PROFILE_ID)) {
                    Profile profile = version.getProfile(DockerConstants.DOCKER_PROVIDER_PROFILE_ID);
                    if (profile != null) {
                        Map<String, String> dockerConfig = profile.getOverlay().getConfiguration(DockerConstants.DOCKER_PROVIDER_PID);
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
                version = service.getDefaultVersion();
            }
            Profile dockerProfile = version.getProfile("docker");
            ports = dockerProfile.getConfiguration(DockerConstants.PORTS_PID);
            if (ports == null || ports.size() == 0) {
                LOG.warn("Could not a docker ports configuration for: " + DockerConstants.PORTS_PID);
                ports = new HashMap<String, String>();
            }
        }
        LOG.info("Got port configuration: " + ports);
        String image = containerConfig.getImage();
        if (Strings.isNullOrBlank(image)) {
            image = configOverlay.get(DockerConstants.PROPERTIES.IMAGE);
            if (Strings.isNullOrBlank(image)) {
                image = System.getenv(DockerConstants.ENV_VARS.FABRIC8_DOCKER_DEFAULT_IMAGE);
            }
            if (Strings.isNullOrBlank(image)) {
                image = dockerProviderConfig.get(DockerConstants.PROPERTIES.IMAGE);
            }
            if (Strings.isNullOrBlank(image)) {
                image = DockerConstants.DEFAULT_IMAGE;
            }
            containerConfig.setImage(image);
        }

        String libDir = configOverlay.get(DockerConstants.PROPERTIES.JAVA_LIBRARY_PATH);
        if (!Strings.isNullOrBlank(libDir)) {
            String imageRepository = configOverlay.get(DockerConstants.PROPERTIES.IMAGE_REPOSITORY);
            List<String> names = new ArrayList<String>(profiles);
            names.add(versionId);
            String tag = "fabric8-" + Strings.join(names, "-").replace('.', '-');

            javaContainerImageBuilder builder = new javaContainerImageBuilder();
            JavaContainerOptions javaContainerOptions = new JavaContainerOptions(image, imageRepository, tag, libDir);
            Profile overlayProfile = service.getCurrentContainer().getOverlayProfile();

            String actualImage = builder.generateContainerImage(service, profileOverlays, docker, javaContainerOptions, downloadExecutor, envVarsOverlay);
            containerConfig.setImage(actualImage);
        }

        String[] cmd = containerConfig.getCmd();
        if (cmd == null || cmd.length == 0) {
            String value = configOverlay.get(DockerConstants.PROPERTIES.CMD);
            if (Strings.isNullOrBlank(value)) {
                cmd = null;
            } else {
                cmd = new String[]{value};
            }
            containerConfig.setCmd(cmd);
        }

        String zookeeperUrl = service.getZookeeperUrl();
        String zookeeperPassword = service.getZookeeperPassword();
        if (zookeeperPassword != null) {
            zookeeperPassword = PasswordEncoder.encode(zookeeperPassword);
        }


        String localIp = service.getCurrentContainer().getLocalIp();
        if (!Strings.isNullOrBlank(localIp)) {
            int idx = zookeeperUrl.lastIndexOf(':');
            if (idx > 0) {
                localIp += zookeeperUrl.substring(idx);
            }
            zookeeperUrl = localIp;
        }

        envVarsOverlay.put(DockerConstants.ENV_VARS.KARAF_NAME, options.getName());
        if (!options.isEnsembleServer()) {
            if (envVarsOverlay.get(DockerConstants.ENV_VARS.ZOOKEEPER_URL) == null) {
                envVarsOverlay.put(DockerConstants.ENV_VARS.ZOOKEEPER_URL, zookeeperUrl);
            }
            if (envVarsOverlay.get(DockerConstants.ENV_VARS.ZOOKEEPER_PASSWORD) == null) {
                envVarsOverlay.put(DockerConstants.ENV_VARS.ZOOKEEPER_PASSWORD, zookeeperPassword);
            }
            if (envVarsOverlay.get(DockerConstants.ENV_VARS.ZOOKEEPER_PASSWORD_ENCODE) == null) {
                String zkPasswordEncode = System.getProperty("zookeeper.password.encode", "true");
                envVarsOverlay.put(DockerConstants.ENV_VARS.ZOOKEEPER_PASSWORD_ENCODE, zkPasswordEncode);
            }
        }
        List<String> env = containerConfig.getEnv();
        if (env == null) {
            env = new ArrayList<String>();
        }
        Set<Map.Entry<String, String>> entries = envVarsOverlay.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null && value != null) {
                env.add(key + "=" + value);
            }
        }

        Map<String, Object> exposedPorts = new HashMap<String, Object>();
        Set<Integer> usedPortByHost = findUsedPortByHostAndDocker();
        Map<String, Integer> internalPorts = options.getInternalPorts();
        Map<String, Integer> externalPorts = options.getExternalPorts();
        Map<String, String> emptyMap = new HashMap<String, String>();

        SortedMap<Integer, String> sortedInternalPorts = new TreeMap<Integer, String>();
        for (Map.Entry<String, String> portEntry : ports.entrySet()) {
            String portName = portEntry.getKey();
            String portText = portEntry.getValue();
            if (portText != null && !Strings.isNullOrBlank(portText)) {
                Integer port = null;
                try {
                    port = Integer.parseInt(portText);
                } catch (NumberFormatException e) {
                    LOG.warn("Ignoring bad port number for " + portName + " value '" + portText + "' in PID: " + DockerConstants.PORTS_PID);
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

        // lets create the ports in sorted order
        for (Map.Entry<Integer, String> entry : sortedInternalPorts.entrySet()) {
            Integer port = entry.getKey();
            String portName = entry.getValue();
            int externalPort = createExternalPort(containerId, portName, usedPortByHost, options);
            externalPorts.put(portName, externalPort);
            env.add("FABRIC8_" + portName + "_PORT=" + port);
            env.add("FABRIC8_" + portName + "_PROXY_PORT=" + externalPort);

            if (portName.equals(DockerConstants.JOLOKIA_PORT_NAME)) {
                jolokiaUrl = "http://" + dockerHost + ":" + externalPort + "/jolokia/";
                LOG.info("Found Jolokia URL: " + jolokiaUrl);
            }
        }

        LOG.info("Passing in manual ip: " + dockerHost);
        env.add(DockerConstants.ENV_VARS.FABRIC8_MANUALIP + "=" + dockerHost);
        env.add(DockerConstants.ENV_VARS.FABRIC8_GLOBAL_RESOLVER + "=" + ZkDefs.MANUAL_IP);
        env.add(DockerConstants.ENV_VARS.FABRIC8_FABRIC_ENVIRONMENT + "=" + DockerConstants.SCHEME);
        containerConfig.setExposedPorts(exposedPorts);
        containerConfig.setEnv(env);

        String name = options.getName();

        LOG.info("Creating container on docker: " + getDockerAddress() + " name: " + name + " env vars: " + env);
        LOG.info("Creating container with config: " + containerConfig);

        ContainerCreateStatus status = docker.containerCreate(containerConfig, name);
        LOG.info("Got status: " + status);
        options = options.updateManualIp(dockerHost);

        CreateDockerContainerMetadata metadata = newInstance(containerConfig, status);
        metadata.setContainerName(containerId);
        metadata.setOverridenResolver(ZkDefs.MANUAL_IP);
        metadata.setCreateOptions(options);
        if (jolokiaUrl != null) {
            metadata.setJolokiaUrl(jolokiaUrl);
            startJolokiaKeepAlive(metadata);
        }
        startDockerContainer(status.getId(), options);
        return metadata;
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
                String pid = DockerConstants.PORTS_PID;
                String key = containerId + "-" + portKey;
                getFabricService().getPortService().registerPort(container, pid, key, externalPortCounter);
                return externalPortCounter;
            }
        }
    }

    protected void startDockerContainer(String id, CreateDockerContainerOptions options) {
        if (!Strings.isNullOrBlank(id)) {
            HostConfig hostConfig = new HostConfig();

            Map<String, Integer> externalPorts = options.getExternalPorts();
            Map<String, Integer> internalPorts = options.getInternalPorts();

            SortedMap<Integer, List<Map<String, String>>> sortedPortsToBinding = new TreeMap<Integer, List<Map<String, String>>>();
            for (Map.Entry<String, Integer> entry : internalPorts.entrySet()) {
                String portName = entry.getKey();
                Integer internalPort = entry.getValue();
                Integer externalPort = externalPorts.get(portName);
                if (internalPort != null && externalPort != null) {
                    sortedPortsToBinding.put(internalPort, createNewPortConfig(externalPort));
                }
            }

            // now lets add the bindings in port order
            Map<String, List<Map<String, String>>> portBindings = new LinkedHashMap<String, List<Map<String, String>>>();
            for (Map.Entry<Integer, List<Map<String, String>>> entry : sortedPortsToBinding.entrySet()) {
                Integer internalPort = entry.getKey();
                List<Map<String, String>> value = entry.getValue();
                portBindings.put("" + internalPort + "/tcp", value);
            }

            hostConfig.setPortBindings(portBindings);
            LOG.info("starting container " + id + " with " + hostConfig);
            docker.containerStart(id, hostConfig);
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
        FabricService fabric = getFabricService();
        Container currentContainer = fabric.getCurrentContainer();
        Set<Integer> usedPorts = fabric.getPortService().findUsedPortByHost(currentContainer);
        Set<Integer> dockerPorts = Dockers.getUsedPorts(docker);
        usedPorts.addAll(dockerPorts);
        return usedPorts;
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
            docker.containerStop(id, timeToWait);
        }
    }

    @Override
    public void destroy(Container container) {
        assertValid();
        String id = getDockerContainerId(container);
        if (!Strings.isNullOrBlank(id)) {
            LOG.info("destroying container " + id);
            Integer removeVolumes = 1;
            docker.containerRemove(id, removeVolumes);
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
                    List<CreateDockerContainerMetadata> list = new ArrayList<CreateDockerContainerMetadata>(jolokiaKeepAliveContainers.values());
                    for (CreateDockerContainerMetadata containerMetadata : list) {
                        try {
                            jolokiaKeepAliveCheck(getFabricService(), containerMetadata);
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

    protected void jolokiaKeepAliveCheck(FabricService fabric, CreateDockerContainerMetadata metadata) {
        String jolokiaUrl = metadata.getJolokiaUrl();
        String containerName = metadata.getContainerName();
        LOG.debug("Performing keep alive jolokia check on " + containerName + " URL: " + jolokiaUrl);
        Container container = fabric.getContainer(containerName);
        if (Strings.isNullOrBlank(jolokiaUrl) || container == null) return;


        String user = fabric.getZooKeeperUser();
        String password = fabric.getZookeeperPassword();
        String url = jolokiaUrl;
        int idx = jolokiaUrl.indexOf("://");
        if (idx > 0) {
            url = "http://" + user + ":" + password + "@" + jolokiaUrl.substring(idx + 3);
        }
        if (!url.endsWith("/")) {
            url += "/";
        }
        url += "list/?maxDepth=1";

        List<String> jmxDomains = new ArrayList<String>();
        boolean valid = false;
        try {
            URL theUrl = new URL(url);
            JsonNode jsonNode = jolokiaMapper.readTree(theUrl);
            if (jsonNode != null) {
                JsonNode value = jsonNode.get("value");
                if (value != null) {
                    Iterator<String> iter = value.getFieldNames();
                    while (iter.hasNext()) {
                        jmxDomains.add(iter.next());
                    }
                    LOG.info("Container " + containerName + " has JMX Domains: " + jmxDomains);
                    valid = jmxDomains.size() > 0;
                }
            }
        } catch (IOException e) {
            LOG.warn("Failed to query: " + url + ". " + e, e);
        }

        String provisionResult = container.getProvisionResult();
        LOG.info("Current provision result: " + provisionResult + " valid: " + valid);
        if (valid) {
            if (!Objects.equal(Container.PROVISION_SUCCESS, provisionResult) || !container.isAlive()) {
                container.setProvisionResult(Container.PROVISION_SUCCESS);
                container.setProvisionException(null);
                container.setAlive(true);
                registerJolokiaUrl(metadata, container, jolokiaUrl);
                // TODO update the bundle list....
            }
            if (!Objects.equal(jmxDomains, container.getJmxDomains())) {
                container.setJmxDomains(jmxDomains);
            }
        } else {
            if (container.isAlive()) {
                container.setAlive(true);
            }
            if (!Objects.equal(Container.PROVISION_FAILED, provisionResult)) {
                container.setProvisionResult(Container.PROVISION_FAILED);
            }
        }

    }

    protected void registerJolokiaUrl(CreateDockerContainerMetadata metadata, Container container, String jolokiaUrl) {
        String currentUrl = container.getJolokiaUrl();
        if (Strings.isNullOrBlank(currentUrl)) {
            container.setJolokiaUrl(jolokiaUrl);
            // TODO do we also need to write it into the servlet registry?
        }
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
    public ContainerAutoScaler createAutoScaler() {
        return new DockerAutoScaler(this);
    }


    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }

    public String getDockerAddress() {
        return dockerFactory.getAddress();
    }
}
