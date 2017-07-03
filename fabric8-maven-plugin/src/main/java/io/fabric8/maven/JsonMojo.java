/**
 * Copyright 2005-2016 Red Hat, Inc.
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
import io.fabric8.kubernetes.api.Annotations;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.extensions.Templates;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.utils.Utils;
import io.fabric8.maven.support.Commandline;
import io.fabric8.maven.support.JsonSchema;
import io.fabric8.maven.support.JsonSchemaProperty;
import io.fabric8.maven.support.VolumeType;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.ImageStreamBuilder;
import io.fabric8.openshift.api.model.ParameterBuilder;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.utils.Base64Encoder;
import io.fabric8.utils.Files;
import io.fabric8.utils.Lists;
import io.fabric8.utils.Objects;
import io.fabric8.utils.PropertiesHelper;
import io.fabric8.utils.Strings;
import io.fabric8.utils.URLUtils;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.fabric8.kubernetes.api.KubernetesHelper.getName;
import static io.fabric8.kubernetes.api.KubernetesHelper.setName;
import static io.fabric8.utils.Files.guessMediaType;
import static io.fabric8.utils.PropertiesHelper.findPropertiesWithPrefix;
import static io.fabric8.utils.PropertiesHelper.getInteger;

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
    public static final String FABRIC8_NODE_PORT_SERVICE = "fabric8.service.nodePort";
    public static final String FABRIC8_PROTOCOL_SERVICE = "fabric8.service.protocol";
    public static final String FABRIC8_METRICS_PREFIX = "fabric8.metrics.";
    public static final String FABRIC8_METRICS_SCRAPE = FABRIC8_METRICS_PREFIX + "scrape";
    public static final String FABRIC8_METRICS_SCRAPE_ANNOTATION = FABRIC8_METRICS_SCRAPE + ".annotation";
    public static final String FABRIC8_METRICS_PORT = FABRIC8_METRICS_PREFIX + "port";
    public static final String FABRIC8_METRICS_PORT_ANNOTATION = FABRIC8_METRICS_PORT + ".annotation";
    public static final String FABRIC8_METRICS_SCHEME = FABRIC8_METRICS_PREFIX + "scheme";
    public static final String FABRIC8_METRICS_SCHEME_ANNOTATION = FABRIC8_METRICS_SCHEME + ".annotation";

    public static final String FABRIC8_ICON_URL_ANNOTATION = "fabric8.io/iconUrl";

    private static final String SERVICE_REGEX = "^fabric8\\.service\\.(?<name>[^. ]+)\\..+$";
    private static final Pattern SERVICE_PATTERN = Pattern.compile(SERVICE_REGEX);

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

    private static final String CPU = "cpu";
    private static final String MEMORY = "memory";

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
     * Should we generate any required SecurityContextConstraints DTOs in the generated json
     */
    @Parameter(property = "fabric8.generateSecurityContextConstraints", defaultValue = "false")
    private boolean generateSecurityContextConstraints;

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
     * Whether we should include the namespace in the containers' env vars
     */
    @Parameter(property = "fabric8.includePodEnvVar", defaultValue = "false")
    private boolean includePodEnvVar;

    /**
     * The name of the env var to add that will contain the pod name at container runtime
     */
    @Parameter(property = "fabric8.podEnvVar", defaultValue = "KUBERNETES_POD")
    private String kubernetesPodEnvVar;

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
     * The annotations for the PodSpec
     */
    @Parameter()
    private Map<String, String> podSpecAnnotations;

    /**
     * The annotations for the ReplicationController
     */
    @Parameter()
    private Map<String, String> rcAnnotations;


    /**
     * The annotations for the Template
     */
    @Parameter()
    private Map<String, String> templateAnnotations;

    /**
     * The annotations for the Service
     */
    @Parameter()
    private Map<String, String> serviceAnnotations;
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
     * The project label used in the generated Kubernetes JSON template
     */
    @Parameter(property = "fabric8.label.project", defaultValue = "${project.artifactId}")
    private String projectName;

    /**
     * The project label used in the generated Kubernetes JSON dependencies template
     */
    @Parameter(property = "fabric8.combineJson.project", defaultValue = "${project.artifactId}")
    private String combineProjectName;

    /**
     * The group label used in the generated Kubernetes JSON template
     */
    @Parameter(property = "fabric8.label.group", defaultValue = "${project.groupId}")
    private String groupName;

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
    private boolean headlessService;

    /**
     * The <a href="http://releases.k8s.io/HEAD/docs/user-guide/services.md#external-services">Type of the service</a>. Set to
     * <code>"LoadBalancer"</code>  if you wish an
     * <a href="https://github.com/GoogleCloudPlatform/kubernetes/blob/master/docs/user-guide/services.md#type-loadbalancer"></a>external load balancer</a> to be created
     */
    @Parameter(property = "fabric8.service.type")
    private String serviceType;

    /**
     * Annotation value to add for metrics scraping.
     */
    @Parameter(property = FABRIC8_METRICS_SCRAPE, defaultValue = "false")
    private boolean metricsScrape;

    /**
     * Annotation to add for metrics scraping.
     */
    @Parameter(property = FABRIC8_METRICS_SCRAPE_ANNOTATION, defaultValue = "prometheus.io/scrape")
    private String metricsScrapeAnnotation;

    /**
     * Annotation value to add for metrics port.
     */
    @Parameter(property = FABRIC8_METRICS_PORT)
    private Integer metricsPort;

    /**
     * Annotation value to add for metrics port.
     */
    @Parameter(property = FABRIC8_METRICS_PORT_ANNOTATION, defaultValue = "prometheus.io/port")
    private String metricsPortAnnotation;

    /**
     * Annotation value to add for metrics scheme.
     */
    @Parameter(property = FABRIC8_METRICS_SCHEME)
    private String metricsScheme;

    /**
     * Annotation to add for metrics scheme.
     */
    @Parameter(property = FABRIC8_METRICS_SCHEME_ANNOTATION, defaultValue = "prometheus.io/scheme")
    private String metricsSchemeAnnotation;

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
     * The service node port
     */
    @Parameter(property = FABRIC8_NODE_PORT_SERVICE)
    private Integer serviceNodePort;

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
     * Should we create the ServiceAccount resource as part of the build
     */
    @Parameter(property = "fabric8.serviceAccountCreate")
    private boolean createServiceAccount;

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
     * The properties file used to specify the annotations to be added to the generated PodSpec
     * <code>
     *     <pre>
     *         acme.com/cheese = SOMETHING
     *     </pre>
     * </code>
     */
    @Parameter(property = "fabric8.podSpecAnnotationsFile", defaultValue = "${basedir}/src/main/fabric8/podSpecAnnotations.properties")
    protected File podSpecAnnotationsFile;

    /**
     * The properties file used to specify the annotations to be added to the generated ReplicationController
     * <code>
     *     <pre>
     *         acme.com/cheese = SOMETHING
     *     </pre>
     * </code>
     */
    @Parameter(property = "fabric8.rcAnnotationsFile", defaultValue = "${basedir}/src/main/fabric8/rcAnnotations.properties")
    protected File rcAnnotationsFile;

    /**
     * The properties file used to specify the annotations to be added to the generated Template
     * <code>
     *     <pre>
     *         acme.com/cheese = SOMETHING
     *     </pre>
     * </code>
     */
    @Parameter(property = "fabric8.templateAnnotationsFile", defaultValue = "${basedir}/src/main/fabric8/templateAnnotations.properties")
    protected File templateAnnotationsFile;

    /**
     * The properties file used to specify the annotations to be added to the generated Service
     * <code>
     *     <pre>
     *         acme.com/cheese = SOMETHING
     *     </pre>
     * </code>
     */
    @Parameter(property = "fabric8.serviceAnnotationsFile", defaultValue = "${basedir}/src/main/fabric8/serviceAnnotations.properties")
    protected File serviceAnnotationsFile;

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

    /**
     * The default requests storage size for a PersistenceVolumeClaim if its created for a persistent volume via a claim
     */
    @Parameter(property = "fabric8.defaultPersistentVolumeClaimRequestsStorage", defaultValue = "20")
    private String defaultPersistentVolumeClaimRequestsStorage;

    /**
     * Should we remove the version label from the service selector?
     */
    @Parameter(property = "fabric8.removeVersionLabelFromServiceSelector", defaultValue = "true")
    private boolean removeVersionLabelFromServiceSelector;

    /**
     * CPU resource limits
     */
    @Parameter(property = "fabric8.resources.limits.cpu", defaultValue = "0")
    private String limitsCpu;

    /**
     * Memory resource limits
     */
    @Parameter(property = "fabric8.resources.limits.memory", defaultValue = "0")
    private String limitsMemory;

    /**
     * CPU resource requests
     */
    @Parameter(property = "fabric8.resources.requests.cpu", defaultValue = "0")
    private String requestsCpu;
    /**
     * Memory resource requests
     */
    @Parameter(property = "fabric8.resources.requests.cpu", defaultValue = "0")
    private String requestsMemory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File json = getKubernetesJson();
        getLog().info("Configured with file: " + json);
        if (json == null) {
            throw new MojoExecutionException("No kubernetes json file is specified!");
        }
        if (shouldGenerateForThisProject()) {
            if (!isIgnoreProject() || combineDependencies) {
            	if (generateJson) {
            		generateKubernetesJson(json);
            		if (combineDependencies) {
            			combineDependentJsonFiles(getKubernetesCombineJson() == null ? json : getKubernetesCombineJson());
            		}
            		if (kubernetesExtraJson != null && kubernetesExtraJson.exists()) {
            			combineJsonFiles(json, kubernetesExtraJson);
            		}
            	}
                if (json.exists() && json.isFile()) {
                    if (useDeploymentConfig) {
                        wrapInDeploymentConfigs(json);
                    }
                    addEnvironmentAnnotations(json);
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
                String templateName = getCombineProjectName();
                setName(template, templateName);
                configureTemplateDescriptionAndIcon(template, getIconUrl());

                addLabelIntoObjects(template.getObjects(), "package", templateName);

                if (pureKubernetes) {
                    combinedJson = applyTemplates(template);
                }
            }
            if (pureKubernetes) {
                combinedJson = filterPureKubernetes(combinedJson);
            }
            json.getParentFile().mkdirs();
            KubernetesHelper.saveJson(json, combinedJson);
            getLog().info("Saved as :" + json.getAbsolutePath());
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to save combined JSON files " + json + " and " + kubernetesExtraJson + " as " + json + ". " + e, e);
        }
    }

    protected void addLabelIntoObjects(List<HasMetadata> objects, String label, String value) {
        for (HasMetadata object : objects) {
            addLabelIfNotExist(object, label, value);
            if (object instanceof ReplicationController) {
                final ReplicationController entity = (ReplicationController) object;
                final ReplicationControllerSpec spec = entity.getSpec();
                if (spec != null) {
                    final PodTemplateSpec template = spec.getTemplate();
                    if (template != null) {
                        // TODO hack until this is fixed https://github.com/fabric8io/kubernetes-model/issues/112
                        HasMetadata hasMetadata = new HasMetadata() {
                            @Override
                            public ObjectMeta getMetadata() {
                                return template.getMetadata();
                            }

                            @Override
                            public void setMetadata(ObjectMeta objectMeta) {
                                template.setMetadata(objectMeta);
                            }

                            @Override
                            public String getKind() {
                                return "PodTemplateSpec";
                            }

                            @Override
                            public String getApiVersion() {
                                return entity.getApiVersion();
                            }

                            @Override
                            public void setApiVersion(String apiVersion) {
                                entity.setApiVersion(apiVersion);
                            }
                        };
                        addLabelIfNotExist(hasMetadata, label, value);
                    }
                }
            }
        }
    }

    protected boolean addLabelIfNotExist(HasMetadata object, String label, String value) {
        if (object != null) {
            Map<String, String> labels = KubernetesHelper.getOrCreateLabels(object);
            if (labels.get(label) == null) {
                labels.put(label, value);
                return true;
            }
        }
        return false;
    }

    protected Object applyTemplates(Template template) throws IOException {
        overrideTemplateParameters(template);
        return Templates.processTemplatesLocally(template, false);
    }

    protected Object filterPureKubernetes(Object dto) throws IOException {
        List<HasMetadata> items = KubernetesHelper.toItemList(dto);
        List<HasMetadata> filtered = new ArrayList<>();
        for (HasMetadata item : items) {
            if (KubernetesHelper.isPureKubernetes(item)) {
                filtered.add(item);
            }
        }
        KubernetesList answer = new KubernetesList();
        answer.setItems(filtered);
        return answer;
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

    protected void generateKubernetesJson(File kubernetesJson) throws MojoExecutionException {
        // TODO populate properties, project etc.
        MavenProject project = getProject();
        Map<String, String> labelMap = getLabels();
        String name = getProjectName();
        String group = getGroupName();
        if (!labelMap.containsKey("version")) {
            labelMap.put("version", project.getVersion());
        }
        if (!labelMap.containsKey("project") && Strings.isNotBlank(name)) {
            labelMap.put("project", name);
        }
        if (!labelMap.containsKey("group") && Strings.isNotBlank(group)) {
            labelMap.put("group", group);
        }
        if (!labelMap.containsKey("provider") && Strings.isNotBlank(provider)) {
            labelMap.put("provider", provider);
        }

        Map<String,String> podSpecAnnotations = getPodSpecAnnotations();
        Map<String,String> rcAnnotations = getRCAnnotations();
        KubernetesListBuilder builder = new KubernetesListBuilder();

        // lets add a ServiceAccount object if we add any new secret annotations
        boolean addedServiceAcount = addServiceAccountIfIUsingSecretAnnotations(builder, podSpecAnnotations);

        List<Volume> volumes = getVolumes();
        List<VolumeMount> volumeMounts = getVolumeMounts();
        Boolean containerPrivileged = getContainerPrivileged();

        if (addedServiceAcount) {
            addServiceConstraints(builder, volumes, containerPrivileged != null && containerPrivileged.booleanValue());
        }

        String iconUrl = getIconUrl();
        if (Strings.isNotBlank(iconUrl)) {
            rcAnnotations.put(FABRIC8_ICON_URL_ANNOTATION, iconUrl);
        }

        if (Utils.isNotNullOrEmpty(getDockerImage())) {
            builder.addNewReplicationControllerItem()
                    .withNewMetadata()
                    .withName(KubernetesHelper.validateKubernetesId(replicationControllerName, "fabric8.replicationController.name"))
                    .withLabels(labelMap)
                    .withAnnotations(rcAnnotations)
                    .endMetadata()
                    .withNewSpec()
                    .withReplicas(replicaCount)
                    .withSelector(labelMap)
                    .withNewTemplate()
                    .withNewMetadata()
                    .withLabels(labelMap)
                    .withAnnotations(podSpecAnnotations)
                    .endMetadata()
                    .withNewSpec()
                    .withServiceAccountName(serviceAccount)
                    .addNewContainer()
                    .withName(getKubernetesContainerName())
                    .withImage(getDockerImage())
                    .withImagePullPolicy(getImagePullPolicy())
                    .withNewResources()
                    .addToLimits(CPU,new Quantity(limitsCpu))
                    .addToLimits(MEMORY,new Quantity(limitsMemory))
                    .addToRequests(CPU,new Quantity(requestsCpu))
                    .addToRequests(MEMORY,new Quantity(requestsMemory))
                    .endResources()
                    .withEnv(getEnvironmentVariables())
                    .withNewSecurityContext()
                    .withPrivileged(containerPrivileged)
                    .endSecurityContext()
                    .withPorts(getContainerPorts())
                    .withVolumeMounts(volumeMounts)
                    .withLivenessProbe(getLivenessProbe())
                    .withReadinessProbe(getReadinessProbe())
                    .endContainer()
                    .withVolumes(volumes)
                    .endSpec()
                    .endTemplate()
                    .endSpec()
                    .endReplicationControllerItem();
        }

        addPersistentVolumeClaims(builder, volumes);

        addServices(builder, labelMap, iconUrl);

        Template template = getTemplate();
        if (!template.getParameters().isEmpty() || Strings.isNotBlank(iconUrl)) {
            configureTemplateDescriptionAndIcon(template, iconUrl);
        }

        KubernetesList kubernetesList;
        List<HasMetadata> items = null;
        try {
            items = builder.getItems();
        } catch (Exception e) {
            getLog().warn("Caught: " + e, e);
        }
        if (Lists.isNullOrEmpty(items)) {
            getLog().warn("No Kubernetes resources found! Skipping...");
            kubernetesList = new KubernetesList();
        } else {
            kubernetesList = builder.build();
        }

        Object result = Templates.combineTemplates(kubernetesList, template);
        if (result instanceof Template) {
            Template resultTemplate = (Template) result;
            defaultIconUrl(resultTemplate.getObjects());
            configureTemplateDescriptionAndIcon(resultTemplate, iconUrl);

            if (pureKubernetes) {
                try {
                    result = applyTemplates(resultTemplate);
                } catch (IOException e) {
                    throw new MojoExecutionException("Failed to process template locally " + e, e);
                }
            }
        }
        try {
            defaultIconUrl(KubernetesHelper.toItemList(result));
            if (pureKubernetes) {
                result = filterPureKubernetes(result);
            }

            ObjectMapper mapper = new ObjectMapper()
                    .enable(SerializationFeature.INDENT_OUTPUT);
            String generated = mapper.writeValueAsString(result);
            Files.writeToFile(kubernetesJson, generated, Charset.defaultCharset());
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to generate Kubernetes JSON.", e);
        }
    }

    private void defaultIconUrl(List<HasMetadata> hasMetadatas) {
        String iconUrl = getIconUrl();
        if (Strings.isNotBlank(iconUrl)) {
            for (HasMetadata entity : hasMetadatas) {
                if (entity instanceof Service || entity instanceof ServiceAccount) {
                    Map<String, String> annotations = KubernetesHelper.getOrCreateAnnotations(entity);
                    if (Strings.isNullOrBlank(annotations.get(FABRIC8_ICON_URL_ANNOTATION))) {
                        annotations.put(FABRIC8_ICON_URL_ANNOTATION, iconUrl);
                    }
                }
            }
        }
    }

    private void addServices(KubernetesListBuilder builder, Map<String, String> labelMap, String iconUrl) throws MojoExecutionException {
        MavenProject project = getProject();
        Properties properties = getProjectAndFabric8Properties(project);

        Set<String> serviceNames = new HashSet<>(Arrays.asList(""));
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            Object key = entry.getKey();
            if (key instanceof String) {
                String s = (String) key;
                Matcher m = SERVICE_PATTERN.matcher(s);
                if (m.matches()) {
                    String name = m.group(NAME);
                    serviceNames.add(name);
                }
            }
        }

        for (String serviceName : serviceNames) {
            Map<String, String> serviceAnnotations = getServiceAnnotations();
            serviceAnnotations.putAll(getMetricsAnnotations(serviceName));

            if (Strings.isNotBlank(this.iconUrl)) {
                serviceAnnotations.put(FABRIC8_ICON_URL_ANNOTATION, this.iconUrl);
            }

            Map<String, String> selector = new HashMap<>(labelMap);
            if (removeVersionLabelFromServiceSelector) {
                if (selector.remove("version") != null) {
                    getLog().info("Removed 'version' label from service selector for service `" + serviceName + "`");
                }
            }

            String tempServiceName = serviceName;
            if (Strings.isNullOrBlank(tempServiceName)) {
                tempServiceName = this.serviceName;
            }

            ServiceBuilder serviceBuilder = new ServiceBuilder()
                .withNewMetadata()
                .withName(tempServiceName)
                .withLabels(labelMap)
                .withAnnotations(serviceAnnotations)
                .endMetadata();

            ServiceFluent.SpecNested<ServiceBuilder> serviceSpecBuilder = serviceBuilder.withNewSpec().withSelector(selector);

            List<ServicePort> servicePorts = getServicePorts(serviceName);
            getLog().info("Generated ports: " + servicePorts);
            boolean hasPorts = servicePorts != null && !servicePorts.isEmpty();
            if (hasPorts) {
                serviceSpecBuilder.withPorts(servicePorts);
            }

            String headlessPrefix = buildServicePrefix(serviceName, "fabric8.service", "headless");
            Boolean tempHeadlessService = Boolean.valueOf(properties.getProperty(headlessPrefix));
            if (tempHeadlessService) {
                serviceSpecBuilder.withClusterIP("None");

                // If this is a headless service with no ports then see if metrics is enabled & add that as a port - hacky!
                if (!hasPorts && Boolean.parseBoolean(serviceAnnotations.get(metricsScrapeAnnotation))) {
                    try {
                        String port = serviceAnnotations.get(metricsPortAnnotation);
                        Integer metricsPort = Integer.parseInt(port);
                        if (metricsPort != null) {
                            ServicePort servicePort = new ServicePort();
                            servicePort.setPort(metricsPort);
                            servicePort.setTargetPort(new IntOrString(metricsPort));
                            serviceSpecBuilder.withPorts(Arrays.asList(servicePort));
                        }
                    } catch (NumberFormatException e) {
                        // Ignore this.
                    }
                }
            }

            String specificServiceType = getServiceType(serviceName);
            if (Strings.isNotBlank(specificServiceType)) {
                serviceSpecBuilder.withType(specificServiceType);
            }
            serviceSpecBuilder.endSpec();

            if (tempHeadlessService || hasPorts) {
                builder = builder.addToServiceItems(serviceBuilder.build());
            }
        }
    }

    private Map<? extends String, ? extends String> getMetricsAnnotations(String serviceName) {
        Map<String, String> metricsAnnotations = new HashMap<>();

        boolean tempMetricsScrape;
        Integer tempMetricsPort = null;
        String tempMetricsScheme;

        if (Strings.isNotBlank(serviceName)) {
            Properties properties = getProjectAndFabric8Properties(getProject());
            tempMetricsScrape = Boolean.parseBoolean(properties.getProperty(buildServicePrefix(serviceName, "fabric8.service", "metrics.scrape")));
            tempMetricsScheme = properties.getProperty(buildServicePrefix(serviceName, "fabric8.service", "metrics.scheme"));
            String port = properties.getProperty(buildServicePrefix(serviceName, "fabric8.service", "metrics.port"));
            if (port != null) {
                tempMetricsPort = Integer.parseInt(port);
            }
        } else {
            tempMetricsScrape = metricsScrape;
            tempMetricsScheme = metricsScheme;
            tempMetricsPort = metricsPort;
        }

        if (tempMetricsScrape) {
            metricsAnnotations.put(metricsScrapeAnnotation, Boolean.toString(tempMetricsScrape));
            if (tempMetricsPort != null) {
                metricsAnnotations.put(metricsPortAnnotation, tempMetricsPort.toString());
            }
            if (tempMetricsScheme != null) {
                metricsAnnotations.put(metricsSchemeAnnotation, tempMetricsScheme);
            }
        }
        return metricsAnnotations;
    }

    protected void addPersistentVolumeClaims(KubernetesListBuilder builder, List<Volume> volumes) {
        for (Volume volume : volumes) {
            PersistentVolumeClaimVolumeSource persistentVolumeClaim = volume.getPersistentVolumeClaim();
            if (persistentVolumeClaim != null) {
                String name = volume.getName();
                String claimName = persistentVolumeClaim.getClaimName();
                Boolean readOnly = persistentVolumeClaim.getReadOnly();

                if (Strings.isNotBlank(claimName)) {
                    String accessModes;
                    if (readOnly != null && readOnly.booleanValue()) {
                        accessModes = "ReadOnly";
                    } else {
                        accessModes = "ReadWriteMany";
                    }
                    Properties properties = getProjectAndFabric8Properties(getProject());
                    String requestStorageProperty = String.format(VolumeType.VOLUME_PROPERTY, name, VolumeType.VOLUME_PVC_REQUEST_STORAGE);
                    String amount = properties.getProperty(requestStorageProperty);
                    if (Strings.isNullOrBlank(amount)) {
                        amount = defaultPersistentVolumeClaimRequestsStorage;
                        getLog().info("No maven property defined for `" + requestStorageProperty + "` so defaulting the requestStorage to " + amount);
                    } else {
                        getLog().debug("Maven property `" + requestStorageProperty + "` = " + amount);
                    }

                    Map<String, Quantity> requests = new HashMap<>();
                    Quantity requestLimit = new QuantityBuilder().withAmount(amount).build();
                    requests.put("storage", requestLimit);

                    builder.addNewPersistentVolumeClaimItem().
                            withNewMetadata().withName(claimName).endMetadata().
                            withNewSpec().withAccessModes(accessModes).withVolumeName(claimName).withNewResources().withRequests(requests).endResources().endSpec().
                            endPersistentVolumeClaimItem();
                } else {
                    getLog().warn("No claimName for persistent volume " + volume);
                }
            }
        }
    }

    protected boolean addServiceAccountIfIUsingSecretAnnotations(KubernetesListBuilder builder, Map<String, String> annotations) {
        Set<String> secretAnnotations = new HashSet<>(Arrays.asList(
                Annotations.Secrets.SSH_KEY,
                Annotations.Secrets.SSH_PUBLIC_KEY,
                Annotations.Secrets.GPG_KEY
        ));
        Set<Map.Entry<String, String>> entries = annotations.entrySet();
        Set<String> secretNameSet = new TreeSet<>();
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (secretAnnotations.contains(key))  {
                List<String> secretNames = parseSecretNames(value);
                secretNameSet.addAll(secretNames);
            }
        }

        List<ObjectReference> secrets = new ArrayList<>();
        for (String secretName : secretNameSet) {
            ObjectReference secretRef = new ObjectReferenceBuilder().withName(secretName).build();
            secrets.add(secretRef);
        }

        if (!secrets.isEmpty() || createServiceAccount) {
            if (Strings.isNullOrBlank(serviceAccount)) {
                serviceAccount = getProject().getArtifactId();
            }

            builder.addNewServiceAccountItem()
                    .withNewMetadata().withName(serviceAccount).endMetadata()
                    .withSecrets(secrets)
                    .endServiceAccountItem();
            return true;

        }
        return false;
    }

    protected void addServiceConstraints(KubernetesListBuilder builder, List<Volume> volumes, boolean containerPrivileged) {
        if (generateSecurityContextConstraints) {
            boolean hostVolume = hasHostVolume(volumes);
            if (hostVolume || containerPrivileged) {
                RunAsUserStrategyOptions runAsUser;
                builder.addNewSecurityContextConstraintsItem().
                        withNewMetadata().withName(serviceAccount).endMetadata().
                        withAllowHostDirVolumePlugin(hostVolume).withAllowPrivilegedContainer(containerPrivileged).
                        withNewRunAsUser().withType("RunAsAny").endRunAsUser().
                        withNewSeLinuxContext().withType("RunAsAny").endSeLinuxContext().
                        withUsers("system:serviceaccount:" + getNamespace() + ":" + serviceAccount).
                        endSecurityContextConstraintsItem();
            }
        }
    }

    protected boolean hasHostVolume(List<Volume> volumes) {
        if (volumes != null) {
            for (Volume volume : volumes) {
                HostPathVolumeSource hostPath = volume.getHostPath();
                if (hostPath != null && Strings.isNotBlank(hostPath.getPath())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<String> parseSecretNames(String value) {
        // lets split by [...] first removing those out
        List<String> answer = new ArrayList<>();
        String[] split = value.split("\\[|\\]");
        if (split != null && split.length > 0) {
            int i = 0;
            while (i < split.length) {
                String name = split[i];
                if (name.startsWith(",")) {
                    name = name.substring(1);
                }
                splitCommas(name, answer);
                // ignore next which is the conetnts of the array
                i += 2;
            }
        } else {
            splitCommas(value, answer);
        }
        return answer;
    }

    private static void splitCommas(String value, List<String> answer) {
        String[] split = value.split(",");
        if (split != null && split.length > 0) {
            answer.addAll(Arrays.asList(split));
        } else {
            answer.add(value);
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
            // maybe its a common icon that is embedded in fabric8-console
            String embeddedIcon = embeddedIconsInConsole(iconRef, "img/icons/");
            if (embeddedIcon != null) {
                return embeddedIcon;
            }
        }

        if (Strings.isNullOrBlank(answer)) {
            getLog().debug("No icon file found for this project");
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
                        return answer;
                    }
                }
            } else {
                String embeddedIcon = embeddedIconsInConsole(iconRef, "img/icons/");
                if (embeddedIcon != null) {
                    return embeddedIcon;
                } else {
                    getLog().warn("Cannot find url for icon to use " + iconUrl);
                }
            }
        }
        return null;
    }

    /**
     * To use embedded icons provided by the fabric8-console
     *
     * @param iconRef  name of icon file
     * @param prefix   prefix location for the icons in the fabric8-console
     * @return the embedded icon ref, or <tt>null</tt> if no embedded icon found to be used
     */
    protected String embeddedIconsInConsole(String iconRef, String prefix) {
        if (iconRef == null) {
            return null;
        }

        if (iconRef.startsWith("icons/")) {
            iconRef = iconRef.substring(6);
        }

        // special for fabric8 as its in a different dir
        if (iconRef.contains("fabric8")) {
            return "img/fabric8_icon.svg";
        }

        if (iconRef.contains("activemq")) {
            return prefix + "activemq.svg";
        } else if (iconRef.contains("apiman")) {
            return prefix + "apiman.png";
        } else if (iconRef.contains("api-registry")) {
            return prefix + "api-registry.svg";
        } else if (iconRef.contains("brackets")) {
            return prefix + "brackets.svg";
        } else if (iconRef.contains("camel")) {
            return prefix + "camel.svg";
        } else if (iconRef.contains("chaos-monkey")) {
            return prefix + "chaos-monkey.png";
        } else if (iconRef.contains("docker-registry")) {
            return prefix + "docker-registry.png";
        } else if (iconRef.contains("elasticsearch")) {
            return prefix + "elasticsearch.png";
        } else if (iconRef.contains("fluentd")) {
            return prefix + "fluentd.png";
        } else if (iconRef.contains("forge")) {
            return prefix + "forge.svg";
        } else if (iconRef.contains("gerrit")) {
            return prefix + "gerrit.png";
        } else if (iconRef.contains("gitlab")) {
            return prefix + "gitlab.svg";
        } else if (iconRef.contains("gogs")) {
            return prefix + "gogs.png";
        } else if (iconRef.contains("grafana")) {
            return prefix + "grafana.png";
        } else if (iconRef.contains("hubot-irc")) {
            return prefix + "hubot-irc.png";
        } else if (iconRef.contains("hubot-letschat")) {
            return prefix + "hubot-letschat.png";
        } else if (iconRef.contains("hubot-notifier")) {
            return prefix + "hubot-notifier.png";
        } else if (iconRef.contains("hubot-slack")) {
            return prefix + "hubot-slack.png";
        } else if (iconRef.contains("image-linker")) {
            return prefix + "image-linker.svg";
        } else if (iconRef.contains("javascript")) {
            return prefix + "javascript.png";
        } else if (iconRef.contains("java")) {
            return prefix + "java.svg";
        } else if (iconRef.contains("jenkins")) {
            return prefix + "jenkins.svg";
        } else if (iconRef.contains("jetty")) {
            return prefix + "jetty.svg";
        } else if (iconRef.contains("karaf")) {
            return prefix + "karaf.svg";
        } else if (iconRef.contains("keycloak")) {
            return prefix + "keycloak.svg";
        } else if (iconRef.contains("kibana")) {
            return prefix + "kibana.svg";
        } else if (iconRef.contains("kiwiirc")) {
            return prefix + "kiwiirc.png";
        } else if (iconRef.contains("letschat")) {
            return prefix + "letschat.png";
        } else if (iconRef.contains("mule")) {
            return prefix + "mule.svg";
        } else if (iconRef.contains("nexus")) {
            return prefix + "nexus.png";
        } else if (iconRef.contains("node")) {
            return prefix + "node.svg";
        } else if (iconRef.contains("orion")) {
            return prefix + "orion.png";
        } else if (iconRef.contains("prometheus")) {
            return prefix + "prometheus.png";
        } else if (iconRef.contains("django") || iconRef.contains("python")) {
            return prefix + "python.png";
        } else if (iconRef.contains("spring-boot")) {
            return prefix + "spring-boot.svg";
        } else if (iconRef.contains("taiga")) {
            return prefix + "taiga.png";
        } else if (iconRef.contains("tomcat")) {
            return prefix + "tomcat.svg";
        } else if (iconRef.contains("tomee")) {
            return prefix + "tomee.svg";
        } else if (iconRef.contains("vertx")) {
            return prefix + "vertx.svg";
        } else if (iconRef.contains("wildfly")) {
            return prefix + "wildfly.svg";
        } else if (iconRef.contains("weld")) {
            return prefix + "weld.svg";
        } else if (iconRef.contains("zipkin")) {
            return prefix + "zipkin.png";
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
        Probe probe = new Probe();
        Properties properties = getProjectAndFabric8Properties(getProject());
        Integer initialDelaySeconds = getInteger(properties, prefix + ".initialDelaySeconds");
        if (initialDelaySeconds != null) {
            probe.setInitialDelaySeconds(initialDelaySeconds);
        }
        Integer timeoutSeconds = getInteger(properties, prefix + ".timeoutSeconds");
        if (timeoutSeconds != null) {
            probe.setTimeoutSeconds(timeoutSeconds);
        }
        HTTPGetAction httpGetAction = getHTTPGetAction(prefix, properties);
        if (httpGetAction != null) {
            probe.setHttpGet(httpGetAction);
            return probe;
        }
        ExecAction execAction = getExecAction(prefix, properties);
        if (execAction != null) {
            probe.setExec(execAction);
            return probe;
        }
        TCPSocketAction tcpSocketAction = getTCPSocketAction(prefix, properties);
        if (tcpSocketAction != null) {
            probe.setTcpSocket(tcpSocketAction);
            return probe;
        }

        return null;
    }

    private HTTPGetAction getHTTPGetAction(String prefix, Properties properties) {
        HTTPGetAction action = null;
        String httpGetPath = properties.getProperty(prefix + ".httpGet.path");
        String httpGetPort = properties.getProperty(prefix + ".httpGet.port");
        String httpGetHost = properties.getProperty(prefix + ".httpGet.host");
        String httpGetScheme = properties.getProperty(prefix + ".httpGet.scheme");
        if (Strings.isNotBlank(httpGetPath)) {
            action = new HTTPGetAction();
            action.setPath(httpGetPath);
            action.setHost(httpGetHost);
            if (Strings.isNotBlank(httpGetScheme)) {
                action.setScheme(httpGetScheme.toUpperCase());
            }
            if (Strings.isNotBlank(httpGetPort)) {
                IntOrString httpGetPortIntOrString = KubernetesHelper.createIntOrString(httpGetPort);
                action.setPort(httpGetPortIntOrString);
            }
        }
        return action;
    }

    private TCPSocketAction getTCPSocketAction(String prefix, Properties properties) {
        TCPSocketAction action = null;
        String port = properties.getProperty(prefix + ".port");
        if (Strings.isNotBlank(port)) {
            IntOrString portObj = new IntOrString();
            try {
                Integer portInt = Integer.parseInt(port);
                portObj.setIntVal(portInt);
            } catch (NumberFormatException e) {
                portObj.setStrVal(port);
            }
            action = new TCPSocketAction(portObj);
        }
        return action;
    }

    private ExecAction getExecAction(String prefix, Properties properties) {
        ExecAction action = null;
        String execCmd = properties.getProperty(prefix + ".exec");
        if (Strings.isNotBlank(execCmd)) {
            List<String> splitCommandLine = Commandline.translateCommandline(execCmd);
            if (!splitCommandLine.isEmpty()) {
                action = new ExecAction(splitCommandLine);
            }
        }
        return action;
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
            String imageName = getDockerImage();
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

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getCombineProjectName() {
        return combineProjectName;
    }

    public void setCombineProjectName(String combineProjectName) {
        this.combineProjectName = combineProjectName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
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
            Properties properties1 = getProjectAndFabric8Properties(getProject());
            Map<String, String> hostPorts = findPropertiesWithPrefix(properties1, FABRIC8_PORT_HOST_PREFIX);
            Properties properties = getProjectAndFabric8Properties(getProject());
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

    private String buildServicePrefix(String name, String prefix, String suffix) {
        String servicePrefix = prefix;
        if (Strings.isNotBlank(name)) {
            servicePrefix += "." + name;
        }
        return servicePrefix + "." + suffix;
    }

    private List<ServicePort> getServicePorts(String serviceName) throws MojoExecutionException {
        String servicePortPrefix = buildServicePrefix(serviceName, "fabric8.service", "port");
        String serviceContainerPortPrefix = buildServicePrefix(serviceName, "fabric8.service", "containerPort");
        String serviceNodePortPrefix = buildServicePrefix(serviceName, "fabric8.service", "nodePort");
        String serviceProtocolPrefix = buildServicePrefix(serviceName, "fabric8.service", "protocol");

        Properties properties1 = getProjectAndFabric8Properties(getProject());

        List<ServicePort> servicePorts = new ArrayList<>();
        Map<String, String> servicePortProperties = findPropertiesWithPrefix(properties1, servicePortPrefix + ".");
        Map<String, String> serviceContainerPortProperties = findPropertiesWithPrefix(properties1, serviceContainerPortPrefix + ".");
        Map<String, String> serviceNodePortProperties = findPropertiesWithPrefix(properties1, serviceNodePortPrefix + ".");
        Map<String, String> serviceProtocolProperties = findPropertiesWithPrefix(properties1, serviceProtocolPrefix + ".");

        for (Map.Entry<String, String> entry : servicePortProperties.entrySet()) {
            String name = entry.getKey();
            String servicePortText = entry.getValue();
            Integer servicePortNumber = parsePort(servicePortText, servicePortPrefix + name);
            if (servicePortNumber != null) {
                String containerPort = serviceContainerPortProperties.get(name);
                if (Strings.isNullOrBlank(containerPort)) {
                    getLog().warn("Missing container port for service - need to specify " + serviceContainerPortPrefix + name + " property");
                } else {
                    ServicePort servicePort = new ServicePort();
                    servicePort.setName(name);
                    servicePort.setPort(servicePortNumber);

                    IntOrString containerPortSpec = getPortSpec(containerPort, serviceContainerPortPrefix, name);
                    servicePort.setTargetPort(containerPortSpec);

                    String nodePort = serviceNodePortProperties.get(name);
                    if (nodePort != null) {
                        IntOrString nodePortSpec = getPortSpec(nodePort, serviceNodePortPrefix, name);
                        Integer nodePortInt = nodePortSpec.getIntVal();
                        if (nodePortInt != null) {
                            servicePort.setNodePort(nodePortInt);
                        }
                    }

                    String portProtocol = serviceProtocolProperties.get(name);
                    if (portProtocol != null) {
                        servicePort.setProtocol(portProtocol);
                    }

                    servicePorts.add(servicePort);
                }
            }
        }

        Integer tempPort;
        String tempContainerPort;
        Integer tempNodePort;
        String tempServiceProtocol;

        if (Strings.isNotBlank(serviceName)) {
            tempPort = parsePort(properties1.getProperty(servicePortPrefix), servicePortPrefix);
            tempContainerPort = properties1.getProperty(serviceContainerPortPrefix);
            tempNodePort = parsePort(properties1.getProperty(serviceNodePortPrefix), serviceNodePortPrefix);
            tempServiceProtocol = properties1.getProperty(serviceProtocolPrefix, "TCP");
        } else {
            tempPort = servicePort;
            tempContainerPort = serviceContainerPort;
            tempNodePort = serviceNodePort;
            tempServiceProtocol = serviceProtocol;
        }

        if (tempContainerPort != null || tempPort != null) {

            if (servicePorts.size() > 0) {
                throw new MojoExecutionException("Multi-port services must use the " + servicePortPrefix + "<name> format");
            }

            ServicePort actualServicePort = new ServicePort();

            IntOrString containerPort = getPortSpec(tempContainerPort, serviceContainerPortPrefix, null);

            actualServicePort.setTargetPort(containerPort);
            actualServicePort.setPort(tempPort);
            if (tempNodePort != null) {
                actualServicePort.setNodePort(tempNodePort);
            }
            if (tempServiceProtocol != null) {
                actualServicePort.setProtocol(tempServiceProtocol);
                servicePorts.add(actualServicePort);
            }
        }

        return servicePorts;
    }

    private IntOrString getPortSpec(String portText, String portServicePrefix, String name) {
        IntOrString portSpec = new IntOrString();
        String portServiceName = portServicePrefix;
        if (name != null) {
            portServiceName = portServicePrefix + name;
        }
        Integer portNumber = parsePort(portText, portServiceName);
        if (portNumber != null) {
            portSpec.setIntVal(portNumber);
        } else {
            portSpec.setStrVal(portText);
        }

        return portSpec;
    }

    private String getServiceType(String serviceName) throws MojoExecutionException {
        String serviceSpecificTypeName = buildServicePrefix(serviceName, "fabric8.service", "type");

        Properties properties = getProjectAndFabric8Properties(getProject());
        String serviceSpecificType = properties.getProperty(serviceSpecificTypeName);

        if (Strings.isNullOrBlank(serviceSpecificType)) {
            serviceSpecificType = this.serviceType;
        }

        return serviceSpecificType;
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
            labels = findPropertiesWithPrefix(getProjectAndFabric8Properties(getProject()), "fabric8.label.", Strings.toLowerCaseFunction());
        }
        return labels;
    }

    public Map<String, String> getPodSpecAnnotations() throws MojoExecutionException {
        if (podSpecAnnotations == null) {
            podSpecAnnotations = loadAnnotations(podSpecAnnotationsFile, "fabric8.annotations.podSpec.", "PodSpec");
        }
        return podSpecAnnotations;
    }

    public Map<String, String> getRCAnnotations() throws MojoExecutionException {
        if (rcAnnotations == null) {
            rcAnnotations = loadAnnotations(rcAnnotationsFile, "fabric8.annotations.rc.", "RC");
        }
        return rcAnnotations;
    }

    public Map<String, String> getTemplateAnnotations() throws MojoExecutionException {
        if (templateAnnotations == null) {
            templateAnnotations = loadAnnotations(templateAnnotationsFile, "fabric8.annotations.template.", "Template");
        }
        return templateAnnotations;
    }

    public Map<String, String> getServiceAnnotations() throws MojoExecutionException {
        Map<String, String> serviceAnnotations = loadAnnotations(serviceAnnotationsFile, "fabric8.annotations.service.", "Service");
        return serviceAnnotations;
    }

    protected Map<String, String> loadAnnotations(File annotationsFile, String propertiesPrefix, String annotationsName) throws MojoExecutionException {
        Map<String, String> answer = findPropertiesWithPrefix(getProjectAndFabric8Properties(getProject()), propertiesPrefix, Strings.toLowerCaseFunction());
        if (annotationsFile != null && annotationsFile.exists() && annotationsFile.isFile()) {
            try {
                Properties properties = new Properties();
                properties.load(new FileInputStream(annotationsFile));
                Map<String, String> fileAnnotations = PropertiesHelper.toMap(properties);
                answer.putAll(fileAnnotations);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to load podSpecAnnotationsFile properties file " + podSpecAnnotationsFile + ". " + e, e);
            }
        }
        //kubernetes annotation keys can be prefixed by namespace like namespace/name, but
        //xml tags can't contain slashes. So by convention we will change the last "." into a "/".
        //for example 'apiman.io.servicepath' will be turned into 'apiman.io/servicepath'
        Map<String, String> newAnswer = new HashMap<String,String>();
        for (String key: answer.keySet()) {
        	int lastDot = key.lastIndexOf(".");
        	if (! key.contains("/") && lastDot > 0) {
	        		String namespace = key.substring(0, lastDot);
	        		String name = key.substring(lastDot + 1);
	        		newAnswer.put(namespace + "/" + name, answer.get(key));
        	} else {
        		newAnswer.put(key, answer.get(key));
        	}
        }
        return newAnswer;
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
        if (includePodEnvVar) {
            environmentVariables.add(
                    new EnvVarBuilder().withName(kubernetesPodEnvVar).
                            withNewValueFrom().withNewFieldRef().
                            withFieldPath("metadata.name").endFieldRef().
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
        for (Map.Entry<Object, Object> entry : getProjectAndFabric8Properties(project).entrySet()) {
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
        Properties properties = getProjectAndFabric8Properties(project);

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
        List<io.fabric8.openshift.api.model.Parameter> parameters = new ArrayList<>();
        MavenProject project = getProject();
        Properties projectProperties = getProjectAndFabric8Properties(getProject());
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
        Template template = new Template();
        template.setMetadata(new ObjectMetaBuilder().withName(templateName).withAnnotations(getTemplateAnnotations()).build());
        template.setParameters(parameters);
        return template;
    }

    protected void loadParametersFromProperties(Properties properties, List<io.fabric8.openshift.api.model.Parameter> parameters, Set<String> paramNames) {
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
