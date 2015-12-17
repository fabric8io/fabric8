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
package io.fabric8.forge.camel.commands.project.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import io.fabric8.forge.addon.utils.CamelProjectHelper;
import io.fabric8.forge.camel.commands.project.completer.CamelComponentsCompleter;
import io.fabric8.forge.camel.commands.project.completer.CamelComponentsDtoCompleter;
import io.fabric8.forge.camel.commands.project.completer.CamelComponentsLabelCompleter;
import io.fabric8.forge.camel.commands.project.dto.ComponentDto;
import io.fabric8.forge.camel.commands.project.model.CamelComponentDetails;
import io.fabric8.forge.camel.commands.project.model.EndpointOptionByGroup;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.JSonSchemaHelper;
import org.jboss.forge.addon.convert.ConverterFactory;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.InputComponentFactory;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jboss.forge.roaster.model.util.Strings;

import static io.fabric8.forge.addon.utils.UIHelper.createUIInput;
import static io.fabric8.forge.camel.commands.project.helper.CamelCatalogHelper.endpointComponentName;

public final class CamelCommandsHelper {

    public static Iterable<String> createComponentLabelValues(Project project) {
        return new CamelComponentsLabelCompleter(project).getValueChoices();
    }

    public static Callable<Iterable<ComponentDto>> createComponentDtoValues(final Project project,
                                                                            final UISelectOne<String> componentCategoryFilter,
                                                                            final boolean excludeComponentsOnClasspath) {
        // use callable so we can live update the filter
        return new Callable<Iterable<ComponentDto>>() {
            @Override
            public Iterable<ComponentDto> call() throws Exception {
                String label = componentCategoryFilter.getValue();
                return new CamelComponentsDtoCompleter(project, null, excludeComponentsOnClasspath).getValueChoices(label);
            }
        };
    }

    public static Callable<Iterable<String>> createComponentNameValues(final Project project,
                                                                       final UISelectOne<String> componentCategoryFilter,
                                                                       final boolean excludeComponentsOnClasspath) {
        // use callable so we can live update the filter
        return new Callable<Iterable<String>>() {
            @Override
            public Iterable<String> call() throws Exception {
                String label = componentCategoryFilter.getValue();
                return new CamelComponentsCompleter(project, null, excludeComponentsOnClasspath).getValueChoices(label);
            }
        };
    }

    /**
     * Populates the details for the given component, returning a Result if it fails.
     */
    public static Result loadCamelComponentDetails(String camelComponentName, CamelComponentDetails details) {
        CamelCatalog catalog = new DefaultCamelCatalog();
        String json = catalog.componentJSonSchema(camelComponentName);
        if (json == null) {
            return Results.fail("Could not find catalog entry for component name: " + camelComponentName);
        }
        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("component", json, false);

        for (Map<String, String> row : data) {
            String javaType = row.get("javaType");
            if (!Strings.isNullOrEmpty(javaType)) {
                details.setComponentClassQName(javaType);
            }
            String artifactId = row.get("artifactId");
            if (!Strings.isNullOrEmpty(artifactId)) {
                details.setArtifactId(artifactId);
            }
        }
        if (Strings.isNullOrEmpty(details.getComponentClassQName())) {
            return Results.fail("Could not find fully qualified class name in catalog for component name: " + camelComponentName);
        }
        return null;
    }

    public static Result ensureCamelArtifactIdAdded(Project project, CamelComponentDetails details, DependencyInstaller dependencyInstaller) {
        String artifactId = details.getArtifactId();
        Dependency core = CamelProjectHelper.findCamelCoreDependency(project);
        if (core == null) {
            return Results.fail("The project does not include camel-core");
        }

        DependencyBuilder component = DependencyBuilder.create().setGroupId("org.apache.camel")
                .setArtifactId(artifactId).setVersion(core.getCoordinate().getVersion());

        // install the component
        dependencyInstaller.install(project, component);
        return null;
    }

    public static boolean isCdiProject(Project project) {
        return CamelProjectHelper.findCamelCDIDependency(project) != null;
    }

    public static boolean isSpringProject(Project project) {
        return CamelProjectHelper.findCamelSpringDependency(project) != null;
    }

    public static boolean isBlueprintProject(Project project) {
        return CamelProjectHelper.findCamelBlueprintDependency(project) != null;
    }

    public static void createCdiComponentProducerClass(JavaClassSource javaClass, CamelComponentDetails details, String camelComponentName, String componentInstanceName, String configurationCode) {
        javaClass.addImport("javax.enterprise.inject.Produces");
        javaClass.addImport("javax.inject.Singleton");
        javaClass.addImport("javax.inject.Named");
        javaClass.addImport(details.getComponentClassQName());

        String componentClassName = details.getComponentClassName();
        String methodName = "create" + Strings.capitalize(componentInstanceName) + "Component";

        String body = componentClassName + " component = new " + componentClassName + "();" + configurationCode + "\nreturn component;";

        MethodSource<JavaClassSource> method = javaClass.addMethod()
                .setPublic()
                .setReturnType(componentClassName)
                .setName(methodName)
                .setBody(body)
                .addThrows(Exception.class);

        method.addAnnotation("Named").setStringValue(camelComponentName);
        method.addAnnotation("Produces");
        method.addAnnotation("Singleton");
    }

    public static void createSpringComponentFactoryClass(JavaClassSource javaClass, CamelComponentDetails details, String camelComponentName, String componentInstanceName, String configurationCode) {
        javaClass.addAnnotation("Component");

        javaClass.addImport("org.springframework.beans.factory.config.BeanDefinition");
        javaClass.addImport("org.springframework.beans.factory.annotation.Qualifier");
        javaClass.addImport("org.springframework.context.annotation.Bean");
        javaClass.addImport("org.springframework.context.annotation.Scope");
        javaClass.addImport("org.springframework.stereotype.Component");
        javaClass.addImport(details.getComponentClassQName());

        String componentClassName = details.getComponentClassName();
        String methodName = "create" + Strings.capitalize(componentInstanceName) + "Component";

        String body = componentClassName + " component = new " + componentClassName + "();" + configurationCode + "\nreturn component;";

        MethodSource<JavaClassSource> method = javaClass.addMethod()
                .setPublic()
                .setReturnType(componentClassName)
                .setName(methodName)
                .setBody(body)
                .addThrows(Exception.class);

        method.addAnnotation("Qualifier").setStringValue(camelComponentName);
        method.addAnnotation("Bean");
        method.addAnnotation("Scope").setLiteralValue("BeanDefinition.SCOPE_SINGLETON");
    }

    /**
     * Converts a java type as a string to a valid input type and returns the class or null if its not supported
     */
    public static Class<Object> loadValidInputTypes(String javaType, String type) {
        // we have generics in the javatype, if so remove it so its loadable from a classloader
        int idx = javaType.indexOf('<');
        if (idx > 0) {
            javaType = javaType.substring(0, idx);
        }

        try {
            Class<Object> clazz = getPrimitiveWrapperClassType(type);
            if (clazz == null) {
                clazz = loadPrimitiveWrapperType(javaType);
            }
            if (clazz == null) {
                clazz = loadStringSupportedType(javaType);
            }
            if (clazz == null) {
                try {
                    clazz = (Class<Object>) Class.forName(javaType);
                } catch (Throwable e) {
                    // its a custom java type so use String as the input type, so you can refer to it using # lookup
                    if ("object".equals(type)) {
                        clazz = loadPrimitiveWrapperType("java.lang.String");
                    }
                }
            }

            // favor specialized UI for these types
            if (clazz != null && (clazz.equals(String.class) || clazz.equals(Date.class) || clazz.equals(Boolean.class)
                    || clazz.isPrimitive() || Number.class.isAssignableFrom(clazz))) {
                return clazz;
            }

            // its a custom java type so use String as the input type, so you can refer to it using # lookup
            if ("object".equals(type)) {
                clazz = loadPrimitiveWrapperType("java.lang.String");
                return clazz;
            }

        } catch (Throwable e) {
            // ignore errors
        }
        return null;
    }

    private static Class loadStringSupportedType(String javaType) {
        if ("java.io.File".equals(javaType)) {
            return String.class;
        } else if ("java.net.URL".equals(javaType)) {
            return String.class;
        } else if ("java.net.URI".equals(javaType)) {
            return String.class;
        }
        return null;
    }

    /**
     * Gets the JSon schema primitive type.
     *
     * @param name the json type
     * @return the primitive Java Class type
     */
    public static Class getPrimitiveWrapperClassType(String name) {
        if ("string".equals(name)) {
            return String.class;
        } else if ("boolean".equals(name)) {
            return Boolean.class;
        } else if ("integer".equals(name)) {
            return Integer.class;
        } else if ("number".equals(name)) {
            return Float.class;
        }

        return null;
    }

    private static Class loadPrimitiveWrapperType(String name) {
        // special for byte[] or Object[] as its common to use
        if ("java.lang.byte[]".equals(name) || "byte[]".equals(name)) {
            return Byte[].class;
        } else if ("java.lang.Byte[]".equals(name) || "Byte[]".equals(name)) {
            return Byte[].class;
        } else if ("java.lang.Object[]".equals(name) || "Object[]".equals(name)) {
            return Object[].class;
        } else if ("java.lang.String[]".equals(name) || "String[]".equals(name)) {
            return String[].class;
            // and these is common as well
        } else if ("java.lang.String".equals(name) || "String".equals(name)) {
            return String.class;
        } else if ("java.lang.Boolean".equals(name) || "Boolean".equals(name)) {
            return Boolean.class;
        } else if ("boolean".equals(name)) {
            return Boolean.class;
        } else if ("java.lang.Integer".equals(name) || "Integer".equals(name)) {
            return Integer.class;
        } else if ("int".equals(name)) {
            return Integer.class;
        } else if ("java.lang.Long".equals(name) || "Long".equals(name)) {
            return Long.class;
        } else if ("long".equals(name)) {
            return Long.class;
        } else if ("java.lang.Short".equals(name) || "Short".equals(name)) {
            return Short.class;
        } else if ("short".equals(name)) {
            return Short.class;
        } else if ("java.lang.Byte".equals(name) || "Byte".equals(name)) {
            return Byte.class;
        } else if ("byte".equals(name)) {
            return Byte.class;
        } else if ("java.lang.Float".equals(name) || "Float".equals(name)) {
            return Float.class;
        } else if ("float".equals(name)) {
            return Float.class;
        } else if ("java.lang.Double".equals(name) || "Double".equals(name)) {
            return Double.class;
        } else if ("double".equals(name)) {
            return Double.class;
        } else if ("java.lang.Character".equals(name) || "Character".equals(name)) {
            return Character.class;
        } else if ("char".equals(name)) {
            return Character.class;
        }
        return null;
    }

    public static List<EndpointOptionByGroup> createUIInputsForCamelComponent(String camelComponentName, String uri, int maxOptionsPerPage, boolean consumerOnly, boolean producerOnly,
                                                                              InputComponentFactory componentFactory, ConverterFactory converterFactory, UIContext ui) throws Exception {
        List<EndpointOptionByGroup> answer = new ArrayList<>();

        if (camelComponentName == null && uri != null) {
            camelComponentName = endpointComponentName(uri);
        }

        CamelCatalog catalog = new DefaultCamelCatalog();
        String json = catalog.componentJSonSchema(camelComponentName);
        if (json == null) {
            throw new IllegalArgumentException("Could not find catalog entry for component name: " + camelComponentName);
        }

        // is the component consumer or producer only, if so we do not need any kind of filter
        boolean componentConsumerOnly = CamelCatalogHelper.isComponentConsumerOnly(camelComponentName);
        boolean componentProducerOnly = CamelCatalogHelper.isComponentProducerOnly(camelComponentName);
        if (componentConsumerOnly || componentProducerOnly) {
            // reset the filters as the component can only be one of them anyway, so we should show all options
            consumerOnly = false;
            producerOnly = false;
        }

        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("properties", json, true);

        Map<String, String> currentValues = uri != null ? catalog.endpointProperties(uri) : Collections.EMPTY_MAP;

        if (data != null) {
            List<InputComponent> inputs = new ArrayList<>();
            EndpointOptionByGroup current = new EndpointOptionByGroup();
            current.setGroup(null);
            current.setInputs(inputs);

            Set<String> namesAdded = new HashSet<>();

            for (Map<String, String> propertyMap : data) {
                String name = propertyMap.get("name");
                String kind = propertyMap.get("kind");
                String group = propertyMap.get("group");
                String label = propertyMap.get("label");
                String type = propertyMap.get("type");
                String javaType = propertyMap.get("javaType");
                String deprecated = propertyMap.get("deprecated");
                String required = propertyMap.get("required");
                String currentValue = currentValues.get(name);
                String defaultValue = propertyMap.get("defaultValue");
                String description = propertyMap.get("description");
                String enums = propertyMap.get("enum");

                if (current.getGroup() == null) {
                    current.setGroup(group);
                }
                // its a new group
                if (group != null && !group.equals(current.getGroup())) {
                    if (!current.getInputs().isEmpty()) {
                        // this group is now done so add to answer
                        answer.add(current);
                    }

                    // get ready for a new group
                    inputs = new ArrayList<>();
                    current = new EndpointOptionByGroup();
                    current.setGroup(group);
                    current.setInputs(inputs);
                }

                // filter out options in case we should only include consumers or producers only
                if (consumerOnly && label != null) {
                    if (!label.contains("consumer")) {
                        continue;
                    }
                }
                if (producerOnly && label != null) {
                    if (!label.contains("producer")) {
                        continue;
                    }
                }

                if (!Strings.isNullOrEmpty(name)) {
                    Class<Object> inputClazz = CamelCommandsHelper.loadValidInputTypes(javaType, type);
                    if (inputClazz != null) {
                        if (namesAdded.add(name)) {

                            // we do not want descriptions in CLI mode as it makes the UI clutter
                            boolean gui = ui.getProvider().isGUI();
                            if (!gui) {
                                description = "";
                            }

                            // if its an enum and its optional then make sure there is a default value
                            // if no default value exists then add none as the 1st choice default value
                            // otherwise the GUI makes us force to select an option which is not what we want
                            if (enums != null && required == null || "false".equals(required)) {
                                if (defaultValue == null || defaultValue.isEmpty()) {
                                    defaultValue = "none";
                                    if (!enums.startsWith("none,")) {
                                        enums = "none," + enums;
                                    }
                                }
                            }

                            InputComponent input = createUIInput(componentFactory, converterFactory, name, inputClazz, required, currentValue, defaultValue, enums, description);
                            if (input != null) {
                                inputs.add(input);

                                // if we hit max options then create a new group
                                if (inputs.size() == maxOptionsPerPage) {
                                    // this group is now done so add to answer
                                    if (!current.getInputs().isEmpty()) {
                                        answer.add(current);
                                    }
                                    // get ready for a new group
                                    inputs = new ArrayList<>();
                                    current = new EndpointOptionByGroup();
                                    current.setGroup(group);
                                    current.setInputs(inputs);
                                }
                            }
                        }
                    }
                }
            }

            // add last group if there was some new inputs
            if (!inputs.isEmpty()) {
                answer.add(current);
            }
        }

        return answer;
    }

}
