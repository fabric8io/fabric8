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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.fabric8.api.Constants;
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
import io.fabric8.api.Profiles;
import io.fabric8.api.ZkDefs;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.common.util.Files;
import io.fabric8.common.util.Objects;
import io.fabric8.common.util.Strings;
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
import io.fabric8.kubernetes.api.model.Port;
import io.fabric8.kubernetes.api.model.ReplicationControllerSchema;
import io.fabric8.kubernetes.api.model.ServiceSchema;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.fabric8.kubernetes.api.KubernetesHelper.getPodMap;
import static io.fabric8.kubernetes.api.KubernetesHelper.getReplicationControllerMap;
import static io.fabric8.kubernetes.api.KubernetesHelper.getServiceMap;
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
public class KubernetesContainerProvider extends DockerContainerProviderSupport implements ContainerProvider<CreateKubernetesContainerOptions, CreateKubernetesContainerMetadata> {

    private static final transient Logger LOG = LoggerFactory.getLogger(KubernetesContainerProvider.class);

    @Reference(referenceInterface = FabricService.class, bind = "bindFabricService", unbind = "unbindFabricService")
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();
    @Reference(referenceInterface = CuratorFramework.class, bind = "bindCurator", unbind = "unbindCurator")
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();
    @Reference(bind = "bindConfigurer", unbind = "unbindConfigurer")
    private Configurer configurer;
    @Reference(referenceInterface = KubernetesService.class, bind = "bindKubernetesService", unbind = "unbindKubernetesService")
    private final ValidatingReference<KubernetesService> kubernetesService = new ValidatingReference<KubernetesService>();

    @Property(name = "dockerHost",
            label = "Docker Host",
            description = "The URL to connect to Docker.")
    private String dockerHost;

    private int externalPortCounter;
    private final Object portLock = new Object();

    private ObjectMapper objectMapper = KubernetesFactory.createObjectMapper();

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
        deactivateComponent();
    }

    private void updateConfiguration(Map<String, ?> configuration) throws Exception {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            getConfigurer().configure(configuration, this);
            if (Strings.isNotBlank(dockerHost)) {
                dockerFactory.setAddress(dockerHost);
            }
            // Resteasy uses the TCCL to load the API
            Thread.currentThread().setContextClassLoader(Docker.class.getClassLoader());
            this.docker = dockerFactory.createDocker();
        } catch (Throwable e) {
            LOG.error("Failed to update configuration " + configuration + ". " + e, e);
            throw e;
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    public Kubernetes getKubernetes() {
        return KubernetesService.getKubernetes(kubernetesService.getOptional());
    }

    private String getKubernetesAddress() {
        return KubernetesService.getKubernetesAddress(kubernetesService.getOptional());
    }

    public CreateKubernetesContainerOptions.Builder newBuilder() {
        return CreateKubernetesContainerOptions.builder();
    }

    @Override
    public CreateKubernetesContainerMetadata create(CreateKubernetesContainerOptions options, CreationStateListener listener) throws Exception {
        FabricService service = getFabricService();
        List<String> kubelets = getKubeletFileNames(service, options);
        if (kubelets.size() > 0) {
            return doCreateKubernetesPodsControllersServices(service, options, kubelets);
        }

        DockerCreateContainerParameters parameters = new DockerCreateContainerParameters(options).invoke();
        doCreateDockerContainer(options, parameters);

        ContainerConfig containerConfig = parameters.getContainerConfig();
        Map<String, String> environmentVariables = parameters.getEnvironmentVariables();
        String containerType = parameters.getContainerType();

        ContainerCreateStatus status = null;
        CreateKubernetesContainerMetadata metadata = null;

        metadata = createKubernetesContainerMetadata(options, status, containerType);
        publishZooKeeperValues(options, environmentVariables);
        return metadata;

    }

    protected KubernetesConfig getKubernetesConfig(FabricService service, CreateKubernetesContainerOptions options) throws Exception {
        KubernetesConfig config = null;
        Set<String> profileIds = options.getProfiles();
        String versionId = options.getVersion();
        Map<String, String> configuration = null;
        if (service != null) {
            configuration = Profiles.getOverlayConfiguration(service, profileIds, versionId,
                    KubernetesConstants.KUBERNETES_PID, "kubernetes");
        }
        if (configuration != null) {
            config = new KubernetesConfig();
            configurer.configure(configuration, config);
        }
        return config;
    }

    /**
     * Returns the file names of the kubelets in the profiles
     */
    protected List<String> getKubeletFileNames(FabricService service, CreateKubernetesContainerOptions options) throws Exception {
        Collection<Profile> profiles = Profiles.getProfiles(service, options.getProfiles(), options.getVersion());
        Set<String> allConfigFiles = Profiles.getConfigurationFileNames(profiles);
        List<String> answer = new ArrayList<>();
        for (String fileName : allConfigFiles) {
            if (fileName.startsWith("kubelet/") && fileName.endsWith(".json")) {
                answer.add(fileName);
            }
        }
        return answer;
    }

    /**
     * Loads all the kubelet objects from the profiles and returns then in a map indexed by the file name
     */
    protected Map<String, Object> loadKubelets(FabricService service, CreateKubernetesContainerOptions options, List<String> definitions, CreateKubernetesContainerMetadata metadata) {
        Map<String, Object> answer = new HashMap<>();
        String containerId = options.getName();
        byte[] json = null;
        Kubernetes kubernetes = getKubernetes();
        Objects.notNull(kubernetes, "kubernetes");

        Set<String> profileIds = options.getProfiles();
        String versionId = options.getVersion();
        List<Profile> profiles = Profiles.getProfiles(service, profileIds, versionId);

        for (String definition : definitions) {
            definition = definition.trim();
            if (!definition.contains(":")) {
                // lets assume its a file in the profile
                json = Profiles.getFileConfiguration(profiles, definition);
            }
            if (json == null) {
                URL url = null;
                try {
                    url = new URL(definition);
                } catch (MalformedURLException e) {
                    LOG.warn("Could not parse kube definition URL " + definition + ". " + e, e);
                }
                if (url != null) {
                    try {
                        InputStream in = url.openStream();
                        if (in != null) {
                            json = Files.readBytes(in);
                        }
                    } catch (IOException e) {
                        LOG.warn("Failed to load URL " + url + ". " + e, e);
                    }
                }
            }
            if (json != null) {
                Object dto = null;
                try {
                    ObjectReader reader = objectMapper.reader();
                    JsonNode tree = null;
                    if (json != null) {
                        tree = reader.readTree(new ByteArrayInputStream(json));
                        if (tree != null) {
                            JsonNode kindNode = tree.get("kind");
                            if (kindNode != null) {
                                String kind = kindNode.asText();
                                if (Objects.equal("Pod", kind)) {
                                    PodSchema podSchema = objectMapper.reader(PodSchema.class).readValue(json);
                                    configurePod(podSchema, service, options, metadata);
                                    answer.put(definition, podSchema);
                                } else if (Objects.equal("ReplicationController", kind)) {
                                    ReplicationControllerSchema replicationControllerSchema = objectMapper.reader(ReplicationControllerSchema.class).readValue(json);
                                    configureReplicationController(replicationControllerSchema, service, options, metadata);
                                    answer.put(definition, replicationControllerSchema);
                                } else if (Objects.equal("Service", kind)) {
                                    ServiceSchema serviceSchema = objectMapper.reader(ServiceSchema.class).readValue(json);
                                    configureService(serviceSchema, service, options, metadata);
                                    answer.put(definition, serviceSchema);
                                } else {
                                    LOG.warn("Unknown JSON from " + definition + ". JSON: " + tree);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    LOG.warn("Failed to parse JSON definition: " + definition + ". " + e, e);
                }
            }
        }
        return answer;
    }

    /**
     * Creates all the controllers, pods and services from the profile metadata
     */
    protected CreateKubernetesContainerMetadata doCreateKubernetesPodsControllersServices(FabricService service, CreateKubernetesContainerOptions options, List<String> kubeletFileNames) {
        String containerId = options.getName();
        String status = "TODO";
        List<String> warnings = new ArrayList<>();
        CreateKubernetesContainerMetadata metadata = createKubernetesContainerMetadata(options, containerId, "kubelet", status, warnings);

        startKubletPodsReplicationControllersServices(service, options, kubeletFileNames, metadata);

        // TODO
        // publishZooKeeperValues(options, environmentVariables);
        return metadata;
    }

    /**
     * Starts any kublets from the profiles which are not already running
     */
    protected void startKubletPodsReplicationControllersServices(FabricService service, CreateKubernetesContainerOptions options, List<String> kubeletFileNames, CreateKubernetesContainerMetadata metadata) {
        Kubernetes kubernetes = getKubernetes();
        Objects.notNull(kubernetes, "kubernetes");

        Map<String, PodSchema> podMap = null;
        Map<String, ReplicationControllerSchema> replicationControllerMap = null;
        Map<String, ServiceSchema> serviceMap = null;

        Map<String, Object> kubelets = loadKubelets(service, options, kubeletFileNames, metadata);
        Set<Map.Entry<String, Object>> entries = kubelets.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            String definition = entry.getKey();
            Object kubelet = entry.getValue();
            if (kubelet instanceof PodSchema) {
                PodSchema podSchema = (PodSchema) kubelet;
                if (podMap == null) {
                    podMap = getPodMap(kubernetes);
                }
                String id = podSchema.getId();
                PodSchema old = podMap.get(id);
                if (isRunning(old)) {
                    LOG.info("Not creating pod for " + id + " from definition " + definition + " as its already running");
                } else {
                    LOG.info("Creating a pod from " + definition);
                    try {
                        Object answer = kubernetes.createPod(podSchema);
                        LOG.info("Created pod result: " + answer);
                    } catch (Exception e) {
                        LOG.error("Failed to create pod from " + definition + ". " + e + ". " + podSchema, e);
                    }
                }
            } else if (kubelet instanceof ReplicationControllerSchema) {
                ReplicationControllerSchema replicationControllerSchema = (ReplicationControllerSchema) kubelet;
                if (replicationControllerMap == null) {
                    replicationControllerMap = getReplicationControllerMap(kubernetes);
                }
                String id = replicationControllerSchema.getId();
                ReplicationControllerSchema old = replicationControllerMap.get(id);
                if (isRunning(old)) {
                    LOG.info("Not creating replicationController for " + id + " from definition " + definition + " as its already running");
                } else {
                    LOG.info("Creating a replicationController from " + definition);
                    try {
                        Object answer = kubernetes.createReplicationController(replicationControllerSchema);
                        LOG.info("Created replicationController: " + answer);
                    } catch (Exception e) {
                        LOG.error("Failed to create controller from " + definition + ". " + e + ". " + replicationControllerSchema, e);
                    }
                }
            } else if (kubelet instanceof ServiceSchema) {
                ServiceSchema serviceSchema = (ServiceSchema) kubelet;
                if (serviceMap == null) {
                    serviceMap = getServiceMap(kubernetes);
                }
                String id = serviceSchema.getId();
                ServiceSchema old = serviceMap.get(id);
                if (isRunning(old)) {
                    LOG.info("Not creating pod for " + id + " from defintion " + definition + " as its already running");
                } else {
                    LOG.info("Creating a service from " + definition);
                    try {
                        Object answer = kubernetes.createService(serviceSchema);
                        LOG.info("Created service: " + answer);
                    } catch (Exception e) {
                        LOG.error("Failed to create controller from " + definition + ". " + e + ". " + serviceSchema, e);
                    }
                }
            } else {
                LOG.warn("Unknown Kublelet from " + definition + ". Object: " + kubelet);
            }
        }
    }

    /**
     * Stops any kublets from the profiles
     */
    protected void stopKubletPodsReplicationControllersServices(FabricService service, CreateKubernetesContainerOptions options, List<String> kubeletFileNames, CreateKubernetesContainerMetadata metadata) {
        Kubernetes kubernetes = getKubernetes();
        Objects.notNull(kubernetes, "kubernetes");

        Map<String, PodSchema> podMap = null;
        Map<String, ReplicationControllerSchema> replicationControllerMap = null;
        Map<String, ServiceSchema> serviceMap = null;

        Map<String, Object> kubelets = loadKubelets(service, options, kubeletFileNames, metadata);
        Set<Map.Entry<String, Object>> entries = kubelets.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            String definition = entry.getKey();
            Object kubelet = entry.getValue();
            if (kubelet instanceof PodSchema) {
                PodSchema podSchema = (PodSchema) kubelet;
                if (podMap == null) {
                    podMap = getPodMap(kubernetes);
                }
                String id = podSchema.getId();
                PodSchema old = podMap.get(id);
                if (isRunning(old)) {
                    try {
                        deletePod(id);
                    } catch (Exception e) {
                        LOG.error("Failed to delete pod " + id + " from " + definition + ": " + e + ". ", e);
                    }
                }
            } else if (kubelet instanceof ReplicationControllerSchema) {
                ReplicationControllerSchema replicationControllerSchema = (ReplicationControllerSchema) kubelet;
                if (replicationControllerMap == null) {
                    replicationControllerMap = getReplicationControllerMap(kubernetes);
                }
                String id = replicationControllerSchema.getId();
                ReplicationControllerSchema old = replicationControllerMap.get(id);
                if (isRunning(old)) {
                    try {
                        deleteReplicationController(id);
                    } catch (Exception e) {
                        LOG.error("Failed to delete replicationController " + id + " from " + definition + ": " + e + ". ", e);
                    }
                }
            } else if (kubelet instanceof ServiceSchema) {
                ServiceSchema serviceSchema = (ServiceSchema) kubelet;
                if (serviceMap == null) {
                    serviceMap = getServiceMap(kubernetes);
                }
                String id = serviceSchema.getId();
                ServiceSchema old = serviceMap.get(id);
                if (isRunning(old)) {
                    try {
                        deleteService(id);
                    } catch (Exception e) {
                        LOG.error("Failed to delete service " + id + " from " + definition + ": " + e + ". ", e);
                    }
                }
            } else {
                LOG.warn("Unknown Kublelet from " + definition + ". Object: " + kubelet);
            }
        }
    }

    protected boolean isRunning(PodSchema entity) {
        // TODO we could maybe ignore failed services?
        return entity != null;
    }

    protected boolean isRunning(ReplicationControllerSchema entity) {
        // TODO we could maybe ignore failed services?
        return entity != null;
    }

    protected boolean isRunning(ServiceSchema entity) {
        // TODO we could maybe ignore failed services?
        return entity != null;
    }

    protected void configurePod(PodSchema podSchema, FabricService service, CreateKubernetesContainerOptions options, CreateKubernetesContainerMetadata metadata) {
        podSchema.setLabels(configureLabels(podSchema.getLabels(), service, options));
        String id = podSchema.getId();
        if (Strings.isNotBlank(id)) {
            metadata.addPodId(id);
        }
    }

    protected void configureReplicationController(ReplicationControllerSchema replicationControllerSchema, FabricService service, CreateKubernetesContainerOptions options, CreateKubernetesContainerMetadata metadata) {
        replicationControllerSchema.setLabels(configureLabels(replicationControllerSchema.getLabels(), service, options));
        String id = replicationControllerSchema.getId();
        if (Strings.isNotBlank(id)) {
            metadata.addReplicationControllerId(id);
        }
    }

    protected void configureService(ServiceSchema serviceSchema, FabricService service, CreateKubernetesContainerOptions options, CreateKubernetesContainerMetadata metadata) {
        serviceSchema.setLabels(configureLabels(serviceSchema.getLabels(), service, options));
        String id = serviceSchema.getId();
        if (Strings.isNotBlank(id)) {
            metadata.addServiceId(id);
        }
    }

    protected Map<String, String> configureLabels(Map<String, String> labels, FabricService service, CreateKubernetesContainerOptions options) {
        String name = options.getName();
        Set<String> profileIds = options.getProfiles();
        String versionId = options.getVersion();
        return updateLabels(labels, service, name, profileIds, versionId);
    }

    @Override
    public void start(Container container) {
        assertValid();
        CreateKubernetesContainerMetadata containerMetadata = getContainerMetadata(container);
        CreateKubernetesContainerOptions options = containerMetadata.getCreateOptions();
        if (containerMetadata != null && containerMetadata.isKubelet()) {
            try {
                FabricService service = getFabricService();
                List<String> kubelets = getKubeletFileNames(service, options);
                if (kubelets.size() > 0) {
                    startKubletPodsReplicationControllersServices(service, options, kubelets, containerMetadata);
                }
            } catch (Exception e) {
                String message = "Could not start kubelet for container: " + container.getId() + ": " + e + Dockers.dockerErrorMessage(e);
                LOG.warn(message, e);
                throw new RuntimeException(message, e);
            }
        } else {
            try {
                DockerCreateContainerParameters parameters = new DockerCreateContainerParameters(options).invoke();
                doCreateDockerContainer(options, parameters);
            } catch (Exception e) {
                String message = "Could not start pod for container: " + container.getId() + ": " + e + Dockers.dockerErrorMessage(e);
                LOG.warn(message, e);
                throw new RuntimeException(message, e);
            }
        }
    }

    @Override
    public void stop(Container container) {
        assertValid();
        CreateKubernetesContainerMetadata containerMetadata = getContainerMetadata(container);
        CreateKubernetesContainerOptions options = containerMetadata.getCreateOptions();
        if (containerMetadata != null && containerMetadata.isKubelet()) {
            try {
                FabricService service = getFabricService();
                List<String> kubelets = getKubeletFileNames(service, options);
                if (kubelets.size() > 0) {
                    stopKubletPodsReplicationControllersServices(service, options, kubelets, containerMetadata);
                }
            } catch (Exception e) {
                String message = "Could not start kubelet for container: " + container.getId() + ": " + e + Dockers.dockerErrorMessage(e);
                LOG.warn(message, e);
                throw new RuntimeException(message, e);
            }
        } else {
            String id = getPodId(container);
            if (!Strings.isNullOrBlank(id)) {
                try {
                    deletePod(id);
                } catch (Exception e) {
                    String message = "Could not remove pod: it probably no longer exists " + e + Dockers.dockerErrorMessage(e);
                    LOG.warn(message, e);
                    throw new RuntimeException(message, e);
                }
                container.setProvisionResult(Container.PROVISION_STOPPED);
            }
        }
    }


    protected void doCreateDockerContainer(CreateKubernetesContainerOptions options, DockerCreateContainerParameters parameters) throws Exception {
        Kubernetes kubernetes = getKubernetes();
        Objects.notNull(kubernetes, "kubernetes");
        ContainerConfig containerConfig = parameters.getContainerConfig();
        Map<String, String> environmentVariables = parameters.getEnvironmentVariables();
        environmentVariables.put(EnvironmentVariables.FABRIC8_LISTEN_ADDRESS, "0.0.0.0");
        //environmentVariables.put(EnvironmentVariables.FABRIC8_FABRIC_ENVIRONMENT, "kubernetes");
        environmentVariables.remove(EnvironmentVariables.FABRIC8_GLOBAL_RESOLVER);
        environmentVariables.remove(EnvironmentVariables.FABRIC8_MANUALIP);
        String containerType = parameters.getContainerType();
        String jolokiaUrl = parameters.getJolokiaUrl();
        String name = options.getName();
        String image = containerConfig.getImage();
        Set<String> profileIds = options.getProfiles();
        String versionId = options.getVersion();
        FabricService service = getFabricService();

        PodSchema pod = new PodSchema();
        pod.setId(KubernetesHelpers.containerNameToPodId(name));

        Map<String, String> labels = updateLabels(null, service, name, profileIds, versionId);

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

        Map<String, String> portConfigurations = Profiles.getOverlayConfiguration(service, profileIds, versionId, Constants.PORTS_PID, "kubernetes");
        if (portConfigurations != null && portConfigurations.size() > 0) {
            List<Port> ports = new ArrayList<>();
            Set<Map.Entry<String, String>> entries = portConfigurations.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                String portName = entry.getKey();
                String defaultPortValue = entry.getValue();
                if (Strings.isNotBlank(defaultPortValue)) {
                    Integer portNumber = null;
                    try {
                        portNumber = Integer.parseInt(defaultPortValue);
                    } catch (NumberFormatException e) {
                        LOG.warn("Could not parse '" + defaultPortValue
                                + "' as integer for port " + portName
                                + " for profiles " + profileIds + " " + versionId + ". " + e, e);
                    }
                    String hostPortText = environmentVariables.get("FABRIC8_" + portName + "_PROXY_PORT");
                    Integer hostPort = null;
                    if (hostPortText != null) {
                        try {
                            hostPort = Integer.parseInt(hostPortText);
                        } catch (NumberFormatException e) {
                            LOG.warn("Could not parse '" + hostPortText
                                    + "' as integer for port " + portName
                                    + " for profiles " + profileIds + " " + versionId + ". " + e, e);
                        }
                    }
                    if (portNumber != null && hostPort != null) {
                        Port port = new Port();
                        //port.setName(portName);
                        //port.setProtocol(portName.toLowerCase());
                        //port.setProtocol("tcp");
                        port.setContainerPort(portNumber);
                        port.setHostPort(hostPort);
                        ports.add(port);
                    }
                }
            }
            manifestContainer.setPorts(ports);
        }

        List<ManifestContainer> containers = new ArrayList<>();
        containers.add(manifestContainer);
        manifest.setContainers(containers);

        try {
            LOG.info("About to create pod with image " + image + " on " + getKubernetesAddress() + " with " + pod);
            Object answer = kubernetes.createPod(pod);
            LOG.info("Go answer: " + answer);
        } catch (Exception e) {
            LOG.info("Failed to create pod " + name + " from config " + pod
                    + ": " + e + Dockers.dockerErrorMessage(e), e);
            throw e;
        }
    }

    protected Map<String, String> updateLabels(Map<String, String> initialLabels, FabricService service, String containerName, Set<String> profileIds, String versionId) {
        Map<String, String> labels = new HashMap<>();
        if (initialLabels != null) {
            labels.putAll(initialLabels);
        }
        if (!labels.containsKey(LABELS.FABRIC8)) {
            labels.put(LABELS.FABRIC8, "true");
        }
        if (!labels.containsKey(LABELS.NAME)) {
            labels.put(LABELS.NAME, containerName);
        }
        if (!labels.containsKey(LABELS.PROFILE)) {
            String profileString = Strings.join(profileIds, ",");
            labels.put(LABELS.PROFILE, profileString);
        }
        if (!labels.containsKey(LABELS.VERSION)) {
            labels.put(LABELS.VERSION, versionId);
        }
        Map<String, String> labelConfiguration = Profiles.getOverlayConfiguration(service, profileIds, versionId, KubernetesConstants.LABELS_PID, "kubernetes");
        if (labelConfiguration != null) {
            labels.putAll(labelConfiguration);
        }
        return labels;
    }


    public static CreateKubernetesContainerMetadata createKubernetesContainerMetadata(CreateKubernetesContainerOptions options, ContainerCreateStatus status, String containerType) {
        String containerId = options.getName();
        List<String> warnings = new ArrayList<String>();
        String statusId = "unknown";
        if (status != null) {
            statusId = status.getId();
            String[] warningArray = status.getWarnings();
            if (warningArray != null) {
                Collections.addAll(warnings, warningArray);
            }
        }
        return createKubernetesContainerMetadata(options, containerId, containerType, statusId, warnings);
    }

    public static CreateKubernetesContainerMetadata createKubernetesContainerMetadata(CreateKubernetesContainerOptions options, String containerId, String containerType, String statusId, List<String> warnings) {
        CreateKubernetesContainerMetadata metadata = new CreateKubernetesContainerMetadata(statusId, warnings);
        metadata.setFailure(null);
        metadata.setCreateOptions(options);
        metadata.setContainerName(containerId);
        metadata.setContainerType(containerType);
        metadata.setOverridenResolver(ZkDefs.MANUAL_IP);
        return metadata;
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

    protected void deletePod(String id) throws Exception {
        LOG.info("stopping pod " + id);
        Kubernetes kubernetes = getKubernetes();
        Objects.notNull(kubernetes, "kubernetes");
        kubernetes.deletePod(id);
    }

    protected void deleteReplicationController(String id) throws Exception {
        LOG.info("stopping replicationController " + id);
        Kubernetes kubernetes = getKubernetes();
        Objects.notNull(kubernetes, "kubernetes");
        kubernetes.deleteReplicationController(id);
    }

    protected void deleteService(String id) throws Exception {
        LOG.info("stopping service " + id);
        Kubernetes kubernetes = getKubernetes();
        Objects.notNull(kubernetes, "kubernetes");
        kubernetes.deleteService(id);
    }

    @Override
    public void destroy(Container container) {
        assertValid();
        String id = getPodId(container);
        if (!Strings.isNullOrBlank(id)) {
            try {
                deletePod(id);
            } catch (Exception e) {
                LOG.info("Could not remove pod: it probably no longer exists " + e + Dockers.dockerErrorMessage(e), e);
            }
        }
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
        return KubernetesHelpers.containerNameToPodId(container.getId());
    }

    protected static CreateKubernetesContainerMetadata getContainerMetadata(Container container) {
        CreateContainerMetadata<?> value = container.getMetadata();
        if (value instanceof CreateKubernetesContainerMetadata) {
            return (CreateKubernetesContainerMetadata) value;
        } else {
            return null;
        }
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

    void bindKubernetesService(KubernetesService kubernetesService) {
        this.kubernetesService.bind(kubernetesService);
    }

    void unbindKubernetesService(KubernetesService kubernetesService) {
        this.kubernetesService.unbind(kubernetesService);
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
