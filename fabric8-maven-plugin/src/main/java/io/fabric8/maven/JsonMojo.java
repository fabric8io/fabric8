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
package io.fabric8.maven;

import io.fabric8.common.util.Files;
import io.fabric8.common.util.Strings;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.mvel2.ParserContext;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Generates or copies the Kubernetes JSON file and attaches it to the build so its
 * installed and released to maven repositories like other build artifacts.
 */
@Mojo(name = "json", defaultPhase = LifecyclePhase.PACKAGE)
public class JsonMojo extends AbstractFabric8Mojo {

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
     * Which MVEL based template should we use to generate the kubernetes JSON?
     */
    @Parameter(property = "fabric8.zip.template", defaultValue = "io/fabric8/templates/default.mvel")
    private String zipTemplate;


    /**
     * Whether or not we should generate the Kubernetes JSON file using the MVEL template if there is not one specified
     * in the build (usually in src/main/resources/kubernetes.json)
     */
    @Parameter(property = "fabric8.generateZip", defaultValue = "true")
    private boolean generateJson;

    /**
     * The ID prefix used in the generated Kubernetes JSON template
     */
    @Parameter(property = "fabric8.kubernetes.id", defaultValue = "${project.groupId}-${project.artifactId}-${project.version}")
    private String kubernetesId;

    /**
     * The name label used in the generated Kubernetes JSON template
     */
    @Parameter(property = "fabric8.kubernetes.name")
    private String kubernetesName;


    /**
     * The labels passed into the generated Kubernetes JSON template.
     * <p/>
     * If no value is explicitly configured in the maven plugin then we use all maven properties starting with "fabric8.label."
     */
    @Parameter()
    private Map<String, String> labels;

    /**
     * The ports passed into the generated Kubernetes JSON template.
     * <p/>
     * If no value is explicitly configured in the maven plugin then we use all maven properties starting with "fabric8.port."
     */
    @Parameter()
    private Map<String, String> ports;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File json = getKubernetesJson();
        getLog().info("Configured with file: " + json);
        if (json == null) {
            throw new MojoExecutionException("No kubernetes json file is specified!");
        }
        if (!json.exists()) {
            if (!isPom(getProject()) && !isIgnoreProject() && generateJson) {
                generateKubernetesJson(json);
            }
        } else {
            getLog().warn("No kubernetes JSON file specified");
        }
        if (Files.isFile(json)) {
            getLog().info("Attaching kubernetes json file: " + json + " to the build");
            projectHelper.attachArtifact(getProject(), artifactType, artifactClassifier, json);
        }
    }

    protected void generateKubernetesJson(File kubernetesJson) throws MojoExecutionException {
        if (Strings.isNullOrBlank(zipTemplate)) {
            throw new MojoExecutionException("No fabric8.zipTemplate specified so cannot generate the Kubernetes JSON file!");
        } else {
            InputStream in = loadPluginResource(zipTemplate);
            if (in == null) {
                throw new MojoExecutionException("Could not find template: " + zipTemplate + " on the ClassPath when trying to generate the Kubernetes JSON!");
            }
            ParserContext parserContext = new ParserContext();
            Map<String, Object> variables = new HashMap<>();


            // TODO populate properties, project etc.
            MavenProject project = getProject();
            Properties properties = project.getProperties();
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
            addIfNotDefined(variables, "fabric8_kubernetes_id", kubernetesId);
            addIfNotDefined(variables, "fabric8_kubernetes_name", getKubernetesName());
            Map<String, String> labels = getLabels();
            Map<String, String> ports = getPorts();
            variables.put("project", project);
            variables.put("labels", labels);
            variables.put("ports", ports);

            try {
                CompiledTemplate compiledTemplate = TemplateCompiler.compileTemplate(in, parserContext);
                String answer = TemplateRuntime.execute(compiledTemplate, parserContext, variables).toString();
                Files.writeToFile(kubernetesJson, answer, Charset.defaultCharset());
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to generate Kubernetes JSON from template " + zipTemplate + ". " + e, e);
            }
        }
    }

    public String getKubernetesName() {
        if (Strings.isNullOrBlank(kubernetesName)) {
            // lets generate it from the docker user and the camelCase artifactId
            String groupPrefix = null;
            MavenProject project = getProject();
            String imageName = project.getProperties().getProperty("docker.dataImage");
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
            String name = groupPrefix + "-" + project.getArtifactId();
            kubernetesName = Strings.convertToCamelCase(name, "-");
        }
        return kubernetesName;
    }

    public void setKubernetesName(String kubernetesName) {
        this.kubernetesName = kubernetesName;
    }

    public Map<String, String> getPorts() {
        if (ports == null) {
            ports = new HashMap<>();
        }
        if (ports.isEmpty()) {
            ports = findPropertiesWithPrefix("fabric8.port.");
        }
        return ports;
    }

    public void setPorts(Map<String, String> ports) {
        this.ports = ports;
    }

    public Map<String, String> getLabels() {
        if (labels == null) {
            labels = new HashMap<>();
        }
        if (labels.isEmpty()) {
            labels = findPropertiesWithPrefix("fabric8.label.");
        }
        return labels;
    }

    protected Map<String, String> findPropertiesWithPrefix(String prefix) {
        Map<String, String> answer = new HashMap<>();
        Properties properties = getProject().getProperties();
        Set<Map.Entry<Object, Object>> entries = properties.entrySet();
        for (Map.Entry<Object, Object> entry : entries) {
            Object value = entry.getValue();
            Object key = entry.getKey();
            if (key instanceof String && value != null) {
                String keyText = key.toString();
                if (keyText.startsWith(prefix)) {
                    String newKey = keyText.substring(prefix.length());
                    answer.put(newKey, value.toString());
                }
            }
        }
        return answer;
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
