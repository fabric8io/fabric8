/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * A helper class for loading and saving the {@link ProjectConfig}
 */
public class ProjectConfigs {
    private static final transient Logger LOG = LoggerFactory.getLogger(ProjectConfigs.class);

    public static final String FILE_NAME = "fabric8.yml";

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
                LOG.warn("Failed to parse " + projectConfigFile);
            }
        }
        return new ProjectConfig();
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
        Object flowValue = map.get("flow");
        if (flowValue != null) {
            flow = flowValue.toString();
        }
        config.setFlow(flow);
    }

}
