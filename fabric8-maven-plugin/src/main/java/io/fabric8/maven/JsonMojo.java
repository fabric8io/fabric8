/**
 * Copyright 2005-2015 Red Hat, Inc.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.extensions.Templates;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.util.IntOrString;
import io.fabric8.maven.support.JsonSchema;
import io.fabric8.maven.support.JsonSchemaProperty;
import io.fabric8.maven.support.VolumeType;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.ImageStreamBuilder;
import io.fabric8.openshift.api.model.template.ParameterBuilder;
import io.fabric8.openshift.api.model.template.Template;
import io.fabric8.openshift.api.model.template.TemplateBuilder;
import io.fabric8.utils.*;
import io.fabric8.utils.Objects;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.fabric8.kubernetes.api.KubernetesHelper.getName;
import static io.fabric8.kubernetes.api.KubernetesHelper.getOrCreateMetadata;
import static io.fabric8.kubernetes.api.KubernetesHelper.setName;
import static io.fabric8.utils.Files.guessMediaType;
import static io.fabric8.utils.PropertiesHelper.findPropertiesWithPrefix;

/**
 * Generates or copies the Kubernetes JSON file and attaches it to the build so its
 * installed and released to maven repositories like other build artifacts.
 */
@Mojo(name = "json", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
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

    private static final String GENERATE = "generate";
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
     * Should we fail the build if no json files could be found
     */
    @Parameter(property = "fabric8.failOnMissingJsonFiles", defaultValue = "true")
    private boolean failOnMissingJsonFiles;

    /**
     * Whether we should include the namespace in the containers' env vars
     */
    @Parameter(property = "fabric8.includeNamespaceEnvVar", defaultValue = "true")
    private boolean includeNamespaceEnvVar;

    /**
     * The name of the env var to add that will contain the namespace at container runtime
     */
    @Parameter(property = "fabric8.namespaceEnvVar", defaultValue = "KUBERNETES_NAMESPACE")
    private String kubernetesNamespaceEnvVar;

    /**
     * The provider to include as a label. Set to empty to disable.
     */
    @Parameter(property = "fabric8.provider", defaultValue = "fabric8")
    private String provider;

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
     * Should we wrap the generated ReplicationController objects in a DeploymentConfig
     */
    // TODO lets disable by default until its working :)
    @Parameter(property = "fabric8.useDeploymentConfig", defaultValue = "false")
    private boolean useDeploymentConfig;

    /**
     * The last triggered image tag if generating a DeploymentConfig
     */
    @Parameter(property = "fabric8.lastTriggeredImageTag", defaultValue = "latest")
    private String lastTriggeredImageTag;

    /**
     * The strategy name for the DeploymentConfig
     */
    @Parameter(property = "fabric8.deploymentStrategy", defaultValue = "Recreate")
    private String deploymentStrategy;

    /**
     * The extra additional kubernetes JSON file for things like services
     */
    @Parameter(property = "fabric8.extra.json", defaultValue = "${basedir}/target/classes/kubernetes-extra.json")
    private File kubernetesExtraJson;

    /**
     * Temporary directory used for creating the template annotations
     */
    @Parameter(property = "fabric8.templateTempDir", defaultValue = "${basedir}/target/fabric8/template-workdir")
    private File templateTempDir;

    /**
     * The URL to use to link to the icon in the generated Template.
     * <p/>
     * For using a common set of icons, see the {@link #iconRef} option.
     */
    @Parameter(property = "fabric8.iconUrl")
    private String iconUrl;

    /**
     * The URL prefix added to the relative path of the icon file
     */
    @Parameter(property = "fabric8.iconUrlPrefix")
    private String iconUrlPrefix;

    /**
     * The SCM branch used when creating a URL to the icon file
     */
    @Parameter(property = "fabric8.iconBranch", defaultValue = "master")
    private String iconBranch;

    /**
     * The replication controller name used in the generated Kubernetes JSON template
     */
    @Parameter(property = "fabric8.replicationController.name", defaultValue = "${project.artifactId}")
    private String replicationControllerName;

    /**
     * The name label used in the generated Kubernetes JSON template
     */
    @Parameter(property = "fabric8.kubernetes.name", defaultValue = "${project.artifactId}")
    private String kubernetesName;

    /**
     * The name label used in the generated Kubernetes JSON template
     */
    @Parameter(property = "fabric8.container.name", defaultValue = "${project.artifactId}")
    private String kubernetesContainerName;

    /**
     * The service name
     */
    @Parameter(property = "fabric8.service.name", defaultValue = "${project.artifactId}")
    private String serviceName;

    /**
     * Should we generate headless services (services with no ports)
     */
    // TODO for now lets default to not creating headless services as it barfs when used with kubernetes...
    //@Parameter(property = "fabric8.service.headless", defaultValue = "true")
    @Parameter(property = "fabric8.service.headless", defaultValue = "false")
    private boolean headlessServices;

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
     * The docker image pull policy for non-snapshots
     */
    @Parameter(property = "fabric8.imagePullPolicy")
    private String imagePullPolicy;

    /**
     * The docker image pull policy for snapshot releases (which should pull always)
     */
    @Parameter(property = "fabric8.imagePullPolicySnapshot")
    private String imagePullPolicySnapshot;

    /**
     * Whether the plugin should discover all the environment variable json schema files in the classpath and export those into the generated kubernetes JSON
     */
    @Parameter(property = "fabric8.includeAllEnvironmentVariables", defaultValue = "true")
    private boolean includeAllEnvironmentVariables;

    @Parameter(property = "fabric8.containerPrivileged")
    protected Boolean containerPrivileged;

    @Parameter(property = "fabric8.serviceAccount")
    protected String serviceAccount;

    /**
     * The properties file used to specify the OpenShift Template parameter values and descriptions. The properties file should be of the form
     * <code>
     *     <pre>
     *         FOO.value = ABC
     *         FOO.description = this is the description of FOO
     *     </pre>
     * </code>
     */
    @Parameter(property = "fabric8.templateParametersFile", defaultValue = "${basedir}/src/main/fabric8/templateParameters.properties")
    protected File templateParametersPropertiesFile;

    /**
     * Defines the maximum size in kilobytes that the data encoded URL of the icon should be before we defer
     * and try to use an external URL
     */
    @Parameter(property = "fabric8.maximumDataUrlSizeK", defaultValue = "2")
    private int maximumDataUrlSizeK;

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
                if (json.exists() && json.isFile()) {
                    if (useDeploymentConfig) {
                        wrapInDeploymentConfigs(json);
                    }
                }
            }
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
                if (failOnMissingJsonFiles) {
                    throw new MojoExecutionException("Could not find any dependent kubernetes JSON files!");
                } else {
                    getLog().warn("Could not find any dependent kubernetes JSON files");
                    return;
                }
            }
            Object combinedJson;
            if (jsonObjectList.size() == 1) {
                combinedJson = jsonObjectList.get(0);
            } else {
                combinedJson = KubernetesHelper.combineJson(jsonObjectList.toArray());
            }
            if (combinedJson instanceof Template) {
                Template template = (Template) combinedJson;
                setName(template, getKubernetesName());
                configureTemplateDescriptionAndIcon(template, getIconUrl());
            }
            json.getParentFile().mkdirs();
            KubernetesHelper.saveJson(json, combinedJson);
            getLog().info("Saved as :" + json.getAbsolutePath());
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to save combined JSON files " + json + " and " + kubernetesExtraJson + " as " + json + ". " + e, e);
        }
    }

    private void addKubernetesJsonFileToList(List<Object> list, File file) {
        if (file.exists() && file.isFile()) {
            try {
                Object jsonObject = loadJsonFile(file);
                if (jsonObject != null) {
                    list.add(jsonObject);
                } else {
                    getLog().warn("No object found for file: " + file);
                }
            } catch (MojoExecutionException e) {
                getLog().warn("Failed to parse file " + file + ". " + e, e);
            }

        } else {
            getLog().warn("Ignoring missing file " + file);
        }
    }

    protected Set<Artifact> resolveArtifacts(Artifact artifact) {
        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(artifact);
        request.setRemoteRepositories(remoteRepositories);
        request.setLocalRepository(localRepository);

        ArtifactResolutionResult resolve = resolver.resolve(request);
        return resolve.getArtifacts();
    }

    protected void combineJsonFiles(File json, File kubernetesExtraJson) throws MojoExecutionException {
        // lets combine json files together
        getLog().info("Combining generated json " + json + " with extra json " + kubernetesExtraJson);
        Object extra = loadJsonFile(kubernetesExtraJson);
        Object generated = loadJsonFile(json);
        try {
            Object combinedJson = KubernetesHelper.combineJson(generated, extra);
            KubernetesHelper.saveJson(json, combinedJson);
            getLog().info("Saved as :" + json.getAbsolutePath());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to save combined JSON files " + json + " and " + kubernetesExtraJson + " as " + json + ". " + e, e);
        }
    }

    protected void wrapInDeploymentConfigs(File json) throws MojoExecutionException {
        try {
            Object dto = loadJsonFile(json);
            if (dto instanceof KubernetesList) {
                KubernetesList container = (KubernetesList) dto;
                List<HasMetadata> items = container.getItems();
                items = wrapInDeploymentConfigs(items);
                getLog().info("Wrapped in DeploymentConfigs:");
                printSummary(items);
                container.setItems(items);
                KubernetesHelper.saveJson(json, container);
            } else if (dto instanceof Template) {
                Template container = (Template) dto;
                List<HasMetadata> items = container.getObjects();
                items = wrapInDeploymentConfigs(items);
                getLog().info("Wrapped in DeploymentConfigs:");
                printSummary(items);
                container.setObjects(items);
                getLog().info("Template is now:");
                printSummary(container.getObjects());
                KubernetesHelper.saveJson(json, container);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to save combined JSON files " + json + " and " + kubernetesExtraJson + " as " + json + ". " + e, e);
        }
    }

    protected List<HasMetadata> wrapInDeploymentConfigs(List<HasMetadata> items) {
        List<HasMetadata> answer = new ArrayList<>();
        for (HasMetadata item : items) {
            if (item instanceof ReplicationController) {
                ReplicationController replicationController = (ReplicationController) item;
                wrapInDeploymentConfigs(answer, replicationController);
            } else {
                answer.add(item);
            }
        }
        return answer;
    }

    /**
     * Wraps the given {@link ReplicationController} in a {@link DeploymentConfig} and adds it to the given list
     * along with any other required entities
     */
    protected void wrapInDeploymentConfigs(List<HasMetadata> list, ReplicationController replicationController) {
        DeploymentConfigBuilder builder = new DeploymentConfigBuilder();

        String name = getName(replicationController);
        if (Strings.isNotBlank(name)) {
            name = Strings.stripSuffix(name, "-controller");
        }
        if (Strings.isNullOrBlank(name)) {
            name = getProject().getArtifactId();
        }
        String deploymentName = name;
        String imageStream = name;


        Map<String, String> labels = KubernetesHelper.getLabels(replicationController);
        builder = builder.withNewMetadata().withName(deploymentName).withLabels(labels).endMetadata();

        ReplicationControllerSpec spec = replicationController.getSpec();
        if (spec != null) {
            List<String> containerNames = new ArrayList<>();
            PodTemplateSpec podTemplateSpec = spec.getTemplate();
            if (podTemplateSpec != null) {
                PodSpec podSpec = podTemplateSpec.getSpec();
                if (podSpec != null) {
                    List<Container> containers = podSpec.getContainers();
                    if (containers != null) {
                        for (Container container : containers) {
                            String containerName = container.getName();
                            if (Strings.isNotBlank(containerName)) {
                                containerNames.add(containerName);
                            }
                        }
                    }
                }
            }
            getOrAddImageStream(list, imageStream, labels);
            builder = builder.withNewSpec().
                    withTemplate(podTemplateSpec).withReplicas(spec.getReplicas()).withSelector(spec.getSelector()).
                    withNewStrategy().
                        withType(deploymentStrategy).
                        endStrategy().
                    addNewTrigger().
                        withType("ImageChange").
                        withNewImageChangeParams().
                            withAutomatic(true).
                            withContainerNames(containerNames).
                            withNewFrom().withName(imageStream + ":" + lastTriggeredImageTag).endFrom().
                            withLastTriggeredImage(lastTriggeredImageTag).
                        endImageChangeParams().
                    endTrigger().
                    endSpec();
        }
        DeploymentConfig config = builder.build();
        list.add(config);
    }

    protected ImageStream getOrAddImageStream(List<HasMetadata> list, String imageStreamName, Map<String, String> labels) {
        for (HasMetadata item : list) {
            if (item instanceof ImageStream) {
                ImageStream stream = (ImageStream) item;
                if (Objects.equal(imageStreamName, getName(stream))) {
                    return stream;
                }
            }
        }
        ImageStream imageStream = new ImageStreamBuilder().withNewMetadata().withName(imageStreamName).withLabels(labels).endMetadata().build();
        list.add(imageStream);
        return imageStream;
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
        Map<String, String> labelMap = getLabels();
        String name = getKubernetesName();
        if (labelMap.isEmpty() && Strings.isNotBlank(name)) {
            // lets add a default label
            labelMap.put("component", name);
        }
        if (!labelMap.containsKey("provider") && Strings.isNotBlank(provider)) {
            labelMap.put("provider", provider);
        }

        KubernetesListBuilder builder = new KubernetesListBuilder()
                .addNewReplicationControllerItem()
                .withNewMetadata()
                .withName(KubernetesHelper.validateKubernetesId(replicationControllerName, "fabric8.replicationController.name"))
                .withLabels(labelMap)
                .endMetadata()
                .withNewSpec()
                .withReplicas(replicaCount)
                .withSelector(labelMap)
                .withNewTemplate()
                .withNewMetadata()
                .withLabels(labelMap)
                .endMetadata()
                .withNewSpec()
                .withServiceAccount(serviceAccount)
                .addNewContainer()
                .withName(getKubernetesContainerName())
                .withImage(getDockerImage())
                .withImagePullPolicy(getImagePullPolicy())
                .withEnv(getEnvironmentVariables())
                .withNewSecurityContext()
                .withPrivileged(getContainerPrivileged())
                .endSecurityContext()
                .withPorts(getContainerPorts())
                .withVolumeMounts(getVolumeMounts())
                .withLivenessProbe(getLivenessProbe())
                .withReadinessProbe(getReadinessProbe())
                .endContainer()
                .withVolumes(getVolumes())
                .endSpec()
                .endTemplate()
                .endSpec()
                .endReplicationControllerItem();

        // Do we actually want to generate a service manifest?
        if (serviceName != null) {
            ServiceBuilder serviceBuilder = new ServiceBuilder()
                    .withNewMetadata()
                    .withName(serviceName)
                    .withLabels(labelMap)
                    .endMetadata();

            ServiceFluent<ServiceBuilder>.SpecNested<ServiceBuilder> serviceSpecBuilder = serviceBuilder.withNewSpec().withSelector(labelMap);

            List<ServicePort> servicePorts = getServicePorts();
            System.out.println("Generated ports: " + servicePorts);
            boolean hasPorts = servicePorts != null & !servicePorts.isEmpty();
            if (hasPorts) {
                serviceSpecBuilder.withPorts(servicePorts);
            } else {
                serviceSpecBuilder.withPortalIP("None");
            }
            serviceSpecBuilder.endSpec();

            if (headlessServices || hasPorts) {
                builder = builder.addToServiceItems(serviceBuilder.build());
            }
        }

        Template template = getTemplate();
        String iconUrl = getIconUrl();
        if (!template.getParameters().isEmpty() || Strings.isNotBlank(iconUrl)) {
            configureTemplateDescriptionAndIcon(template, iconUrl);
            builder = builder.addToTemplateItems(template);
        }

        KubernetesList kubernetesList = builder.build();

        Object result = Templates.combineTemplates(kubernetesList);
        if (result instanceof Template) {
            Template resultTemplate = (Template) result;
            configureTemplateDescriptionAndIcon(resultTemplate, iconUrl);
        }

        try {
            ObjectMapper mapper = new ObjectMapper()
                    .enable(SerializationFeature.INDENT_OUTPUT);
            String generated = mapper.writeValueAsString(result);
            Files.writeToFile(kubernetesJson, generated, Charset.defaultCharset());
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to generate Kubernetes JSON.", e);
        }
    }

    protected void configureTemplateDescriptionAndIcon(Template template, String iconUrl) {
        Map<String, String> annotations = KubernetesHelper.getOrCreateAnnotations(template);
        addDocumentationAnnotations(template, annotations);
        if (Strings.isNotBlank(iconUrl)) {
            annotations.put(getTemplateKey(template, AnnotationKeys.ICON_URL), iconUrl);
        }
    }

    protected String getTemplateKey(Template template, String key) {
        String name = getName(template);
        if (Strings.isNullOrBlank(name)) {
            name = getProject().getArtifactId();
        }
        return AnnotationKeys.PREFIX + name + "/" + key;
    }

    protected void addDocumentationAnnotations(Template template, Map<String, String> annotations) {
        // we want summary before description
        try {
            copySummaryText(templateTempDir);
            copyReadMe(templateTempDir);
        } catch (IOException e) {
            getLog().warn("Failed to copy documentation: " + e, e);
        }

        File summary = new File(templateTempDir, "Summary.md");
        if (summary.exists() && summary.isFile()) {
            try {
                String text = Files.toString(summary);
                annotations.put(getTemplateKey(template, AnnotationKeys.SUMMARY), text);
            } catch (IOException e) {
                getLog().warn("Failed to load " + summary + ". " + e, e);
            }
        }

        String description = null;
        File readme = new File(templateTempDir, "ReadMe.md");
        if (readme.exists() && readme.isFile()) {
            try {
                description = Files.toString(readme);
            } catch (IOException e) {
                getLog().warn("Failed to load " + readme + ". " + e, e);
            }
        }
        if (description == null) {
            description = getProject().getDescription();
        }
        if (Strings.isNotBlank(description)) {
            annotations.put(AnnotationKeys.DESCRIPTION, description);
        }
    }

    /**
     * Generate a URL for the icon.
     *
     * Lets use a data URL if possible if the icon is relatively small; otherwise lets try convert the icon name
     * to an external link (e.g. using github).
     */
    protected String getIconUrl() {
        String answer = iconUrl;
        if (Strings.isNullOrBlank(answer)) {
            try {
                if (templateTempDir != null) {
                    templateTempDir.mkdirs();
                    File iconFile = copyIconToFolder(templateTempDir);
                    if (iconFile == null) {
                        copyAppConfigFiles(templateTempDir, appConfigDir);

                        // lets find the icon file...
                        for (String ext : ICON_EXTENSIONS) {
                            File file = new File(templateTempDir, "icon" + ext);
                            if (file.exists() && file.isFile()) {
                                iconFile = file;
                                break;
                            }
                        }
                    }
                    if (iconFile != null) {
                        answer = convertIconFileToURL(iconFile);
                    }
                }
            } catch (Exception e) {
                getLog().warn("Failed to load icon file: " + e, e);
            }
        }

        if (Strings.isNullOrBlank(answer)) {
            // maybe its a common icon
            String commonRef = asCommonIconRef(iconRef);
            if (commonRef != null) {
                answer = URLUtils.pathJoin("https://cdn.rawgit.com/fabric8io/fabric8", iconBranch, "/fabric8-maven-plugin/src/main/resources/", commonRef);
            }
        }

        if (Strings.isNullOrBlank(answer)) {
            getLog().warn("No icon file found for this project!");
        } else {
            getLog().info("Icon URL: " + answer);
        }

        return answer;
    }

    protected String convertIconFileToURL(File iconFile) throws IOException {
        long length = iconFile.length();

        int sizeK = Math.round(length / 1024);

        byte[] bytes = Files.readBytes(iconFile);
        byte[] encoded = Base64Encoder.encode(bytes);

        int base64SizeK = Math.round(encoded.length / 1024);

        getLog().info("found icon file: " + iconFile +
                " which is " + sizeK + "K" +
                " base64 encoded " + base64SizeK + "K");

        if (base64SizeK < maximumDataUrlSizeK) {
            String mimeType = guessMediaType(iconFile);
            return "data:" + mimeType + ";charset=UTF-8;base64," + new String(encoded);
        } else {
            File iconSourceFile = new File(appConfigDir, iconFile.getName());
            if (iconSourceFile.exists()) {
                File rootProjectFolder = getRootProjectFolder();
                if (rootProjectFolder != null) {
                    String relativePath = Files.getRelativePath(rootProjectFolder, iconSourceFile);
                    String relativeParentPath = Files.getRelativePath(rootProjectFolder, getProject().getBasedir());
                    String urlPrefix = iconUrlPrefix;
                    if (Strings.isNullOrBlank(urlPrefix)) {
                        Scm scm = getProject().getScm();
                        if (scm != null) {
                            String url = scm.getUrl();
                            if (url != null) {
                                String[] prefixes = {"http://github.com/", "https://github.com/"};
                                for (String prefix : prefixes) {
                                    if (url.startsWith(prefix)) {
                                        url = URLUtils.pathJoin("https://cdn.rawgit.com/", url.substring(prefix.length()));
                                        break;
                                    }
                                }
                                if (url.endsWith(relativeParentPath)) {
                                    url = url.substring(0, url.length() - relativeParentPath.length());
                                }
                                urlPrefix = url;
                            }
                        }
                    }
                    if (Strings.isNullOrBlank(urlPrefix)) {
                        getLog().warn("No iconUrlPrefix defined or could be found via SCM in the pom.xml so cannot add an icon URL!");
                    } else {
                        String answer = URLUtils.pathJoin(urlPrefix, iconBranch, relativePath);
                        getLog().info("icon url is: " + answer);
                        return answer;
                    }
                }
            } else {
                String commonRef = asCommonIconRef(iconRef);
                if (commonRef != null) {
                    String answer = URLUtils.pathJoin("https://cdn.rawgit.com/fabric8io/fabric8", iconBranch, "/fabric8-maven-plugin/src/main/resources/", commonRef);
                    return answer;
                } else {
                    getLog().warn("Cannot find url for icon to use " + iconUrl);
                }
            }
        }
        return null;
    }

    protected String asCommonIconRef(String iconRef) {
        if (iconRef == null) {
            return null;
        }

        if (iconRef.startsWith("icons/")) {
            iconRef = iconRef.substring(6);
        }

        if (iconRef.contains("activemq")) {
            return "icons/activemq.svg";
        } else if (iconRef.contains("camel")) {
            return "icons/camel.svg";
        } else if (iconRef.contains("java")) {
            return "icons/java.svg";
        } else if (iconRef.contains("jetty")) {
            return "icons/jetty.svg";
        } else if (iconRef.contains("karaf")) {
            return "icons/karaf.svg";
        } else if (iconRef.contains("mule")) {
            return "icons/mule.svg";
        } else if (iconRef.contains("spring-boot")) {
            return "icons/spring-boot.svg";
        } else if (iconRef.contains("tomcat")) {
            return "icons/tomcat.svg";
        } else if (iconRef.contains("tomee")) {
            return "icons/tomee.svg";
        } else if (iconRef.contains("weld")) {
            return "icons/weld.svg";
        } else if (iconRef.contains("wildfly")) {
            return "icons/wildfly.svg";
        }

        return null;
    }

    protected Probe getLivenessProbe() {
        return getProbe("fabric8.livenessProbe");
    }

    protected Probe getReadinessProbe() {
        return getProbe("fabric8.readinessProbe");
    }

    protected Probe getProbe(String prefix) {
        Probe answer = new Probe();
        boolean added = false;
        Properties properties = getProject().getProperties();
        String httpGetPath = properties.getProperty(prefix + ".httpGet.path");
        String httpGetPort = properties.getProperty(prefix + ".httpGet.port");
        String httpGetHost = properties.getProperty(prefix + ".httpGet.host");
        if (Strings.isNotBlank(httpGetPath)) {
            added = true;
            HTTPGetAction httpGet = new HTTPGetAction();
            httpGet.setPath(httpGetPath);
            httpGet.setHost(httpGetHost);
            if (Strings.isNotBlank(httpGetPort)) {
                IntOrString httpGetPortIntOrString = KubernetesHelper.createIntOrString(httpGetPort);
                httpGet.setPort(httpGetPortIntOrString);
            }
            answer.setHttpGet(httpGet);
        }
        Long initialDelaySeconds = PropertiesHelper.getLong(properties, prefix + ".initialDelaySeconds");
        if (initialDelaySeconds != null) {
            answer.setInitialDelaySeconds(initialDelaySeconds);
        }
        Long timeoutSeconds = PropertiesHelper.getLong(properties, prefix + ".timeoutSeconds");
        if (timeoutSeconds != null) {
            answer.setTimeoutSeconds(timeoutSeconds);
        }
        return added ? answer : null;
    }

    public Boolean getContainerPrivileged() {
        return containerPrivileged;
    }

    public String getImagePullPolicy() {
        MavenProject project = getProject();
        String pullPolicy = imagePullPolicy;
        if (project != null) {
            String version = project.getVersion();
            if (Strings.isNullOrBlank(pullPolicy)) {
                if (version != null && version.endsWith("SNAPSHOT")) {
                    // TODO pullPolicy = "PullAlways";
                    pullPolicy = imagePullPolicySnapshot;
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

            answer.setName(name);
        }
        return answer;
    }

    public List<ServicePort> getServicePorts() throws MojoExecutionException {
        if (servicePorts == null) {
            servicePorts = new ArrayList<>();
        }
        if (servicePorts.isEmpty()) {
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
                        servicePort.setTargetPort(containerPortSpec);

                        String portProtocol = serviceProtocolProperties.get(name);
                        if (portProtocol != null) {
                            servicePort.setProtocol(portProtocol);
                        }

                        servicePorts.add(servicePort);
                    }
                }
            }

            if (serviceContainerPort != null && servicePort != null) {

                if (servicePorts.size() > 0) {
                    throw new MojoExecutionException("Multi-port services must use the " + FABRIC8_PORT_SERVICE_PREFIX + "<name> format");
                }

                ServicePort actualServicePort = new ServicePort();
                Integer containerPortNumber = parsePort(serviceContainerPort, FABRIC8_CONTAINER_PORT_SERVICE);
                IntOrString containerPort = new IntOrString();
                if (containerPortNumber != null) {
                    containerPort.setIntVal(containerPortNumber);
                } else {
                    containerPort.setStrVal(serviceContainerPort);
                }
                actualServicePort.setTargetPort(containerPort);
                actualServicePort.setPort(servicePort);
                if (serviceProtocol != null) {
                    actualServicePort.setProtocol(serviceProtocol);
                    servicePorts.add(actualServicePort);
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

        if (includeNamespaceEnvVar) {
            environmentVariables.add(
                    new EnvVarBuilder().withName(kubernetesNamespaceEnvVar).
                            withNewValueFrom().withNewFieldRef().
                            withFieldPath("metadata.namespace").endFieldRef().
                            endValueFrom().
                            build());
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

    public Template getTemplate() throws MojoExecutionException {
        List<io.fabric8.openshift.api.model.template.Parameter> parameters = new ArrayList<>();
        MavenProject project = getProject();
        Properties projectProperties = project.getProperties();
        Set<String> paramNames = new HashSet<>();
        if (templateParametersPropertiesFile != null && templateParametersPropertiesFile.isFile() && templateParametersPropertiesFile.exists()) {
            final String valuePostfix = ".value";
            final String descriptionPostfix = ".description";
            try {
                Properties properties = new Properties();
                properties.load(new FileInputStream(templateParametersPropertiesFile));
                // lets append the prefix
                Set<Object> keys = properties.keySet();
                Properties prefixedProperties = new Properties();
                for (Object key : keys) {
                    if (key != null) {
                        String name = key.toString();
                        String value = properties.getProperty(name);
                        prefixedProperties.put(PARAMETER_PREFIX + "." + name, value);
                    }
                }
                loadParametersFromProperties(prefixedProperties, parameters, paramNames);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to load templateParameters properties file " + templateParametersPropertiesFile + ". " + e, e);
            }
        }
        loadParametersFromProperties(projectProperties, parameters, paramNames);
        String templateName = projectProperties.containsKey(TEMPLATE_NAME) ?
                String.valueOf(projectProperties.getProperty(TEMPLATE_NAME)) :
                project.getArtifactId();
        return new TemplateBuilder().withNewMetadata().withName(templateName).endMetadata().withParameters(parameters).build();
    }

    protected void loadParametersFromProperties(Properties properties, List<io.fabric8.openshift.api.model.template.Parameter> parameters, Set<String> paramNames) {
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            Object key = entry.getKey();
            if (key instanceof String) {
                String s = (String) key;
                Matcher m = PARAM_PATTERN.matcher(s);
                if (m.matches()) {
                    String name = m.group(NAME);
                    if (paramNames.add(name)) {
                        String value = properties.getProperty(String.format(PARAMETER_PROPERTY, name, VALUE));
                        String from = properties.getProperty(String.format(PARAMETER_PROPERTY, name, FROM));
                        String description = properties.getProperty(String.format(PARAMETER_PROPERTY, name, DESCRIPTION));
                        String generate = properties.getProperty(String.format(PARAMETER_PROPERTY, name, GENERATE));
                        //If neither value nor from has been specified read the value inline.
                        if (Strings.isNullOrBlank(value) && Strings.isNullOrBlank(from)) {
                            value = properties.getProperty(String.format(PARAMETER_NAME_PREFIX, name));
                        }
                        getLog().info("Found Template parameter: " + name +
                                labelValueOrBlank("value", value) +
                                labelValueOrBlank("from", from) +
                                labelValueOrBlank("generate", generate) +
                                labelValueOrBlank("description", description));

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
        }
    }

    private String labelValueOrBlank(String label, String value) {
        if (Strings.isNotBlank(value)) {
            return " " + label + ": " + value;
        } else {
            return "";
        }
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
