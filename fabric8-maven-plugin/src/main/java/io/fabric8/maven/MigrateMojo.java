/**
 * Copyright 2005-2016 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.maven;

import com.fasterxml.jackson.databind.node.TextNode;
import io.fabric8.kubernetes.api.Annotations;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentSpec;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.internal.HasMetadataComparator;
import io.fabric8.maven.support.JsonSchema;
import io.fabric8.maven.support.JsonSchemaProperty;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.utils.DomHelper;
import io.fabric8.utils.Files;
import io.fabric8.utils.IOHelpers;
import io.fabric8.utils.Strings;
import io.fabric8.utils.XmlUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static io.fabric8.utils.DomHelper.firstChild;

/**
 * Migrates the generated Kubernetes manifest to be used by the 3.x or later of the fabric8-maven-plugin
 */
@Mojo(name = "migrate", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.INSTALL)
public class MigrateMojo extends AbstractFabric8Mojo {

    /**
     * The output folder for the migrations
     */
    @Parameter(property = "fabric8.migrate.outputDir", defaultValue = "${basedir}/src/main/fabric8")
    protected File outputDir;

    private Map<String, String> kindAliases = new HashMap();

    /**
     * Should we also update the pom.xml to remove the fabric8-maven-plugin 2.x properties?
     */
    @Parameter(property = "fabric8.migrate.outputDir", defaultValue = "true")
    private boolean updatePom;

    /**
     * Should we ensure that the fabric8-maven-plugin has executions? If using a multi module project
     * you may wish to define these executions in the parent pom
     */
    @Parameter(property = "fabric8.migrate.updateExecutions", defaultValue = "true")
    private boolean updateExecutions;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        init();
        File json = getKubernetesJson();
        if (Files.isFile(json)) {
            try {
                Object dto = KubernetesHelper.loadJson(json);
                if (dto == null) {
                    throw new MojoFailureException("Cannot load kubernetes json: " + json);
                }

                Set<HasMetadata> entities = new TreeSet<>(new HasMetadataComparator());

                ConfigMap parameterConfigMap = null;
                if (dto instanceof Template) {
                    Template template = (Template) dto;

                    List<HasMetadata> objects = template.getObjects();
                    if (objects != null) {
                        entities.addAll(objects);
                    }

                    List<io.fabric8.openshift.api.model.Parameter> parameters = template.getParameters();
                    if (parameters != null && parameters.size() > 0) {
                        JsonSchema schema = new JsonSchema();
                        Map<String, String> configMapData = new TreeMap<>();
                        for (io.fabric8.openshift.api.model.Parameter parameter : parameters) {
                            String name = parameter.getName();
                            String value = parameter.getValue();
                            if (value == null) {
                                value = "";
                            }
                            String key = convertToConfigMapKey(name);
                            JsonSchemaProperty property = schema.getOrCreateProperty(key);
                            String generate = parameter.getGenerate();
                            if (Strings.isNotBlank(generate)) {
                                property.setGenerate(generate);
                            }
                            Boolean required = parameter.getRequired();
                            if (required != null && required.booleanValue()) {
                                schema.addRequired(name);
                            }
                            String description = parameter.getDescription();
                            if (Strings.isNotBlank(description)) {
                                property.setDescription(description);
                            }
                            if (Strings.isNotBlank(value)) {
                                property.setDefaultValue(value);
                            }
                            configMapData.put(key, value);
                        }
                        String jsonSchemaJson = KubernetesHelper.toPrettyJson(schema);
                        getLog().info("Generated ConfigMap JSON Schema: " + jsonSchemaJson);
                        parameterConfigMap = new ConfigMapBuilder().withNewMetadataLike(template.getMetadata()).
                                addToAnnotations(Annotations.Config.JSON_SCHEMA, jsonSchemaJson).endMetadata().build();
                        parameterConfigMap.setData(configMapData);


                        migrateEntity(parameterConfigMap, parameterConfigMap);
                        entities.add(parameterConfigMap);
                    }
                } else {
                    entities.addAll(KubernetesHelper.toItemList(dto));
                }

                outputDir.mkdirs();
                for (HasMetadata entity : entities) {
                    entity = migrateEntity(entity, parameterConfigMap);
                    String name = KubernetesHelper.getName(entity);
                    String kind = shortenKind(KubernetesHelper.getKind(entity).toLowerCase());

                    File outFile = new File(outputDir, name + "-" + kind + ".yml");
                    if (entity instanceof ConfigMap || entity instanceof Secret) {
                        KubernetesHelper.saveYaml(entity, outFile);
                    } else {
                        KubernetesHelper.saveYamlNotEmpty(entity, outFile);
                    }

                    getLog().info("Generated migration file: " + outFile);
                }
                tryAddFilesToGit(".");

                File basedir = getProject().getBasedir();
                if (updatePom) {
                    updatePomFile(new File(basedir, "pom.xml"));
                }
                String[] filesToDelete = {"uses.fmp2", "src/main/fabric8/templateParameters.properties", "src/main/fabric8/env.properties", "src/main/fabric8/kubernetes.json"};
                for (String fileName : filesToDelete) {
                    File file = new File(basedir, fileName);
                    if (file.exists()) {
                        file.delete();
                    }
                }
                deleteModelProcessorJavaFiles(new File(basedir, "src/main/java"));
            } catch (Exception e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
    }

    private void deleteModelProcessorJavaFiles(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String name = file.getName().toLowerCase();
                    if (name.endsWith(".java")) {
                        try {
                            String text = IOHelpers.readFully(file);
                            if (text.contains("@KubernetesModelProcessor")) {
                                file.delete();
                            }
                        } catch (IOException e) {
                            getLog().warn("Failed to load file " + file + ". " + e, e);
                        }
                    }
                } else if (file.isDirectory()) {
                    deleteModelProcessorJavaFiles(file);
                }
            }
        }
    }

    private void tryAddFilesToGit(String filePattern) {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try {
            Repository repository = builder
                    .readEnvironment() // scan environment GIT_* variables
                    .findGitDir() // scan up the file system tree
                    .build();

            Git git = new Git(repository);
            git.add().addFilepattern(filePattern).call();
        } catch (Exception e) {
            getLog().warn("Failed to add generated files to the git repository: " + e, e);
        }
    }

    protected void updatePomFile(File pom) throws MojoExecutionException {
        boolean updated = false;
        Document doc;
        try {
            doc = XmlUtils.parseDoc(pom);
        } catch (Exception e) {
            getLog().error("Failed to parse pom " + pom + ". " + e, e);
            throw new MojoExecutionException(e.getMessage(), e);
        }

        Map<String, String> propertyMap = new HashMap<>();
        Element properties = firstChild(doc.getDocumentElement(), "properties");
        if (properties != null) {
            NodeList childNodes = properties.getChildNodes();
            if (childNodes != null) {
                boolean lastRemoved = false;
                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node item = childNodes.item(i);
                    if (item instanceof Element) {
                        Element property = (Element) item;
                        String tagName = property.getTagName();
                        String value = property.getTextContent();
                        propertyMap.put(tagName, value);
                        if (removePropertyName(tagName, value)) {
                            properties.removeChild(property);
                            i--;
                            lastRemoved = true;
                            updated = true;
                        }
                    } else if (item instanceof Text) {
                        Text text = (Text) item;
                        if (lastRemoved) {
                            properties.removeChild(text);
                            i--;
                            lastRemoved = false;
                        }
                    }
                }
            }
        }
        if (removeProfiles(doc, "docker-build", "docker-push", "jube")) {
            updated = true;
        }
        if (removePlugin(doc, "io.fabric8.jube", "jube-maven-plugin")) {
            updated = true;
        }
        if (migrateDockerMavenPluginConfiguration(doc, propertyMap)) {
            updated = true;
        }
        if (updated) {
            getLog().info("Updating the pom " + pom);
            try {
                DomHelper.save(doc, pom);
            } catch (Exception e) {
                getLog().error("Failed to update pom " + pom + ". " + e, e);
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
    }


    private boolean removeProfiles(Document doc, String... profileIds) {
        Set<String> profileIdSet = new HashSet<>(Arrays.asList(profileIds));
        boolean updated = false;
        Element profiles = firstChild(doc.getDocumentElement(), "profiles");
        if (profiles != null) {
            NodeList childNodes = profiles.getChildNodes();
            if (childNodes != null) {
                boolean lastRemoved = false;
                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node item = childNodes.item(i);
                    if (item instanceof Element) {
                        Element profile = (Element) item;
                        Element idElement = firstChild(profile, "id");
                        if (idElement != null) {
                            String id = idElement.getTextContent();
                            if (id != null && profileIdSet.contains(id)) {
                                profiles.removeChild(profile);
                                i--;
                                lastRemoved = true;
                                updated = true;
                            }
                        }
                    } else if (item instanceof Text) {
                        Text text = (Text) item;
                        if (lastRemoved) {
                            profiles.removeChild(text);
                            i--;
                            lastRemoved = false;
                        }
                    }
                }
            }
        }
        return updated;
    }

    private boolean migrateDockerMavenPluginConfiguration(Document doc, Map<String, String> propertyMap) {
        boolean updated = false;
        Element configuration = null;
        Element dmpPlugin = findPlugin(doc, "io.fabric8", "docker-maven-plugin");
        if (dmpPlugin != null) {
            configuration = firstChild(dmpPlugin, "configuration");
            if (configuration != null) {
                DomHelper.detach(configuration);
            }
            Node nextSibling = dmpPlugin.getNextSibling();
            if (nextSibling instanceof TextNode) {
                DomHelper.detach(nextSibling);
            }
            DomHelper.detach(dmpPlugin);
            updated = true;
        }
        Element fmpPlugin = findPlugin(doc, "io.fabric8", "fabric8-maven-plugin");
        if (fmpPlugin == null) {
            if (configuration != null) {
                fmpPlugin = findOrAddPlugin(doc, "io.fabric8", "fabric8-maven-plugin", "${fabric8.maven.plugin.version}", configuration);
                updated = true;
            }
        } else {
            if (configuration != null) {
                Element oldConfig = firstChild(fmpPlugin, "configuration");
                DomHelper.detach(oldConfig);
                fmpPlugin.appendChild(configuration);
                if (oldConfig == null) {
                    fmpPlugin.appendChild(doc.createTextNode("\n      "));
                }
            }
        }
        if (updateExecutions && fmpPlugin != null) {
            Element executions = firstChild(fmpPlugin, "executions");
            if (executions == null) {
                executions = DomHelper.addChildElement(fmpPlugin, "executions");
                fmpPlugin.appendChild(doc.createTextNode("\n      "));
            } else {
                // lets remove all the children to be sure
                DomHelper.removeChildren(executions);
            }
            executions.appendChild(doc.createTextNode("\n        "));
            Element execution = DomHelper.addChildElement(executions, "execution");
            execution.appendChild(doc.createTextNode("\n          "));

            DomHelper.addChildElement(execution, "id", "fmp");
            execution.appendChild(doc.createTextNode("\n          "));

            Element goals = DomHelper.addChildElement(execution, "goals");
            execution.appendChild(doc.createTextNode("\n          "));

            String[] goalNames = {"resource", "helm", "build"};
            for (String goalName : goalNames) {
                goals.appendChild(doc.createTextNode("\n            "));
                DomHelper.addChildElement(goals, "goal", goalName);
            }
            goals.appendChild(doc.createTextNode("\n          "));

            executions.appendChild(doc.createTextNode("\n      "));
            updated= true;
        }
        return updated;
    }

    private boolean removePlugin(Document doc, String groupId, String artifactId) {
        Element plugin = findPlugin(doc, groupId, artifactId);
        if (plugin != null) {
            Node nextSibling = plugin.getNextSibling();
            DomHelper.detach(plugin);
            if (nextSibling instanceof TextNode) {
                DomHelper.detach(nextSibling);
            }
            return true;
        }
        return false;
    }

    private Element findOrAddPlugin(Document doc, String groupId, String artifactId, String version, Element configuration) {
        Element plugin = findPlugin(doc, groupId, artifactId);
        if (plugin != null) {
            return plugin;
        }
        Element documentElement = doc.getDocumentElement();
        Element build = firstChild(documentElement, "build");
        if (build == null) {
            build = DomHelper.addChildElement(documentElement, "build");
        }
        Element plugins = firstChild(build, "plugins");
        if (plugins == null) {
            plugins = DomHelper.addChildElement(build, "plugins");
        }
        plugins.appendChild(doc.createTextNode("\n      "));
        plugin = DomHelper.addChildElement(plugins, "plugin");
        plugin.appendChild(doc.createTextNode("\n        "));
        DomHelper.addChildElement(plugin, "groupId", groupId);
        plugin.appendChild(doc.createTextNode("\n        "));
        DomHelper.addChildElement(plugin, "artifactId", artifactId);
        plugin.appendChild(doc.createTextNode("\n        "));
        DomHelper.addChildElement(plugin, "version", version);
        plugin.appendChild(doc.createTextNode("\n        "));
        if (configuration != null) {
            plugin.appendChild(configuration);
        }
        plugin.appendChild(doc.createTextNode("\n      "));
        plugins.appendChild(doc.createTextNode("\n      "));
        return plugin;
    }

    private Element findPlugin(Document doc, String groupId, String artifactId) {
        Element build = firstChild(doc.getDocumentElement(), "build");
        if (build != null) {
            Element plugins = firstChild(build, "plugins");
            if (plugins != null) {
                NodeList childNodes = plugins.getChildNodes();
                if (childNodes != null) {
                    for (int i = 0; i < childNodes.getLength(); i++) {
                        Node item = childNodes.item(i);
                        if (item instanceof Element) {
                            Element plugin = (Element) item;
                            if (Objects.equals(DomHelper.firstChildTextContent(plugin, "groupId"), groupId) &&
                                    Objects.equals(DomHelper.firstChildTextContent(plugin, "artifactId"), artifactId)) {
                                return plugin;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }


    private boolean removePropertyName(String tagName, String value) {
        return tagName.startsWith("docker.port.") || tagName.startsWith("fabric8.") ||
                tagName.equals("docker.maven.plugin.version") || tagName.equals("jube.version");
    }

    protected String convertToConfigMapKey(String name) {
        return name.toLowerCase().replace('_', '-');
    }

    /**
     * Returns the migrated entity
     * <p>
     * - use Deployment by default instead of ReplicationController
     * - remove some annotations which should be generated at real build time
     * - replace groupId, artifactId and version with expressions
     */
    protected HasMetadata migrateEntity(HasMetadata entity, ConfigMap parameterConfigMap) {
        migrateMetadata(entity.getMetadata());

        if (entity instanceof ReplicationController) {
            ReplicationController rc = (ReplicationController) entity;
            ReplicationControllerSpec rcSpec = rc.getSpec();

            Deployment deployment = new Deployment();
            deployment.setMetadata(entity.getMetadata());
            if (rcSpec != null) {
                DeploymentSpec deploymentSpec = new DeploymentSpec();

                Map<String, String> selector = rcSpec.getSelector();
                if (selector != null) {
                    selector = new LinkedHashMap<>(selector);
                    // Deployment's selector should not have a version as we use that for each RC / RS
                    selector.remove("version");
                }
                migrateLabels(selector);
                deploymentSpec.setReplicas(rcSpec.getReplicas());
                if (selector != null) {
                    deploymentSpec.setSelector(new LabelSelectorBuilder().withMatchLabels(selector).build());
                }
                PodTemplateSpec podTemplateSpec = rcSpec.getTemplate();
                if (podTemplateSpec != null) {
                    PodSpec podSpec = podTemplateSpec.getSpec();
                    if (podSpec != null) {
                        List<Container> containers = podSpec.getContainers();
                        if (containers != null) {
                            for (Container container : containers) {
                                migrateContainer(container, parameterConfigMap);
                            }
                        }
                    }
                }

                PodTemplateSpec template = rcSpec.getTemplate();
                if (template != null) {
                    migrateMetadata(template.getMetadata());
                    deploymentSpec.setTemplate(template);
                }
                deployment.setSpec(deploymentSpec);
            }
            return deployment;
        }
        return entity;
    }

    protected void migrateContainer(Container container, ConfigMap parameterConfigMap) {
        String image = container.getImage();
        if (image != null) {
            MavenProject project = getProject();
            String version = project.getVersion();
            if (version != null) {
                String label = ":" + version;
                if (image.endsWith(label)) {
                    image = Strings.stripSuffix(image, label) + ":${project.version}";
                    container.setImage(image);
                } else {
                    getLog().warn("Image does not end with " + label + " as image is: " + image);
                }
            }
        }
        if (parameterConfigMap != null) {
            Map<String, String> parameters = parameterConfigMap.getData();
            if (parameters != null) {
                String configMapName = KubernetesHelper.getName(parameterConfigMap);
                List<EnvVar> env = container.getEnv();
                for (EnvVar envVar : env) {
                    String value = envVar.getValue();
                    if (value != null) {
                        String name = envVar.getName();
                        if (value.startsWith("${") && value.endsWith("}")) {
                            String variableName = Strings.stripPrefix(Strings.stripSuffix(name, "}"), "${");
                            String expression = convertToConfigMapKey(variableName);
                            if (parameters.containsKey(expression)) {
                                // lets switch to a refer to the config map!
                                envVar.setValue(null);
                                envVar.setValueFrom(new EnvVarSourceBuilder().
                                        withNewConfigMapKeyRef().withName(configMapName).withKey(expression).endConfigMapKeyRef().build());
                            }

                        }
                    }
                }
            }
        }
    }

    protected void migrateMetadata(ObjectMeta metadata) {
        if (metadata == null) {
            return;
        }
        Map<String, String> annotations = metadata.getAnnotations();
        if (annotations != null) {
            annotations.remove(Annotations.Builds.BUILD_ID);
            annotations.remove(Annotations.Builds.BUILD_URL);
            annotations.remove(Annotations.Builds.GIT_BRANCH);
            annotations.remove(Annotations.Builds.GIT_COMMIT);
            annotations.remove(Annotations.Builds.GIT_URL);
        }
        migrateLabels(metadata.getLabels());
    }

    private void migrateLabels(Map<String, String> labels) {
        if (labels != null) {
            MavenProject project = getProject();

            // TODO there is a currently a bug when using values other than ${project.artifactId} in fabric8-maven-plugin 3.x
            // where it overrides the value of the sepc.selector.matchLabels to the artifactId which then breaks
            // as the template.metadata.labels.project will differ
            boolean alwaysUseArtifactIdForProject = true;
            if (alwaysUseArtifactIdForProject) {
                if (labels.containsKey("project")) {
                    labels.put("project", "${project.artifactId}");
                }
            } else {
                replaceKeyValueWith(labels, "project", project.getArtifactId(), "${project.artifactId}");
            }
            replaceKeyValueWith(labels, "version", project.getVersion(), "${project.version}");
        }
    }

    private void replaceKeyValueWith(Map<String, String> labels, String key, String oldValue, String newValue) {
        String value = labels.get(key);
        if (Objects.equals(value, oldValue)) {
            labels.put(key, newValue);
        }
    }

    private void init() {
        kindAliases.put("configmap", "cm");
        kindAliases.put("deploymentconfig", "dc");
        kindAliases.put("replicationcontroller", "rc");
        kindAliases.put("replicaset", "rs");
        kindAliases.put("service", "svc");
        kindAliases.put("serviceaccount", "sa");
    }


    private String shortenKind(String kind) {
        String answer = kindAliases.get(kind);
        if (answer == null) {
            return kind;
        }
        return answer;
    }
}
