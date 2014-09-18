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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        Set<String> profileIds = options.getProfiles();
        String versionId = options.getVersion();

        Map<String, String> configuration = null;
        FabricService service = getFabricService();
        if (service != null) {
            configuration = Profiles.getOverlayConfiguration(service, profileIds, versionId,
                    KubernetesConstants.KUBERNETES_PID, "kubernetes");
        }
        if (configuration != null) {
            KubernetesConfig config = new KubernetesConfig();
            configurer.configure(configuration, config);
            List<String> definitions = config.getDefinitions();
            if (definitions != null && definitions.size() > 0) {
                return doCreateKubernetesPodsControllersServices(service, options, config);
            }
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

    /**
     * Creates all the controllers, pods and services from the profile metadata
     */
    protected CreateKubernetesContainerMetadata doCreateKubernetesPodsControllersServices(FabricService service, CreateKubernetesContainerOptions options, KubernetesConfig config) {
        List<String> definitions = config.getDefinitions();
        String containerId = "TODO";
        byte[] json = null;
        Kubernetes kubernetes = getKubernetes();
        Objects.notNull(kubernetes, "kubernetes");
        for (String definition : definitions) {
            definition = definition.trim();
            if (!definition.contains(":")) {
                // lets assume its a file in the profile
                Set<String> profileIds = options.getProfiles();
                String versionId = options.getVersion();
                List<Profile> profiles = Profiles.getProfiles(service, profileIds, versionId);
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
                                    LOG.info("Creating a pod from " + definition);
                                    try {
                                        kubernetes.createPod(podSchema);
                                    } catch (Exception e) {
                                        LOG.error("Failed to create pod from " + definition + ". " + e + ". " + podSchema, e);
                                    }
                                } else if (Objects.equal("ReplicationController", kind)) {
                                    ReplicationControllerSchema replicationControllerSchema = objectMapper.reader(ReplicationControllerSchema.class).readValue(json);
                                    LOG.info("Creating a controller from " + definition);
                                    try {
                                        kubernetes.createReplicationController(replicationControllerSchema);
                                    } catch (Exception e) {
                                        LOG.error("Failed to create controller from " + definition + ". " + e + ". " + replicationControllerSchema, e);
                                    }
                                } else if (Objects.equal("Service", kind)) {
                                    ServiceSchema serviceSchema = objectMapper.reader(ServiceSchema.class).readValue(json);
                                    LOG.info("Creating a service from " + definition);
                                    try {
                                        kubernetes.createService(serviceSchema);
                                    } catch (Exception e) {
                                        LOG.error("Failed to create controller from " + definition + ". " + e + ". " + serviceSchema, e);
                                    }
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
        String status = "TODO";
        List<String> warnings = new ArrayList<>();
        CreateKubernetesContainerMetadata metadata = createKubernetesContainerMetadata(containerId, "kubelet", status, warnings);
        // TODO
        // publishZooKeeperValues(options, environmentVariables);
        return metadata;

    }

    @Override
    public void start(Container container) {
        assertValid();
        CreateKubernetesContainerMetadata containerMetadata = getContainerMetadata(container);
        CreateKubernetesContainerOptions options = containerMetadata.getCreateOptions();

        try {
            DockerCreateContainerParameters parameters = new DockerCreateContainerParameters(options).invoke();
            doCreateDockerContainer(options, parameters);
        } catch (Exception e) {
            String message = "Could not start pod: " + e + Dockers.dockerErrorMessage(e);
            LOG.warn(message, e);
            throw new RuntimeException(message, e);
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


    public static CreateKubernetesContainerMetadata createKubernetesContainerMetadata(DockerCreateOptions options, ContainerCreateStatus status, String containerType) {
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
        CreateKubernetesContainerMetadata metadata = createKubernetesContainerMetadata(containerId, containerType, statusId, warnings);
        metadata.setCreateOptions(options);
        return metadata;
    }

    public static CreateKubernetesContainerMetadata createKubernetesContainerMetadata(String containerId, String containerType, String statusId, List<String> warnings) {
        CreateKubernetesContainerMetadata metadata = new CreateKubernetesContainerMetadata(statusId, warnings);
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

    @Override
    public void stop(Container container) {
        assertValid();
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

    protected void deletePod(String id) throws Exception {
        LOG.info("stopping pod " + id);
        Kubernetes kubernetes = getKubernetes();
        Objects.notNull(kubernetes, "kubernetes");
        kubernetes.deletePod(id);
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
