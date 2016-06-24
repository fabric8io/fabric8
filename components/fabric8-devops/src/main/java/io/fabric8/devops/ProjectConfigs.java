/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.devops;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.utils.Maps;
import io.fabric8.utils.Strings;
import io.fabric8.utils.Systems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A helper class for loading and saving the {@link ProjectConfig}
 */
public class ProjectConfigs {
    private static final transient Logger LOG = LoggerFactory.getLogger(ProjectConfigs.class);

    public static final String FILE_NAME = "fabric8.yml";
    public static final String LOCAL_FLOW_FILE_NAME = "Jenkinsfile";

    public static String toYaml(Object dto) throws JsonProcessingException {
        ObjectMapper mapper = createObjectMapper();
        return mapper.writeValueAsString(dto);
    }

    /**
     * Returns the configuration from the {@link #FILE_NAME} in the given folder or returns the default configuration
     */
    public static ProjectConfig loadFromFolder(File folder) {
        File projectConfigFile = new File(folder, FILE_NAME);
        if (projectConfigFile != null && projectConfigFile.exists() && projectConfigFile.isFile()) {
            LOG.debug("Parsing fabric8 devops project configuration from: " + projectConfigFile.getName());
            try {
                return ProjectConfigs.parseProjectConfig(projectConfigFile);
            } catch (IOException e) {
                LOG.warn("Failed to parse " + projectConfigFile + ". " + e, e);
            }
        }
        return new ProjectConfig();
    }


    /**
     * Tries to find the project configuration from the current directory or a parent folder.
     *
     * If no fabric8.yml file can be found just return an empty configuration
     */
    public static ProjectConfig findFromFolder(File folder) {
        if (folder.isDirectory()) {
            File projectConfigFile = new File(folder, FILE_NAME);
            if (projectConfigFile != null && projectConfigFile.exists() && projectConfigFile.isFile()) {
                return loadFromFolder(folder);
            }
            File parentFile = folder.getParentFile();
            if (parentFile != null) {
                return findFromFolder(parentFile);
            }
        }
        return new ProjectConfig();
    }

    /**
     * Returns the project config from the given url if it exists or null
     */
    public static ProjectConfig loadFromUrl(String url) {
        if (Strings.isNotBlank(url)) {
            try {
                return loadFromUrl(new URL(url));
            } catch (MalformedURLException e) {
                LOG.warn("Failed to create URL from: " + url + ". " + e, e);
            }
        }
        return null;
    }

    /**
     * Returns the project config from the given url if it exists or null
     */
    public static ProjectConfig loadFromUrl(URL url) {
        InputStream input = null;
        try {
            input = url.openStream();
        } catch (FileNotFoundException e) {
            LOG.info("No fabric8.yml at URL: " + url);
        } catch (IOException e) {
            LOG.warn("Failed to open fabric8.yml file at URL: " + url + ". " + e, e);
        }
        if (input != null) {
            try {
                LOG.info("Parsing " + ProjectConfigs.FILE_NAME + " from " + url);
                return ProjectConfigs.parseProjectConfig(input);
            } catch (IOException e) {
                LOG.warn("Failed to parse " + ProjectConfigs.FILE_NAME + " from " + url + ". " + e, e);
            }
        }
        return null;
    }

    /**
     * Returns true if the given folder has a configuration file called {@link #FILE_NAME}
     */
    public static boolean hasConfigFile(File folder) {
        File projectConfigFile = new File(folder, FILE_NAME);
        return projectConfigFile != null && projectConfigFile.exists() && projectConfigFile.isFile();
    }

    /**
     * Creates a configured Jackson object mapper for parsing YAML
     */
    public static ObjectMapper createObjectMapper() {
        return new ObjectMapper(new YAMLFactory());
    }

    public static ProjectConfig parseProjectConfig(File file) throws IOException {
        return parseYaml(file, ProjectConfig.class);
    }

    public static ProjectConfig parseProjectConfig(InputStream input) throws IOException {
        return parseYaml(input, ProjectConfig.class);
    }

    public static ProjectConfig parseProjectConfig(String yaml) throws IOException {
        return parseYaml(yaml, ProjectConfig.class);
    }

    private static <T> T parseYaml(File file, Class<T> clazz) throws IOException {
        ObjectMapper mapper = createObjectMapper();
        return mapper.readValue(file, clazz);
    }

    static <T> List<T> parseYamlValues(File file, Class<T> clazz) throws IOException {
        ObjectMapper mapper = createObjectMapper();
        MappingIterator<T> iter = mapper.readerFor(clazz).readValues(file);
        List<T> answer = new ArrayList<>();
        while (iter.hasNext()) {
            answer.add(iter.next());
        }
        return answer;
    }

    private static <T> T parseYaml(InputStream inputStream, Class<T> clazz) throws IOException {
        ObjectMapper mapper = createObjectMapper();
        return mapper.readValue(inputStream, clazz);
    }

    private static <T> T parseYaml(String yaml, Class<T> clazz) throws IOException {
        ObjectMapper mapper = createObjectMapper();
        return mapper.readValue(yaml, clazz);
    }

    /**
     * Saves the fabric8.yml file to the given project directory
     */
    public static boolean saveToFolder(File basedir, ProjectConfig config, boolean overwriteIfExists) throws IOException {
        File file = new File(basedir, ProjectConfigs.FILE_NAME);
        if (file.exists()) {
            if (!overwriteIfExists) {
                LOG.warn("Not generating " + file + " as it already exists");
                return false;
            }
        }
        return saveConfig(config, file);
    }

    /**
     * Saves the configuration as YAML in the given file
     */
    public static boolean saveConfig(ProjectConfig config, File file) throws IOException {
        createObjectMapper().writeValue(file, config);
        return true;
    }

    /**
     * Configures the given {@link ProjectConfig} with a map of key value pairs from
     * something like a JBoss Forge command
     */
    public static void configureProperties(ProjectConfig config, Map map) {
        Class<? extends ProjectConfig> clazz = config.getClass();
        BeanInfo beanInfo = null;
        try {
            beanInfo = Introspector.getBeanInfo(clazz);
        } catch (IntrospectionException e) {
            LOG.warn("Could not introspect " + clazz.getName() + ". " + e, e);
        }
        if (beanInfo != null) {
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor descriptor : propertyDescriptors) {
                Method writeMethod = descriptor.getWriteMethod();
                if (writeMethod != null) {
                    String name = descriptor.getName();
                    Object value = map.get(name);
                    if (value != null) {
                        Object safeValue = null;
                        Class<?> propertyType = descriptor.getPropertyType();
                        if (propertyType.isInstance(value)) {
                            safeValue = value;
                        } else {
                            PropertyEditor editor = descriptor.createPropertyEditor(config);
                            if (editor == null) {
                                editor = PropertyEditorManager.findEditor(propertyType);
                            }
                            if (editor != null) {
                                String text = value.toString();
                                editor.setAsText(text);
                                safeValue = editor.getValue();
                            } else {
                                LOG.warn("Cannot update property " + name
                                        + " with value " + value
                                        + " of type " + propertyType.getName()
                                        + " on " + clazz.getName());
                            }
                        }
                        if (safeValue != null) {
                            try {
                                writeMethod.invoke(config, safeValue);
                            } catch (Exception e) {
                                LOG.warn("Failed to set property " + name
                                        + " with value " + value
                                        + " on " + clazz.getName() + " " + config + ". " + e, e);
                            }

                        }
                    }
                }
            }
        }
        String flow = null;
        Object flowValue = map.get("pipeline");
        if (flowValue == null) {
            flowValue = map.get("flow");
        }
        if (flowValue != null) {
            flow = flowValue.toString();
        }
        config.setPipeline(flow);
    }

    /**
     * If no environments have been configured lets default them from the `FABRIC8_DEFAULT_ENVIRONMENTS` environment variable
     */
    public static void defaultEnvironments(ProjectConfig config, String namespace) {
        if (config != null) {
            String buildName = config.getBuildName();
            if (Strings.isNotBlank(buildName) && Maps.isNullOrEmpty(config.getEnvironments())) {
                // lets default the environments from env var
                String defaultEnvironmentsText = Systems.getEnvVarOrSystemProperty("FABRIC8_DEFAULT_ENVIRONMENTS", "Testing=${namespace}-testing,Staging=${namespace}-staging,Production=${namespace}-prod");
                String text = Strings.replaceAllWithoutRegex(defaultEnvironmentsText, "${buildName}", buildName);
                text = Strings.replaceAllWithoutRegex(text, "${namespace}", namespace);
                LinkedHashMap<String,String> environments = Maps.parseMap(text);
                config.setEnvironments(environments);
            }
        }
    }

}
