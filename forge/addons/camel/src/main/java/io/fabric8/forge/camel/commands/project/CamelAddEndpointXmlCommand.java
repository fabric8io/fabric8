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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import io.fabric8.forge.camel.commands.project.completer.XmlFileCompleter;
import io.fabric8.forge.camel.commands.project.helper.CamelCatalogHelper;
import io.fabric8.forge.camel.commands.project.helper.CamelCommandsHelper;
import io.fabric8.forge.camel.commands.project.model.EndpointOptionByGroup;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.InputComponentFactory;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.input.ValueChangeListener;
import org.jboss.forge.addon.ui.input.events.ValueChangeEvent;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.result.navigation.NavigationResultBuilder;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizard;

import static io.fabric8.forge.camel.commands.project.helper.CamelCommandsHelper.createUIInputsForCamelComponent;

public class CamelAddEndpointXmlCommand extends AbstractCamelProjectCommand implements UIWizard {

    private static final int MAX_OPTIONS = 20;

    @Inject
    @WithAttributes(label = "Filter", required = false, description = "To filter components")
    private UISelectOne<String> componentNameFilter;

    @Inject
    @WithAttributes(label = "Name", required = true, description = "Name of component to use for the endpoint")
    private UISelectOne<String> componentName;

    @Inject
    @WithAttributes(label = "Endpoint type", required = true, description = "Type of endpoint")
    private UISelectOne<String> endpointType;

    @Inject
    @WithAttributes(label = "Instance Name", required = true, description = "Name of endpoint instance to add")
    private UIInput<String> instanceName;

    @Inject
    @WithAttributes(label = "XML File", required = true, description = "The XML file to use (either Spring or Blueprint)")
    private UISelectOne<String> xml;

    @Inject
    private InputComponentFactory componentFactory;

    @Inject
    private DependencyInstaller dependencyInstaller;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(CamelAddEndpointXmlCommand.class).name(
                "Camel: Add Endpoint XML").category(Categories.create(CATEGORY))
                .description("Adds a Camel endpoint to an existing XML file");
    }

    @Override
    public boolean isEnabled(UIContext context) {
        boolean enabled = super.isEnabled(context);
        if (enabled) {
            // must be spring or blueprint project for editing xml files
            boolean spring = CamelCommandsHelper.isSpringProject(getSelectedProject(context));
            boolean blueprint = CamelCommandsHelper.isBlueprintProject(getSelectedProject(context));
            return spring || blueprint;
        }
        return false;
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        Map<Object, Object> attributeMap = builder.getUIContext().getAttributeMap();
        attributeMap.remove("navigationResult");

        Project project = getSelectedProject(builder.getUIContext());
        final ResourcesFacet resourcesFacet = project.getFacet(ResourcesFacet.class);
        WebResourcesFacet webResourcesFacet = null;
        if (project.hasFacet(WebResourcesFacet.class)) {
            webResourcesFacet = project.getFacet(WebResourcesFacet.class);
        }

        componentNameFilter.setValueChoices(CamelCommandsHelper.createComponentNameValues(project));
        componentNameFilter.setDefaultValue("<all>");
        componentName.setValueChoices(CamelCommandsHelper.createComponentNameValues(project, componentNameFilter, false));
        // show note about the chosen component
        componentName.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChanged(ValueChangeEvent event) {
                String component = event.getNewValue() != null ? event.getNewValue().toString() : null;
                if (component != null) {
                    String description = CamelCatalogHelper.getComponentDescription(component);
                    componentName.setNote(description != null ? description : "");
                } else {
                    componentName.setNote("");
                }

                // limit the endpoint types what is possible with this chosen component
                if (component != null) {
                    boolean consumerOnly = CamelCatalogHelper.isComponentConsumerOnly(component);
                    boolean producerOnly = CamelCatalogHelper.isComponentConsumerOnly(component);
                    if (consumerOnly) {
                        String[] types = new String[]{"Consumer"};
                        endpointType.setValueChoices(Arrays.asList(types));
                        endpointType.setValue("Consumer");
                        endpointType.setDefaultValue("Consumer");
                    } else if (producerOnly) {
                        String[] types = new String[]{"Producer"};
                        endpointType.setValueChoices(Arrays.asList(types));
                        endpointType.setValue("Producer");
                        endpointType.setDefaultValue("Producer");
                    } else {
                        String[] types = new String[]{"<any>", "Consumer", "Producer"};
                        endpointType.setValueChoices(Arrays.asList(types));
                        endpointType.setValue("<any>");
                        endpointType.setDefaultValue("<any>");
                    }
                } else {
                    String[] types = new String[]{"<any>", "Consumer", "Producer"};
                    endpointType.setValueChoices(Arrays.asList(types));
                    endpointType.setValue("<any>");
                    endpointType.setDefaultValue("<any>");
                }
            }
        });

        String[] types = new String[]{"<any>", "Consumer", "Producer"};
        endpointType.setValueChoices(Arrays.asList(types));
        endpointType.setDefaultValue("<any>");

        // use value choices instead of completer as that works better in web console
        xml.setValueChoices(new XmlFileCompleter(resourcesFacet, webResourcesFacet).getFiles());
        builder.add(componentNameFilter).add(componentName).add(endpointType).add(instanceName).add(xml);
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();

        // always refresh these as the end user may have edited the instance name
        attributeMap.put("instanceName", instanceName.getValue());
        attributeMap.put("xml", xml.getValue());
        attributeMap.put("mode", "add");
        attributeMap.put("kind", "xml");

        // must be same component name and endpoint type to allow reusing existing navigation result
        String previous = (String) attributeMap.get("componentName");
        String previous2 = (String) attributeMap.get("endpointType");
        if (previous != null && previous.equals(componentName.getValue()) && previous2 != null && previous2.equals(endpointType.getValue())) {
            NavigationResult navigationResult = (NavigationResult) attributeMap.get("navigationResult");
            if (navigationResult != null) {
                return navigationResult;
            }
        }

        attributeMap.put("componentName", componentName.getValue());
        attributeMap.put("endpointType", endpointType.getValue());

        // we need to figure out how many options there is so we can as many steps we need
        String camelComponentName = componentName.getValue();

        CamelCatalog catalog = new DefaultCamelCatalog();
        String json = catalog.componentJSonSchema(camelComponentName);
        if (json == null) {
            throw new IllegalArgumentException("Could not find catalog entry for component name: " + camelComponentName);
        }

        // producer vs consumer only if selected
        boolean consumerOnly = false;
        boolean producerOnly = false;
        String type = endpointType.getValue();
        if ("Consumer".equals(type)) {
            consumerOnly = true;
        } else if ("Producer".equals(type)) {
            producerOnly = true;
        }

        List<EndpointOptionByGroup> groups = createUIInputsForCamelComponent(camelComponentName, null, MAX_OPTIONS, consumerOnly, producerOnly, componentFactory, converterFactory);

        // need all inputs in a list as well
        List<InputComponent> allInputs = new ArrayList<>();
        for (EndpointOptionByGroup group : groups) {
            allInputs.addAll(group.getInputs());
        }

        NavigationResultBuilder builder = Results.navigationBuilder();
        int pages = groups.size();
        for (int i = 0; i < pages; i++) {
            boolean last = i == pages - 1;
            EndpointOptionByGroup current = groups.get(i);
            ConfigureEndpointPropertiesStep step = new ConfigureEndpointPropertiesStep(projectFactory, dependencyInstaller,
                    camelComponentName, current.getGroup(), allInputs, current.getInputs(), last, i, pages);
            builder.add(step);
        }

        NavigationResult navigationResult = builder.build();
        attributeMap.put("navigationResult", navigationResult);
        return navigationResult;
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        return Results.success();
    }

}
