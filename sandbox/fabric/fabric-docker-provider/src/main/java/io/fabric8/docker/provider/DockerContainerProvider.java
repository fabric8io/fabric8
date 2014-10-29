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

import io.fabric8.api.Constants;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerAutoScaler;
import io.fabric8.api.ContainerAutoScalerFactory;
import io.fabric8.api.ContainerProvider;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.CreationStateListener;
import io.fabric8.api.FabricRequirements;
import io.fabric8.api.FabricService;
import io.fabric8.api.ProfileRequirements;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.utils.Strings;
import io.fabric8.container.process.JolokiaAgentHelper;
import io.fabric8.docker.api.Docker;
import io.fabric8.docker.api.DockerApiConnectionException;
import io.fabric8.docker.api.Dockers;
import io.fabric8.docker.api.container.ContainerConfig;
import io.fabric8.docker.api.container.ContainerCreateStatus;
import io.fabric8.docker.api.container.HostConfig;
import io.fabric8.internal.Objects;
import io.fabric8.zookeeper.utils.ZooKeeperMasterCache;
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

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
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

@ThreadSafe
@Component(name = "io.fabric8.container.provider.docker", label = "Fabric8 Docker Container Provider", policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = true)
@Service(ContainerProvider.class)
@Properties(
        @Property(name = "fabric.container.protocol", value = DockerConstants.SCHEME)
)
public class DockerContainerProvider extends DockerContainerProviderSupport implements ContainerProvider<CreateDockerContainerOptions, CreateDockerContainerMetadata>, ContainerAutoScalerFactory {

    private static final transient Logger LOG = LoggerFactory.getLogger(DockerContainerProvider.class);

    @Reference(referenceInterface = FabricService.class, bind = "bindFabricService", unbind = "unbindFabricService")
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();
    @Reference(referenceInterface = CuratorFramework.class, bind = "bindCurator", unbind = "unbindCurator")
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();
    @Reference(bind = "bindConfigurer", unbind = "unbindConfigurer")
    private Configurer configurer;

    @Reference(referenceInterface = MBeanServer.class, bind = "bindMBeanServer", unbind = "unbindMBeanServer")
    private MBeanServer mbeanServer;

    @Property(name = "jolokiaKeepAlivePollTime", longValue = 10000,
            label = "The Jolokia Keep Alive Timer Poll Period", description = "The number of milliseconds after which the jolokia agents for any docker containers which expose jolokia will be polled to check for the container status and discover any container resources.")
    private long jolokiaKeepAlivePollTime = 10000;

    private ZooKeeperMasterCache zkMasterCache;

    private ObjectName objectName;
    private DockerFacade mbean;
    private int externalPortCounter;
    private final Object portLock = new Object();

    private Timer keepAliveTimer;
    private Map<String, CreateDockerContainerMetadata> jolokiaKeepAliveContainers = new ConcurrentHashMap<String, CreateDockerContainerMetadata>();

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


    public CreateDockerContainerOptions.Builder newBuilder() {
        return CreateDockerContainerOptions.builder();
    }


    @Override
    public CreateDockerContainerMetadata create(CreateDockerContainerOptions options, CreationStateListener listener) throws Exception {
        DockerCreateContainerParameters parameters = new DockerCreateContainerParameters(options).invoke();
        return doCreateDockerContainer(options, parameters);
    }


    protected CreateDockerContainerMetadata doCreateDockerContainer(CreateDockerContainerOptions options, DockerCreateContainerParameters parameters) {
        ContainerConfig containerConfig = parameters.getContainerConfig();
        Map<String, String> environmentVariables = parameters.getEnvironmentVariables();
        String containerType = parameters.getContainerType();
        String jolokiaUrl = parameters.getJolokiaUrl();
        String name = options.getName();
        String dockerHost = dockerFactory.getDockerHost();
        ContainerCreateStatus status = null;
        CreateDockerContainerMetadata metadata = null;
        try {
            status = docker.containerCreate(containerConfig, name);
            LOG.info("Got status: " + status);
            options = options.updateManualIp(dockerHost);

            metadata = newInstance(containerConfig, options, status, containerType);

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

    @Override
    public void start(Container container) {
        assertValid();
        CreateDockerContainerMetadata containerMetadata = getContainerMetadata(container);
        CreateDockerContainerOptions options = containerMetadata.getCreateOptions();

        String id = getDockerContainerId(container);
        startDockerContainer(id, options);
    }


    @Override
    protected int createExternalPort(String containerId, String portKey, Set<Integer> usedPortByHost, DockerCreateOptions options) {
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
                    String pid = Constants.PORTS_PID;
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

    @Override
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
    public boolean isValidProvider() {
        // docker provider isn't valid in openshift/kubernetes environment
        FabricService service = getFabricService();
        if (service != null) {
            // lets disable child if in docker or openshift environments
            String environment = service.getEnvironment();
            if (Objects.equal(environment, "openshift") || Objects.equal(environment, "kubernetes")) {
                return false;
            }
        }
        return true;
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
    public ContainerAutoScaler createAutoScaler(FabricRequirements requirements, ProfileRequirements profileRequirements) {
        return new DockerAutoScaler(this);
    }

    protected FabricService getFabricService() {
        return fabricService.get();
    }

    protected CuratorFramework getCuratorFramework() {
        return curator.getOptional();
    }

    public Configurer getConfigurer() {
        return configurer;
    }

    public void setConfigurer(Configurer configurer) {
        this.configurer = configurer;
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
        this.setConfigurer(configurer);
    }

    void unbindConfigurer(Configurer configurer) {
        this.setConfigurer(null);
    }

    void bindMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    void unbindMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = null;
    }


    @Override
    public String getDockerAddress() {
        return dockerFactory.getAddress();
    }

}
