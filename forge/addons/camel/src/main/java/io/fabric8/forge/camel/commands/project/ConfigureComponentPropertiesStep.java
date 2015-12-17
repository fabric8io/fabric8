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
package io.fabric8.forge.camel.commands.project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import io.fabric8.forge.addon.utils.CamelProjectHelper;
import io.fabric8.forge.camel.commands.project.helper.CamelCommandsHelper;
import io.fabric8.forge.camel.commands.project.model.CamelComponentDetails;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.JSonSchemaHelper;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.InputComponentFactory;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.util.Strings;

import static io.fabric8.forge.addon.utils.UIHelper.createUIInput;
import static io.fabric8.forge.camel.commands.project.helper.CamelCommandsHelper.ensureCamelArtifactIdAdded;
import static io.fabric8.forge.camel.commands.project.helper.CamelCommandsHelper.loadCamelComponentDetails;
import static io.fabric8.forge.camel.commands.project.helper.CamelCommandsHelper.loadValidInputTypes;

public class ConfigureComponentPropertiesStep extends AbstractCamelProjectCommand implements UIWizardStep {

    @Inject
    private InputComponentFactory componentFactory;

    @Inject
    private DependencyInstaller dependencyInstaller;

    @Inject
    private DependencyResolver dependencyResolver;

    @Inject
    private CamelCatalog camelCatalog;

    private List<InputComponent> inputs = new ArrayList<>();

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(ConfigureComponentPropertiesStep.class).name(
                "Camel: New Component").category(Categories.create(CATEGORY))
                .description("Configure the component options to use");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        // lets create a field for each property on the component

        Map<Object, Object> attributeMap = builder.getUIContext().getAttributeMap();
        String camelComponentName = mandatoryAttributeValue(attributeMap, "componentName");
        String json = camelCatalog.componentJSonSchema(camelComponentName);
        if (json == null) {
            throw new IllegalArgumentException("Could not find catalog entry for component name: " + camelComponentName);
        }
        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("componentProperties", json, true);
        if (data != null) {
            Set<String> namesAdded = new HashSet<>();
            for (Map<String, String> propertyMap : data) {
                String name = propertyMap.get("name");
                String kind = propertyMap.get("kind");
                String type = propertyMap.get("type");
                String javaType = propertyMap.get("javaType");
                String deprecated = propertyMap.get("deprecated");
                String required = propertyMap.get("required");
                String currentValue = null;
                String defaultValue = propertyMap.get("defaultValue");
                String description = propertyMap.get("description");
                String enums = propertyMap.get("enum");

                if (!Strings.isNullOrEmpty(name)) {
                    Class<Object> inputClazz = loadValidInputTypes(javaType, type);
                    if (inputClazz != null) {
                        if (namesAdded.add(name)) {
                            InputComponent input = createUIInput(componentFactory, getConverterFactory(), name, inputClazz, required, currentValue, defaultValue, enums, description);
                            if (input != null) {
                                builder.add(input);
                                inputs.add(input);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        return null;
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();
        try {
            String camelComponentName = mandatoryAttributeValue(attributeMap, "componentName");
            String componentInstanceName = mandatoryAttributeValue(attributeMap, "instanceName");
            String generatePackageName = mandatoryAttributeValue(attributeMap, "targetPackage");
            String generateClassName = mandatoryAttributeValue(attributeMap, "className");
            String kind = mandatoryAttributeValue(attributeMap, "kind");

            Project project = getSelectedProject(context);
            JavaSourceFacet facet = project.getFacet(JavaSourceFacet.class);

            // does the project already have camel?
            Dependency core = CamelProjectHelper.findCamelCoreDependency(project);
            if (core == null) {
                return Results.fail("The project does not include camel-core");
            }

            // lets find the camel component class

            CamelComponentDetails details = new CamelComponentDetails();
            Result result = loadCamelComponentDetails(camelComponentName, details);
            if (result != null) {
                return result;
            }
            result = ensureCamelArtifactIdAdded(project, details, dependencyInstaller);
            if (result != null) {
                return result;
            }

            // do we already have a class with the name
            String fqn = generatePackageName != null ? generatePackageName + "." + generateClassName : generateClassName;

            JavaResource existing = facet.getJavaResource(fqn);
            if (existing != null && existing.exists()) {
                return Results.fail("A class with name " + fqn + " already exists");
            }

            // need to parse to be able to extends another class
            final JavaClassSource javaClass = Roaster.create(JavaClassSource.class);
            javaClass.setName(generateClassName);
            if (generatePackageName != null) {
                javaClass.setPackage(generatePackageName);
            }

            // generate the correct class payload based on the style...
            StringBuilder buffer = new StringBuilder();
            for (InputComponent input : inputs) {
                // only use value if there was a value set
                if (input.hasValue()) {
                    String valueExpression = null;

                    Object value = input.getValue();
                    if (value != null) {
                        if (value instanceof String) {
                            String text = value.toString();
                            if (!Strings.isBlank(text)) {
                                valueExpression = "\"" + text + "\"";
                            }
                        }
                        if (value instanceof Number) {
                            valueExpression = value.toString();
                        }
                    }
                    if (valueExpression != null) {
                        buffer.append("\n");
                        buffer.append("component.set");
                        buffer.append(Strings.capitalize(input.getName()));
                        buffer.append("(");
                        buffer.append(valueExpression);
                        buffer.append(");");
                    }
                }
            }
            String configurationCode = buffer.toString();
            if (kind.equals("cdi")) {
                CamelCommandsHelper.createCdiComponentProducerClass(javaClass, details, camelComponentName, componentInstanceName, configurationCode);
            } else {
                CamelCommandsHelper.createSpringComponentFactoryClass(javaClass, details, camelComponentName, componentInstanceName, configurationCode);
            }

            facet.saveJavaSource(javaClass);

            return Results.success("Created new class " + generateClassName);
        } catch (IllegalArgumentException e) {
            return Results.fail(e.getMessage());
        }
    }

    /**
     * Returns the mandatory String value of the given name
     *
     * @throws IllegalArgumentException if the value is not available in the given attribute map
     */
    public static String mandatoryAttributeValue(Map<Object, Object> attributeMap, String name) {
        Object value = attributeMap.get(name);
        if (value != null) {
            String text = value.toString();
            if (!Strings.isBlank(text)) {
                return text;
            }
        }
        throw new IllegalArgumentException("The attribute value '" + name + "' did not get passed on from the previous wizard page");
    }
}
