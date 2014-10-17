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
import io.fabric8.common.util.Lists;
import io.fabric8.common.util.Strings;
import io.fabric8.kubernetes.api.model.Port;
import io.fabric8.kubernetes.template.GenerateTemplateDTO;
import io.fabric8.kubernetes.template.TemplateGenerator;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Generates or copies the Kubernetes JSON file and attaches it to the build so its
 * installed and released to maven repositories like other build artifacts.
 */
@Mojo(name = "json", defaultPhase = LifecyclePhase.PACKAGE)
public class JsonMojo extends AbstractFabric8Mojo {

    public static final String FABRIC8_PORT_HOST_PREFIX = "fabric8.port.host.";
    public static final String FABRIC8_PORT_CONTAINER_PREFIX = "fabric8.port.container.";
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
    @Parameter(property = "fabric8.json.template", defaultValue = TemplateGenerator.DEFAULT_TEMPLATE)
    private String jsonTemplate;


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
     * The name label used in the generated Kubernetes JSON template
     */
    @Parameter(property = "fabric8.kubernetes.containerName")
    private String kubernetesContainerName;


    /**
     * The labels passed into the generated Kubernetes JSON template.
     * <p/>
     * If no value is explicitly configured in the maven plugin then we use all maven properties starting with "fabric8.label."
     */
    @Parameter()
    private Map<String, String> labels;

    /**
     * The ports passed into the generated Kubernetes JSON template.
     */
    @Parameter()
    private List<Port> ports;

    /**
     * Maps the port names to the default container port numbers
     */
    @Parameter()
    private Map<String, Integer> defaultContainerPortMap;


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
        if (Strings.isNullOrBlank(jsonTemplate)) {
            throw new MojoExecutionException("No fabric8.jsonTemplate specified so cannot generate the Kubernetes JSON file!");
        } else {

            GenerateTemplateDTO config = new GenerateTemplateDTO();
            config.setTemplate(jsonTemplate);

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
            config.setLabels(getLabels());
            config.setVariables(variables);
            config.setPorts(getPorts());
            config.setName(getKubernetesName());
            config.setContainerName(getKubernetesContainerName());

            List<ClassLoader> classLoaders = Lists.newArrayList(Thread.currentThread().getContextClassLoader(),
                    getTestClassLoader(),
                    getClass().getClassLoader(),
                    TemplateGenerator.class.getClassLoader());

            TemplateGenerator generator = new TemplateGenerator(config, classLoaders);
            generator.generate(kubernetesJson);
        }
    }

    public String getKubernetesContainerName() {
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
            kubernetesContainerName = groupPrefix + "-" + project.getArtifactId();
        }
        return kubernetesContainerName;
    }

    public void setKubernetesContainerName(String kubernetesContainerName) {
        this.kubernetesContainerName = kubernetesContainerName;
    }

    public String getKubernetesName() {
        if (Strings.isNullOrBlank(kubernetesName)) {
            kubernetesName = Strings.convertToCamelCase(getKubernetesContainerName(), "-");
        }
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

    public List<Port> getPorts() {
        if (ports == null) {
            ports = new ArrayList<>();
        }
        if (ports.isEmpty()) {
            Map<String,Port> portMap = new HashMap<>();
            Map<String, String> hostPorts = findPropertiesWithPrefix(FABRIC8_PORT_HOST_PREFIX);
            Map<String, String> containerPorts = findPropertiesWithPrefix(FABRIC8_PORT_CONTAINER_PREFIX);

            for (Map.Entry<String, String> entry : containerPorts.entrySet()) {
                String name = entry.getKey();
                String portText = entry.getValue();
                Integer portNumber = parsePort(portText, FABRIC8_PORT_CONTAINER_PREFIX + name);
                if (portNumber != null) {
                    Port port = getOrCreatePort(portMap, name);
                    port.setContainerPort(portNumber);
                }
            }
            for (Map.Entry<String, String> entry : hostPorts.entrySet()) {
                String name = entry.getKey();
                String portText = entry.getValue();
                Integer portNumber = parsePort(portText, FABRIC8_PORT_HOST_PREFIX + name);
                if (portNumber != null) {
                    Port port = getOrCreatePort(portMap, name);
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
            ports.addAll(portMap.values());
        }
        return ports;
    }

    protected static Port getOrCreatePort(Map<String, Port> portMap, String name) {
        Port answer = portMap.get(name);
        if (answer == null) {
            answer = new Port();
            portMap.put(name, answer);

            // TODO should we set the name?
            // answer.setName(name);
        }
        return answer;
    }

    protected Integer parsePort(String portText, String propertyName) {
        if (Strings.isNotBlank(portText)) {
            try {
                return Integer.parseInt(portText);
            } catch (NumberFormatException e) {
                getLog().warn("Failed to parse port text: " + portText + " from maven property " + propertyName + ". " + e, e);
            }
        }
        return null;
    }

    public void setPorts(List<Port> ports) {
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
