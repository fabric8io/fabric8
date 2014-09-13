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
package io.fabric8.kubernetes.provider;

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
import io.fabric8.api.Profiles;
import io.fabric8.api.ZkDefs;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.common.util.Objects;
import io.fabric8.common.util.Strings;
import io.fabric8.container.process.JolokiaAgentHelper;
import io.fabric8.docker.api.Docker;
import io.fabric8.docker.api.DockerApiConnectionException;
import io.fabric8.docker.api.Dockers;
import io.fabric8.docker.api.container.ContainerConfig;
import io.fabric8.docker.api.container.ContainerCreateStatus;
import io.fabric8.docker.provider.DockerConstants;
import io.fabric8.docker.provider.DockerContainerProviderSupport;
import io.fabric8.docker.provider.DockerCreateOptions;
import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.api.KubernetesFactory;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.DesiredState;
import io.fabric8.kubernetes.api.model.ManifestContainer;
import io.fabric8.kubernetes.api.model.ManifestSchema;
import io.fabric8.kubernetes.api.model.PodSchema;
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

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import static io.fabric8.kubernetes.provider.KubernetesConstants.LABELS;

/**
 * A container provider using <a href="http://kubernetes.io/">Kubernetes</a> to create containers
 */
@ThreadSafe
@Component(name = "io.fabric8.kubernetes.provider",
        label = "Fabric8 Kubernetes Container Provider",
        description = "Supports managing containers on Kubernetes and OpenShift 3.x or later",
        policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = true)
@Service(ContainerProvider.class)
@Properties(
        @Property(name = "fabric.container.protocol", value = KubernetesConstants.SCHEME)
)
public class KubernetesContainerProvider extends DockerContainerProviderSupport implements ContainerProvider<CreateKubernetesContainerOptions, CreateKubernetesContainerMetadata>, ContainerAutoScalerFactory {

    private static final transient Logger LOG = LoggerFactory.getLogger(KubernetesContainerProvider.class);

    @Reference(referenceInterface = FabricService.class, bind = "bindFabricService", unbind = "unbindFabricService")
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();
    @Reference(referenceInterface = CuratorFramework.class, bind = "bindCurator", unbind = "unbindCurator")
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();
    @Reference(bind = "bindConfigurer", unbind = "unbindConfigurer")
    private Configurer configurer;

    @Property(name = "jolokiaKeepAlivePollTime", longValue = 10000,
            label = "The Jolokia Keep Alive Timer Poll Period", description = "The number of milliseconds after which the jolokia agents for any docker containers which expose jolokia will be polled to check for the container status and discover any container resources.")
    private long jolokiaKeepAlivePollTime = 10000;

    @Property(name = "kubernetesMaster",
            label = "Kubernetes Master",
            description = "The URL to connect to the Kubernetes Master.")
    private String kubernetesMaster;

    @Property(name = "dockerHost",
            label = "Docker Host",
            description = "The URL to connect to Docker.")
    private String dockerHost;

    private KubernetesFactory kubernetesFactory;
    private Kubernetes kubernetes;

    private ZooKeeperMasterCache zkMasterCache;

    private int externalPortCounter;
    private final Object portLock = new Object();

    private Timer keepAliveTimer;
    private Map<String, CreateKubernetesContainerMetadata> jolokiaKeepAliveContainers = new ConcurrentHashMap<String, CreateKubernetesContainerMetadata>();


    @Activate
    public void activate(Map<String, ?> configuration) throws Exception {
        updateConfiguration(configuration);
        activateComponent();
    }

    @Modified
    public void modified(Map<String, ?> configuration) throws Exception {
        updateConfiguration(configuration);
    }

    @Deactivate
    public void deactivate() throws MBeanRegistrationException, InstanceNotFoundException {
        if (zkMasterCache != null) {
            zkMasterCache = null;
        }
        deactivateComponent();
    }

    private void updateConfiguration(Map<String, ?> configuration) throws Exception {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            getConfigurer().configure(configuration, this);
            if (Strings.isNotBlank(dockerHost)) {
                dockerFactory.setAddress(dockerHost);
            }
            kubernetesFactory = new KubernetesFactory();
            if (Strings.isNotBlank(kubernetesMaster)) {
                kubernetesFactory.setAddress(kubernetesMaster);
            }
            // Resteasy uses the TCCL to load the API
            Thread.currentThread().setContextClassLoader(Docker.class.getClassLoader());
            this.docker = dockerFactory.createDocker();

            // Resteasy uses the TCCL to load the API
            Thread.currentThread().setContextClassLoader(Kubernetes.class.getClassLoader());
            this.kubernetes = kubernetesFactory.createKubernetes();
        } catch (Throwable e) {
            LOG.error("Failed to update configuration " + configuration + ". " + e, e);
            throw e;
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    public CreateKubernetesContainerOptions.Builder newBuilder() {
        return CreateKubernetesContainerOptions.builder();
    }


    @Override
    public CreateKubernetesContainerMetadata create(CreateKubernetesContainerOptions options, CreationStateListener listener) throws Exception {
        DockerCreateContainerParameters parameters = new DockerCreateContainerParameters(options).invoke();
        return doCreateDockerContainer(options, parameters);
    }


    protected CreateKubernetesContainerMetadata doCreateDockerContainer(CreateKubernetesContainerOptions options, DockerCreateContainerParameters parameters) throws Exception {
        Objects.notNull(kubernetes, "kubernetes");
        ContainerConfig containerConfig = parameters.getContainerConfig();
        Map<String, String> environmentVariables = parameters.getEnvironmentVariables();
        String containerType = parameters.getContainerType();
        String jolokiaUrl = parameters.getJolokiaUrl();
        String name = options.getName();
        String image = parameters.getContainerConfig().getImage();
        Set<String> profileIds = options.getProfiles();
        String versionId = options.getVersion();
        FabricService service = getFabricService();

        PodSchema pod = new PodSchema();
        pod.setId(name);

        Map<String, String> labels = new HashMap<>();
        labels.put(LABELS.FABRIC8, "true");
        labels.put(LABELS.CONTAINER, name);
        String profileString = Strings.join(profileIds, ",");
        labels.put(LABELS.PROFILE, profileString);
        labels.put(LABELS.VERSION, versionId);
        Map<String, String> labelConfiguration = Profiles.getOverlayConfiguration(service, profileIds, versionId, KubernetesConstants.LABELS_PID, "kubernetes");
        if (labelConfiguration != null) {
            labels.putAll(labelConfiguration);
        }

        pod.setLabels(labels);
        DesiredState desiredState = new DesiredState();
        pod.setDesiredState(desiredState);
        ManifestSchema manifest = new ManifestSchema();
        manifest.setVersion(ManifestSchema.Version.V_1_BETA_1);
        desiredState.setManifest(manifest);

        ManifestContainer manifestContainer = new ManifestContainer();
        manifestContainer.setName(name);
        manifestContainer.setImage(image);
        List<String> cmd = options.getCmd();
        if (cmd != null && !cmd.isEmpty()) {
            manifestContainer.setCommand(cmd);
        }
        manifestContainer.setWorkingDir(options.getWorkingDir());
        manifestContainer.setEnv(KubernetesHelper.createEnv(environmentVariables));

        // TODO
        //manifestContainer.setVolumeMounts();
        //manifestContainer.setPorts();

        List<ManifestContainer> containers = new ArrayList<>();
        containers.add(manifestContainer);
        manifest.setContainers(containers);

        try {
            LOG.info("About to create pod with image " + image + " on " + kubernetesFactory.getAddress() + " with " + pod);
            kubernetes.createPod(pod);

        } catch (Exception e) {
            LOG.info("Failed to create pod " + name + " from config " + pod
                    + ": " + e + Dockers.dockerErrorMessage(e), e);
            throw e;
        }
        ContainerCreateStatus status = null;
        CreateKubernetesContainerMetadata metadata = null;

        // TODO how to set the manual IP???
        // options = options.updateManualIp(dockerHost);

        metadata = createKubernetesContainerMetadata(containerConfig, options, status, containerType);
        publishZooKeeperValues(options, environmentVariables);

        if (jolokiaUrl != null) {
            metadata.setJolokiaUrl(jolokiaUrl);
            startJolokiaKeepAlive(metadata);
        }
        return metadata;
    }


    public static CreateKubernetesContainerMetadata createKubernetesContainerMetadata(ContainerConfig containerConfig, DockerCreateOptions options, ContainerCreateStatus status, String containerType) {
        List<String> warnings = new ArrayList<String>();
        String statusId = "unknown";
        if (status != null) {
            statusId = status.getId();
            String[] warningArray = status.getWarnings();
            if (warningArray != null) {
                Collections.addAll(warnings, warningArray);
            }
        }
        CreateKubernetesContainerMetadata metadata = new CreateKubernetesContainerMetadata(statusId, warnings);
        String containerId = options.getName();
        metadata.setContainerName(containerId);
        metadata.setContainerType(containerType);
        metadata.setOverridenResolver(ZkDefs.MANUAL_IP);
        metadata.setCreateOptions(options);
        return metadata;
    }
    
    @Override
    public void start(Container container) {
        assertValid();
        CreateKubernetesContainerMetadata containerMetadata = getContainerMetadata(container);
        CreateKubernetesContainerOptions options = containerMetadata.getCreateOptions();

        String id = getPodId(container);
        startPod(id, options);
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

    protected void startPod(String id, CreateKubernetesContainerOptions options) {
        if (!Strings.isNullOrBlank(id)) {
            // TODO
            todo();
        }
    }

    @Override
    protected Set<Integer> findUsedPortByHostAndDocker() {
        try {
            // TODO
            return new HashSet<>();
        } catch (DockerApiConnectionException e) {
            String suggestion = String.format("Can't connect to the Docker server. Are you sure a Docker server is running at %s?", dockerFactory.getAddress());
            throw new DockerApiConnectionException(suggestion, e.getCause());
        }
    }

    @Override
    public void stop(Container container) {
        assertValid();
        String id = getPodId(container);
        if (!Strings.isNullOrBlank(id)) {
            LOG.info("stopping pod " + id);
            CreateKubernetesContainerMetadata metadata = getContainerMetadata(container);
            if (metadata != null) {
                stopJolokiaKeepAlive(metadata);
            }

            todo();
            container.setProvisionResult(Container.PROVISION_STOPPED);
        }
    }

    @Override
    public void destroy(Container container) {
        assertValid();
        String id = getPodId(container);
        if (!Strings.isNullOrBlank(id)) {
            LOG.info("destroying pod " + id);
            try {
                todo();
            } catch (Exception e) {
                LOG.info("Could not remove pod: it probably no longer exists " + e + Dockers.dockerErrorMessage(e), e);
            }
        }
    }

    protected void todo() {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet!");
    }


    protected synchronized void startJolokiaKeepAlive(CreateKubernetesContainerMetadata metadata) {
        LOG.info("Starting Jolokia Keep Alive for " + metadata.getId());
        jolokiaKeepAliveContainers.put(metadata.getId(), metadata);
        if (keepAliveTimer == null) {
            keepAliveTimer = new Timer("fabric8-docker-container-keepalive");

            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    List<CreateKubernetesContainerMetadata> list = new ArrayList<>(jolokiaKeepAliveContainers.values());
                    for (CreateKubernetesContainerMetadata containerMetadata : list) {
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

    protected void stopJolokiaKeepAlive(CreateKubernetesContainerMetadata metadata) {
        LOG.info("Stopping Jolokia Keep Alive for " + metadata.getId());
        jolokiaKeepAliveContainers.remove(metadata.getId());
    }

    @Override
    public String getScheme() {
        assertValid();
        return KubernetesConstants.SCHEME;
    }

    @Override
    public boolean isValidProvider() {
        return true;
    }

    @Override
    public Class<CreateKubernetesContainerOptions> getOptionsType() {
        assertValid();
        return CreateKubernetesContainerOptions.class;
    }

    @Override
    public Class<CreateKubernetesContainerMetadata> getMetadataType() {
        assertValid();
        return CreateKubernetesContainerMetadata.class;
    }

    public Docker getDocker() {
        return docker;
    }

    protected String getPodId(Container container) {
        CreateKubernetesContainerMetadata containerMetadata = getContainerMetadata(container);
        if (containerMetadata != null) {
            return containerMetadata.getId();
        }
        return container.getId();
    }

    protected static CreateKubernetesContainerMetadata getContainerMetadata(Container container) {
        CreateContainerMetadata<?> value = container.getMetadata();
        if (value instanceof CreateKubernetesContainerMetadata) {
            return (CreateKubernetesContainerMetadata) value;
        } else {
            return null;
        }
    }

    @Override
    public ContainerAutoScaler createAutoScaler(FabricRequirements requirements, ProfileRequirements profileRequirements) {
        return new KubernetesAutoScaler(this);
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
        // TODO
        // zkMasterCache = new ZooKeeperMasterCache(curator);
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

    @Override
    public String getDockerAddress() {
        return dockerFactory.getAddress();
    }

}
