/**
 * Copyright 2005-2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.maven;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.util.IntOrString;
import io.fabric8.maven.support.JsonSchema;
import io.fabric8.maven.support.JsonSchemaProperty;
import io.fabric8.maven.support.VolumeType;
import io.fabric8.openshift.api.model.template.ParameterBuilder;
import io.fabric8.openshift.api.model.template.Template;
import io.fabric8.openshift.api.model.template.TemplateBuilder;
import io.fabric8.utils.Files;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.fabric8.utils.PropertiesHelper.findPropertiesWithPrefix;

/**
 * Generates or copies the Kubernetes JSON file and attaches it to the build so its
 * installed and released to maven repositories like other build artifacts.
 */
@Mojo(name = "json", defaultPhase = LifecyclePhase.COMPILE)
public class JsonMojo extends AbstractFabric8Mojo {

    public static final String FABRIC8_PORT_HOST_PREFIX = "docker.port.host.";
    public static final String FABRIC8_PORT_CONTAINER_PREFIX = "docker.port.container.";
    public static final String FABRIC8_PORT_SERVICE = "fabric8.service.port";
    public static final String FABRIC8_CONTAINER_PORT_SERVICE = "fabric8.service.containerPort";
    public static final String FABRIC8_PROTOCOL_SERVICE = "fabric8.service.protocol";
    public static final String FABRIC8_PORT_SERVICE_PREFIX = FABRIC8_PORT_SERVICE + ".";
    public static final String FABRIC8_CONTAINER_PORT_SERVICE_PREFIX = FABRIC8_CONTAINER_PORT_SERVICE + ".";
    public static final String FABRIC8_PROTOCOL_SERVICE_PREFIX = FABRIC8_PROTOCOL_SERVICE + ".";


    private static final String NAME = "name";
    private static final String ATTRIBUTE_TYPE = "attributeType";
    
    private static final String VOLUME_MOUNT_PATH = "mountPath";
    private static final String VOLUME_REGEX = "fabric8.volume.(?<name>[^. ]*).(?<attributeType>[^. ]*)";
    private static final Pattern VOLUME_PATTERN = Pattern.compile(VOLUME_REGEX);

    private static final String PARAM_REGEX = "fabric8.parameter.(?<name>[^. ]*)(.)?(?<attributeType>[^ ]*)";
    private static final Pattern PARAM_PATTERN = Pattern.compile(PARAM_REGEX);

    private static final String TEMPLATE_NAME = "fabric8.template";
    private static final String PARAMETER_PREFIX = "fabric8.parameter";
    private static final String PARAMETER_NAME_PREFIX = PARAMETER_PREFIX + ".%s";
    private static final String PARAMETER_PROPERTY = PARAMETER_NAME_PREFIX + ".%s";
    
    private static final String GENEATE = "generate";
    private static final String FROM = "from";
    private static final String VALUE = "value";
    private static final String DESCRIPTION = "description";

    @Component
    private MavenProjectHelper projectHelper;

    /**
     * The artifact type for attaching the generated kubernetes json file to the project
     */
    @Parameter(property = "fabric8.kubernetes.artifactType", defaultValue = "json")
    private String artifactType = "json";

    /**
     * The artifact classifier for attaching the generated kubernetes json file to the project
     */
    @Parameter(property = "fabric8.kubernetes.artifactClassifier", defaultValue = "kubernetes")
    private String artifactClassifier = "kubernetes";

    /**
     * Whether or not we should generate the Kubernetes JSON file using the MVEL template if there is not one specified
     * in the build (usually in src/main/resources/kubernetes.json)
     */
    @Parameter(property = "fabric8.generateJson", defaultValue = "true")
    private boolean generateJson;

    /**
     * Whether we should combine kubernetes JSON dependencies on the classpath into the generated JSON
     */
    @Parameter(property = "fabric8.combineDependencies", defaultValue = "false")
    private boolean combineDependencies;

    /**
     * The labels passed into the generated Kubernetes JSON template.
     * <p/>
     * If no value is explicitly configured in the maven plugin then we use all maven properties starting with "fabric8.label."
     */
    @Parameter()
    private Map<String, String> labels;

    /**
     * The environment variables passed into the generated Kubernetes JSON template.
     * <p/>
     * If no value is explicitly configured in the maven plugin then we use all maven properties starting with "fabric8.env."
     */
    @Parameter()
    private List<EnvVar> environmentVariables;

    /**
     * The container ports passed into the generated Kubernetes JSON template.
     */
    @Parameter()
    private List<ContainerPort> containerPorts;

    /**
     * Maps the port names to the default container port numbers
     */
    @Parameter()
    private Map<String, Integer> defaultContainerPortMap;

    /**
     * The service ports passed into the generated Kubernetes JSON template.
     */
    @Parameter()
    private List<ServicePort> servicePorts;

    /**
     * The ID prefix used in the generated Kubernetes JSON template
     */
    @Parameter(property = "fabric8.replicas", defaultValue = "1")
    private Integer replicaCount;

    /**
     * The extra additional kubernetes JSON file for things like services
     */
    @Parameter(property = "kubernetesExtraJson", defaultValue = "${basedir}/target/classes/kubernetes-extra.json")
    private File kubernetesExtraJson;

    /**
     * The replication controller name used in the generated Kubernetes JSON template
     */
    @Parameter(property = "fabric8.replicationController.name", defaultValue = "${project.artifactId}-controller")
    private String replicationControllerName;

    /**
     * The name label used in the generated Kubernetes JSON template
     */
    @Parameter(property = "fabric8.kubernetes.name", defaultValue = "${project.artifactId}")
    private String kubernetesName;

    /**
     * The name label used in the generated Kubernetes JSON template
     */
    @Parameter(property = "fabric8.container.name", defaultValue = "${project.artifactId}-container")
    private String kubernetesContainerName;

    /**
     * The service name
     */
    @Parameter(property = "fabric8.service.name", defaultValue = "${project.artifactId}-service")
    private String serviceName;

    /**
     * The service port
     */
    @Parameter(property = FABRIC8_PORT_SERVICE)
    private Integer servicePort;

    /**
     * The service container port
     */
    @Parameter(property = FABRIC8_CONTAINER_PORT_SERVICE)
    private String serviceContainerPort;

    /**
     * The service protocol
     */
    @Parameter(property = FABRIC8_PROTOCOL_SERVICE, defaultValue = "TCP")
    private String serviceProtocol;

    /**
     * The docker image pull policy. If a SNAPSHOT dependency is used then this value defaults to <code>"PullAlways"</code>
     */
    @Parameter(property = "fabric8.imagePullPolicy")
    private String imagePullPolicy;

    /**
     * Whether the plugin should discover all the environment variable json schema files in the classpath and export those into the generated kubernetes JSON
     */
    @Parameter(property = "fabric8.includeAllEnvironmentVariables", defaultValue = "true")
    private boolean includeAllEnvironmentVariables;


    @Component
    protected ArtifactResolver resolver;

    @Parameter(property = "localRepository", readonly = true, required = true)
    protected ArtifactRepository localRepository;

    @Parameter(property = "project.remoteArtifactRepositories")
    protected List remoteRepositories;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File json = getKubernetesJson();
        getLog().info("Configured with file: " + json);
        if (json == null) {
            throw new MojoExecutionException("No kubernetes json file is specified!");
        }
        if (shouldGenerateForThisProject()) {
            if (!isIgnoreProject() || combineDependencies) {
                if (combineDependencies) {
                    combineDependentJsonFiles(json);
                } else if (generateJson) {
                    generateKubernetesJson(json);

                    if (kubernetesExtraJson != null && kubernetesExtraJson.exists()) {
                        combineJsonFiles(json, kubernetesExtraJson);
                    }
                }
            }
        }
        if (Files.isFile(json)) {
            printSummary(json);

            getLog().info("Attaching kubernetes json file: " + json + " to the build");
            projectHelper.attachArtifact(getProject(), artifactType, artifactClassifier, json);
        }
    }

    @Override
    protected boolean shouldGenerateForThisProject() {
        return super.shouldGenerateForThisProject() || combineDependencies;
    }

    protected void combineDependentJsonFiles(File json) throws MojoExecutionException {
        try {
            MavenProject project = getProject();
            Set<File> jsonFiles = new LinkedHashSet<>();
            List<Dependency> dependencies = project.getDependencies();
            Set<Artifact> dependencyArtifacts = project.getDependencyArtifacts();
            for (Artifact artifact : dependencyArtifacts) {
                String classifier = artifact.getClassifier();
                String type = artifact.getType();
                File file = artifact.getFile();

                if (isKubernetesJsonArtifact(classifier, type)) {
                    if (file != null) {
                        System.out.println("Found kubernetes JSON dependency: " + artifact);
                        jsonFiles.add(file);
                    } else {
                        Set<Artifact> artifacts = resolveArtifacts(artifact);
                        for (Artifact resolvedArtifact : artifacts) {
                            classifier = resolvedArtifact.getClassifier();
                            type = resolvedArtifact.getType();
                            file = resolvedArtifact.getFile();
                            if (isKubernetesJsonArtifact(classifier, type) && file != null) {
                                System.out.println("Resolved kubernetes JSON dependency: " + artifact);
                                jsonFiles.add(file);
                            }
                        }
                    }
                }
            }
            List<Object> jsonObjectList = new ArrayList<>();
            for (File file : jsonFiles) {
                addKubernetesJsonFileToList(jsonObjectList, file);
            }
            if (jsonObjectList.isEmpty()) {
                throw new MojoExecutionException("Could not find any dependent kubernetes JSON files!");
            }
            Object combinedJson = null;
            if (jsonObjectList.size() == 1) {
                combinedJson = jsonObjectList.get(0);
            } else {
                combinedJson = KubernetesHelper.combineJson(jsonObjectList.toArray());
            }
            json.getParentFile().mkdirs();
            KubernetesHelper.saveJson(json, combinedJson);
            getLog().info("Saved as :" + json.getAbsolutePath());
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to save combined JSON files " + json + " and " + kubernetesExtraJson + " as " + json + ". " + e, e);
        }
    }

    protected void printSummary(File json) throws MojoExecutionException {
        try {
            Object savedObjects = KubernetesHelper.loadJson(json);
            getLog().info("Generated Kubernetes JSON resources:");
            printSummary(KubernetesHelper.toItemList(savedObjects));
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to load saved json file " + json + ". Reason: " + e, e);
        }
    }

    protected void printSummary(List<Object> list) {
        for (Object object : list) {
            if (object != null) {
                if (object instanceof List) {
                    printSummary((List<Object>) object);
                } else {
                    String kind = object.getClass().getSimpleName();
                    String id = KubernetesHelper.getObjectId(object);
                    getLog().info("    " + kind + " " + id + " " + KubernetesHelper.summaryText(object));
                }
            }
        }
    }


    private void addKubernetesJsonFileToList(List<Object> list, File file) {
        if (file.exists() && file.isFile()) {
            try {
                Object jsonObject = loadJsonFile(file);
                if (jsonObject != null) {
                    list.add(jsonObject);
                }
            } catch (MojoExecutionException e) {
                getLog().warn("Failed to parse file " + file + ". " + e, e);
            }

        } else {
            getLog().warn("Ignoring missing file " + file);
        }
    }

    protected static boolean isKubernetesJsonArtifact(String classifier, String type) {
        return Objects.equal("json", type) && Objects.equal("kubernetes", classifier);
    }

    protected Set<Artifact> resolveArtifacts(Artifact artifact) {
        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(artifact);
        addNeededRemoteRepository();
        request.setRemoteRepositories(remoteRepositories);
        request.setLocalRepository(localRepository);

        ArtifactResolutionResult resolve = resolver.resolve(request);
        return resolve.getArtifacts();
    }


    @SuppressWarnings("unchecked")
    private void addNeededRemoteRepository() {
        // TODO: Remove this code when we use releases from Maven Central
        // included jboss-fs repo which is required until we use an Apache version of Karaf
        boolean found = false;
        if (remoteRepositories != null) {
            for (Object obj : remoteRepositories) {
                if (obj instanceof ArtifactRepository) {
                    ArtifactRepository repo = (ArtifactRepository) obj;
                    if (repo.getUrl().contains("repository.jboss.org/nexus/content/groups/fs-public")) {
                        found = true;
                        getLog().debug("Found existing (" + repo.getId() + ") remote repository: " + repo.getUrl());
                        break;
                    }
                }
            }
        }
        if (!found) {
            ArtifactRepository fsPublic = new MavenArtifactRepository();
            fsPublic.setUrl("http://repository.jboss.org/");
            fsPublic.setLayout(new DefaultRepositoryLayout());
            fsPublic.setReleaseUpdatePolicy(new ArtifactRepositoryPolicy(true, "never", "warn"));
            fsPublic.setSnapshotUpdatePolicy(new ArtifactRepositoryPolicy(false, "never", "ignore"));
            if (remoteRepositories == null) {
                remoteRepositories = new ArrayList();
            }
            remoteRepositories.add(fsPublic);
        }
    }


    protected void combineJsonFiles(File json, File kubernetesExtraJson) throws MojoExecutionException {
        // lets combine json files together
        getLog().info("Combining generated json " + json + " with extra json " + kubernetesExtraJson);
        Object extra = loadJsonFile(kubernetesExtraJson);
        Object generated = loadJsonFile(json);
        try {
            JsonNode combinedJson = KubernetesHelper.combineJson(generated, extra);
            KubernetesHelper.saveJson(json, combinedJson);
            getLog().info("Saved as :" + json.getAbsolutePath());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to save combined JSON files " + json + " and " + kubernetesExtraJson + " as " + json + ". " + e, e);
        }
    }

    protected static Object loadJsonFile(File file) throws MojoExecutionException {
        try {
            return KubernetesHelper.loadJson(file);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to parse JSON " + file + ". " + e, e);
        }
    }

    protected void generateKubernetesJson(File kubernetesJson) throws MojoExecutionException {
        // TODO populate properties, project etc.
        MavenProject project = getProject();
        Properties properties = project.getProperties();
        Map<String, Object> variables = new HashMap<>();
        Set<Map.Entry<Object, Object>> entries = properties.entrySet();
        for (Map.Entry<Object, Object> entry : entries) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (key instanceof String) {
                String keyText = key.toString();
                // lets replace dots so we can access properties directly inside MVEL
                keyText = keyText.replace('.', '_');
                variables.put(keyText, value);
            }
        }
        Map<String, String> labelMap = getLabels();
        String name = getKubernetesName();
        if (labelMap.isEmpty() && Strings.isNotBlank(name)) {
            // lets add a default label
            labelMap.put("component", name);
        }


        KubernetesListBuilder builder = new KubernetesListBuilder()
                .withId(name)
                .addNewReplicationController()
                .withId(KubernetesHelper.validateKubernetesId(replicationControllerName, "fabric8.replicationController.name"))
                .withLabels(labels)
                .withNewDesiredState()
                .withReplicas(replicaCount)
                .withReplicaSelector(labelMap)
                .withNewPodTemplate()
                .withLabels(labelMap)
                .withNewDesiredState()
                .withNewManifest()
                .addNewContainer()
                .withName(getKubernetesContainerName())
                .withImage(getDockerImage())
                .withImagePullPolicy(getImagePullPolicy())
                .withEnv(getEnvironmentVariables())
                .withPorts(getContainerPorts())
                .withVolumeMounts(getVolumeMounts())
                .endContainer()
                .withVolumes(getVolumes())
                .endManifest()
                .endDesiredState()
                .endPodTemplate()
                .endDesiredState()
                .endReplicationController();

        // Do we actually want to generate a service manifest?
        if (serviceName != null) {
            ServiceBuilder serviceBuilder = new ServiceBuilder()
                    .withId(serviceName)
                    .withSelector(labelMap)
                    .withLabels(labelMap);

            List<ServicePort> servicePorts = getServicePorts();
            if (servicePorts != null & !servicePorts.isEmpty()) {
                serviceBuilder.withPorts(servicePorts);
            } else {
                serviceBuilder.withPortalIP("None");
            }

            builder = builder.addToServices(serviceBuilder.build());
        }
        
        Template template = getTemplate();
        if (!template.getParameters().isEmpty()) {
            builder = builder.addToTemplates(template);
        }

        KubernetesList kubernetesList = builder.build();

        try {
            ObjectMapper mapper = new ObjectMapper()
                    .enable(SerializationFeature.INDENT_OUTPUT);
            String generated = mapper.writeValueAsString(kubernetesList);
            Files.writeToFile(kubernetesJson, generated, Charset.defaultCharset());
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to generate Kubernetes JSON.", e);
        }
    }

    public String getDockerImage() {
        MavenProject project = getProject();
        return project.getProperties().getProperty("docker.image");
    }

    public String getImagePullPolicy() {
        MavenProject project = getProject();
        String pullPolicy = imagePullPolicy;
        if (project != null) {
            String version = project.getVersion();
            if (Strings.isNullOrBlank(pullPolicy)) {
                if (version != null && version.endsWith("SNAPSHOT")) {
                    pullPolicy = "PullAlways";
                }
            }
        }
        return pullPolicy;
    }

    public String getKubernetesContainerName() {
        if (Strings.isNullOrBlank(kubernetesContainerName)) {
            // lets generate it from the docker user and the camelCase artifactId
            String groupPrefix = null;
            MavenProject project = getProject();
            String imageName = project.getProperties().getProperty("docker.image");
            if (Strings.isNotBlank(imageName)) {
                String[] paths = imageName.split("/");
                if (paths != null) {
                    if (paths.length == 2) {
                        groupPrefix = paths[0];
                    } else if (paths.length == 3) {
                        groupPrefix = paths[1];
                    }
                }
            }
            if (Strings.isNullOrBlank(groupPrefix)) {
                groupPrefix = project.getGroupId();
            }
            kubernetesContainerName = groupPrefix + "-" + project.getArtifactId();
        }
        return kubernetesContainerName;
    }

    public void setKubernetesContainerName(String kubernetesContainerName) {
        this.kubernetesContainerName = kubernetesContainerName;
    }

    public String getKubernetesName() {
        return kubernetesName;
    }

    public void setKubernetesName(String kubernetesName) {
        this.kubernetesName = kubernetesName;
    }

    public Map<String, Integer> getDefaultContainerPortMap() {
        if (defaultContainerPortMap == null) {
            defaultContainerPortMap = new HashMap<>();
        }
        if (defaultContainerPortMap.isEmpty()) {
            // lets populate default values
            defaultContainerPortMap.put("jolokia", 8778);
            defaultContainerPortMap.put("web", 8080);
        }
        return defaultContainerPortMap;
    }

    public void setDefaultContainerPortMap(Map<String, Integer> defaultContainerPortMap) {
        this.defaultContainerPortMap = defaultContainerPortMap;
    }

    public List<ContainerPort> getContainerPorts() {
        if (containerPorts == null) {
            containerPorts = new ArrayList<>();
        }
        if (containerPorts.isEmpty()) {
            Map<String, ContainerPort> portMap = new HashMap<>();
            Properties properties1 = getProject().getProperties();
            Map<String, String> hostPorts = findPropertiesWithPrefix(properties1, FABRIC8_PORT_HOST_PREFIX);
            Properties properties = getProject().getProperties();
            Map<String, String> containerPortsMap = findPropertiesWithPrefix(properties, FABRIC8_PORT_CONTAINER_PREFIX);

            for (Map.Entry<String, String> entry : containerPortsMap.entrySet()) {
                String name = entry.getKey();
                String portText = entry.getValue();
                Integer portNumber = parsePort(portText, FABRIC8_PORT_CONTAINER_PREFIX + name);
                if (portNumber != null) {
                    ContainerPort port = getOrCreatePort(portMap, name);
                    port.setContainerPort(portNumber);
                    port.setName(name);
                }
            }
            for (Map.Entry<String, String> entry : hostPorts.entrySet()) {
                String name = entry.getKey();
                String portText = entry.getValue();
                Integer portNumber = parsePort(portText, FABRIC8_PORT_HOST_PREFIX + name);
                if (portNumber != null) {
                    ContainerPort port = getOrCreatePort(portMap, name);
                    port.setHostPort(portNumber);

                    // if the container port isn't set, lets try default that using defaults
                    if (port.getContainerPort() == null) {
                        port.setContainerPort(getDefaultContainerPortMap().get(name));
                    }
                }
            }
            getLog().info("Generated port mappings: " + portMap);
            getLog().debug("from host ports: " + hostPorts);
            getLog().debug("from containerPorts ports: " + containerPorts);
            containerPorts.addAll(portMap.values());
        }
        return containerPorts;
    }

    protected static ContainerPort getOrCreatePort(Map<String, ContainerPort> portMap, String name) {
        ContainerPort answer = portMap.get(name);
        if (answer == null) {
            answer = new ContainerPort();
            portMap.put(name, answer);

            // TODO should we set the name?
            // answer.setName(name);
        }
        return answer;
    }

    public List<ServicePort> getServicePorts() {
        if (servicePorts == null) {
            servicePorts = new ArrayList<>();
        }
        if (servicePorts.isEmpty()) {
            if (serviceContainerPort != null && servicePort != null) {
                ServicePort actualServicePort = new ServicePort();
                Integer containerPortNumber = parsePort(serviceContainerPort, FABRIC8_CONTAINER_PORT_SERVICE);
                IntOrString containerPort = new IntOrString();
                if (containerPortNumber != null) {
                    containerPort.setIntVal(containerPortNumber);
                } else {
                    containerPort.setStrVal(serviceContainerPort);
                }
                actualServicePort.setContainerPort(containerPort);
                actualServicePort.setPort(servicePort);
                if (serviceProtocol != null) {
                    actualServicePort.setProtocol(serviceProtocol);
                    servicePorts.add(actualServicePort);
                }
            }

            Properties properties1 = getProject().getProperties();
            Map<String, String> servicePortProperties = findPropertiesWithPrefix(properties1, FABRIC8_PORT_SERVICE_PREFIX);
            Map<String, String> serviceContainerPortProperties = findPropertiesWithPrefix(properties1, FABRIC8_CONTAINER_PORT_SERVICE_PREFIX);
            Map<String, String> serviceProtocolProperties = findPropertiesWithPrefix(properties1, FABRIC8_PROTOCOL_SERVICE_PREFIX);

            for (Map.Entry<String, String> entry : servicePortProperties.entrySet()) {
                String name = entry.getKey();
                String servicePortText = entry.getValue();
                Integer servicePortNumber = parsePort(servicePortText, FABRIC8_PORT_SERVICE_PREFIX + name);
                if (servicePortNumber != null) {
                    String containerPort = serviceContainerPortProperties.get(name);
                    if (Strings.isNullOrBlank(containerPort)) {
                        getLog().warn("Missing container port for service - need to specify " + FABRIC8_CONTAINER_PORT_SERVICE_PREFIX + name + " property");
                    } else {
                        ServicePort servicePort = new ServicePort();
                        servicePort.setName(name);
                        servicePort.setPort(servicePortNumber);

                        IntOrString containerPortSpec = new IntOrString();
                        Integer containerPortNumber = parsePort(containerPort, FABRIC8_CONTAINER_PORT_SERVICE_PREFIX + name);
                        if (containerPortNumber != null) {
                            containerPortSpec.setIntVal(containerPortNumber);
                        } else {
                            containerPortSpec.setStrVal(containerPort);
                        }
                        servicePort.setContainerPort(containerPortSpec);

                        String portProtocol = serviceProtocolProperties.get(name);
                        if (portProtocol != null) {
                            servicePort.setProtocol(portProtocol);
                        }

                        servicePorts.add(servicePort);
                    }
                }
            }
        }
        return servicePorts;
    }

    protected static EnvVar getOrCreateEnv(Map<String, EnvVar> envMap, String name) {
        EnvVar answer = envMap.get(name);
        if (answer == null) {
            answer = new EnvVar();
            envMap.put(name, answer);
        }
        return answer;
    }

    protected Integer parsePort(String portText, String propertyName) {
        if (Strings.isNotBlank(portText)) {
            try {
                return Integer.parseInt(portText);
            } catch (NumberFormatException e) {
                getLog().debug("Failed to parse port text: " + portText + " from maven property " + propertyName + ". " + e, e);
            }
        }
        return null;
    }

    public void setContainerPorts(List<ContainerPort> ports) {
        this.containerPorts = ports;
    }

    public void setServicePorts(List<ServicePort> ports) {
        this.servicePorts = ports;
    }

    public Map<String, String> getLabels() {
        if (labels == null) {
            labels = new HashMap<>();
        }
        if (labels.isEmpty()) {
            labels = findPropertiesWithPrefix(getProject().getProperties(), "fabric8.label.", Strings.toLowerCaseFunction());
        }
        return labels;
    }

    public List<EnvVar> getEnvironmentVariables() throws MojoExecutionException {
        if (environmentVariables == null) {
            environmentVariables = new ArrayList<EnvVar>();
        }
        if (environmentVariables.isEmpty()) {
            Map<String, EnvVar> envMap = new HashMap<>();
            Map<String, String> envs = getExportedEnvironmentVariables();

            for (Map.Entry<String, String> entry : envs.entrySet()) {
                String name = entry.getKey();
                String value = entry.getValue();

                if (name != null) {
                    EnvVar env = getOrCreateEnv(envMap, name);
                    env.setName(name);

                    if (env.getValue() == null) {
                        env.setValue(value);
                    }
                }
            }
            getLog().info("Generated env mappings: " + envMap);
            getLog().debug("from envs: " + envs);
            environmentVariables.addAll(envMap.values());
        }
        return environmentVariables;
    }

    public Map<String, String> getExportedEnvironmentVariables() throws MojoExecutionException {
        if (includeAllEnvironmentVariables) {
            try {
                JsonSchema schema = getEnvironmentVariableJsonSchema();
                Map<String, String> answer = new TreeMap<>();
                Map<String, JsonSchemaProperty> properties = schema.getProperties();
                Set<Map.Entry<String, JsonSchemaProperty>> entries = properties.entrySet();
                for (Map.Entry<String, JsonSchemaProperty> entry : entries) {
                    String name = entry.getKey();
                    String value = entry.getValue().getDefaultValue();
                    if (value == null) {
                        value = "";
                    }
                    answer.put(name, value);
                }
                Map<String, String> mavenEnvVars = getEnvironmentVariableProperties();
                answer.putAll(mavenEnvVars);
                return answer;
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to load environment variable json schema files: " + e, e);
            }
        } else {
            return getEnvironmentVariableProperties();
        }
    }

    public List<VolumeMount> getVolumeMounts() {
        List<VolumeMount> volumeMount = new ArrayList<>();
        MavenProject project = getProject();
        for (Map.Entry<Object, Object> entry : project.getProperties().entrySet()) {
            Object key = entry.getKey();
            if (key instanceof String) {
                String s = (String) key;
                Matcher m = VOLUME_PATTERN.matcher(s);
                if (m.matches()) {
                    String name = m.group(NAME);
                    String type = m.group(ATTRIBUTE_TYPE);
                    if (type.equals(VOLUME_MOUNT_PATH)) {
                        String path = String.valueOf(entry.getValue());
                        volumeMount.add(new VolumeMountBuilder()
                                .withName(name)
                                .withMountPath(path)
                                .withReadOnly(false).build());
                    }
                }
            }
        }
        return volumeMount;
    }

    public List<Volume> getVolumes() {
        List<Volume> volumes = new ArrayList<>();
        MavenProject project = getProject();
        Properties properties = project.getProperties();

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            Object key = entry.getKey();
            if (key instanceof String) {
                String s = (String) key;
                Matcher m = VOLUME_PATTERN.matcher(s);
                if (m.matches()) {
                    String name = m.group(NAME);
                    String type = m.group(ATTRIBUTE_TYPE);
                    VolumeType volumeType = VolumeType.typeFor(type);
                    if (volumeType != null) {
                        volumes.add(volumeType.fromProperties(name, properties));
                    }
                }
            }
        }
        return volumes;
    }

    public Template getTemplate() {
        List<io.fabric8.openshift.api.model.template.Parameter> parameters = new ArrayList<>();
        MavenProject project = getProject();
        Properties properties = project.getProperties();
        for (Map.Entry<Object, Object> entry : project.getProperties().entrySet()) {
            Object key = entry.getKey();
            if (key instanceof String) {
                String s = (String) key;
                Matcher m = PARAM_PATTERN.matcher(s);
                if (m.matches()) {
                    String name = m.group(NAME);
                    String value = properties.getProperty(String.format(PARAMETER_PROPERTY, name, VALUE));
                    String from = properties.getProperty(String.format(PARAMETER_PROPERTY, name, FROM));
                    String description = properties.getProperty(String.format(PARAMETER_PROPERTY, name, DESCRIPTION));
                    String generate = properties.getProperty(String.format(PARAMETER_PROPERTY, name, GENEATE));
                    //If neither value nor from has been specified read the value inline.
                    if (Strings.isNullOrBlank(value) && Strings.isNullOrBlank(from)) {
                        value = properties.getProperty(String.format(PARAMETER_NAME_PREFIX, name));
                    }
                    parameters.add(new ParameterBuilder()
                            .withName(name)
                            .withFrom(from)
                            .withValue(value)
                            .withGenerate(generate)
                            .withDescription(description)
                            .build());
                }
            }
        }
        String templateName = properties.containsKey(TEMPLATE_NAME) ? 
                String.valueOf(properties.getProperty(TEMPLATE_NAME)) :
                project.getArtifactId();
        return new TemplateBuilder().withName(templateName).withParameters(parameters).build();
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    protected static void addIfNotDefined(Map<String, Object> variables, String key, String value) {
        if (!variables.containsKey(key)) {
            variables.put(key, value);
        }
    }
}
