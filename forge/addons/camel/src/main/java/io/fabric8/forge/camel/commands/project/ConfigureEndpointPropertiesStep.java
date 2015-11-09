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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import io.fabric8.forge.addon.utils.CamelProjectHelper;
import io.fabric8.forge.addon.utils.LineNumberHelper;
import io.fabric8.forge.addon.utils.XmlLineNumberParser;
import io.fabric8.forge.camel.commands.project.helper.CamelCommandsHelper;
import io.fabric8.forge.camel.commands.project.helper.StringHelper;
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
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.resource.FileResource;
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
import org.jboss.forge.roaster.model.source.AnnotationSource;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.util.Strings;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static io.fabric8.forge.addon.utils.CamelProjectHelper.findCamelArtifactDependency;
import static io.fabric8.forge.addon.utils.UIHelper.createUIInput;
import static io.fabric8.forge.camel.commands.project.helper.CamelCatalogHelper.endpointComponentName;
import static io.fabric8.forge.camel.commands.project.helper.CamelCatalogHelper.isDefaultValue;
import static io.fabric8.forge.camel.commands.project.helper.CamelCommandsHelper.ensureCamelArtifactIdAdded;
import static io.fabric8.forge.camel.commands.project.helper.CamelCommandsHelper.loadCamelComponentDetails;

public class ConfigureEndpointPropertiesStep extends AbstractCamelProjectCommand implements UIWizardStep {

    @Inject
    private InputComponentFactory componentFactory;

    @Inject
    private DependencyInstaller dependencyInstaller;

    @Inject
    private DependencyResolver dependencyResolver;

    private List<InputComponent> inputs = new ArrayList<>();

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(ConfigureEndpointPropertiesStep.class).name(
                "Camel: Endpoint options").category(Categories.create(CATEGORY))
                .description("Configure the endpoint options to use");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initializeUI(UIBuilder builder) throws Exception {
        // lets create a field for each property on the component

        CamelCatalog catalog = new DefaultCamelCatalog();

        Map<Object, Object> attributeMap = builder.getUIContext().getAttributeMap();

        // either we have an uri from an existing endpoint to edit, or we only have the component name to create a new endpoint
        String camelComponentName = optionalAttributeValue(attributeMap, "componentName");
        String uri = optionalAttributeValue(attributeMap, "endpointUri");

        if (camelComponentName == null && uri != null) {
            camelComponentName = endpointComponentName(uri);
        }

        String json = catalog.componentJSonSchema(camelComponentName);
        if (json == null) {
            throw new IllegalArgumentException("Could not find catalog entry for component name: " + camelComponentName);
        }

        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("properties", json, true);
        Map<String, String> currentValues = uri != null ? catalog.endpointProperties(uri) : Collections.EMPTY_MAP;

        if (data != null) {
            Set<String> namesAdded = new HashSet<>();
            for (Map<String, String> propertyMap : data) {
                String name = propertyMap.get("name");
                String kind = propertyMap.get("kind");
                String type = propertyMap.get("type");
                String javaType = propertyMap.get("javaType");
                String deprecated = propertyMap.get("deprecated");
                String required = propertyMap.get("required");
                String currentValue = currentValues.get(name);
                String defaultValue = propertyMap.get("defaultValue");
                String description = propertyMap.get("description");
                String enums = propertyMap.get("enum");

                if (!Strings.isNullOrEmpty(name)) {
                    Class<Object> inputClazz = CamelCommandsHelper.loadValidInputTypes(javaType, type);
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
        String kind = mandatoryAttributeValue(attributeMap, "kind");
        if ("xml".equals(kind)) {
            return executeXml(context, attributeMap);
        } else {
            return executeJava(context, attributeMap);
        }
    }

    protected Result executeXml(UIExecutionContext context, Map<Object, Object> attributeMap) throws Exception {
        String camelComponentName = optionalAttributeValue(attributeMap, "componentName");
        String endpointInstanceName = optionalAttributeValue(attributeMap, "instanceName");
        String mode = mandatoryAttributeValue(attributeMap, "mode");
        String xml = mandatoryAttributeValue(attributeMap, "xml");

        // edit mode includes the existing uri and line number
        String lineNumber = null;
        String endpointUrl = null;
        if ("edit".equals(mode)) {
            lineNumber = mandatoryAttributeValue(attributeMap, "lineNumber");
            endpointUrl = mandatoryAttributeValue(attributeMap, "endpointUri");

            // since this is XML we need to escape & as &amp;
            // to be safe that & is not already &amp; we need to revert it first
            endpointUrl = StringHelper.replaceAll(endpointUrl, "&amp;", "&");
            endpointUrl = StringHelper.replaceAll(endpointUrl, "&", "&amp;");
            endpointUrl = StringHelper.replaceAll(endpointUrl, "<", "&lt;");
            endpointUrl = StringHelper.replaceAll(endpointUrl, ">", "&gt;");
        }

        Project project = getSelectedProject(context);
        ResourcesFacet facet = project.getFacet(ResourcesFacet.class);
        WebResourcesFacet webResourcesFacet = null;
        if (project.hasFacet(WebResourcesFacet.class)) {
            webResourcesFacet = project.getFacet(WebResourcesFacet.class);
        }

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
        // and make sure its dependency is added
        result = ensureCamelArtifactIdAdded(project, details, dependencyInstaller);
        if (result != null) {
            return result;
        }

        // collect all the options that was set
        Map<String, String> options = new HashMap<String, String>();
        for (InputComponent input : inputs) {
            String key = input.getName();
            // only use the value if a value was set (and the value is not the same as the default value)
            if (input.hasValue()) {
                String value = input.getValue().toString();
                if (value != null) {
                    // do not add the value if it match the default value
                    boolean matchDefault = isDefaultValue(camelComponentName, key, value);
                    if (!matchDefault) {
                        options.put(key, value);
                    }
                }
            } else if (input.isRequired() && input.hasDefaultValue()) {
                // if its required then we need to grab the value
                String value = input.getValue().toString();
                if (value != null) {
                    options.put(key, value);
                }
            }
        }

        CamelCatalog catalog = new DefaultCamelCatalog();
        String uri = catalog.asEndpointUriXml(camelComponentName, options);
        if (uri == null) {
            return Results.fail("Cannot create endpoint uri");
        }
        // TODO: Remove this when upgrading to Camel 2.16.1
        // we do not want to use %23 for # syntax
        uri = uri.replaceAll("\\=\\%23", "=#");

        FileResource file = facet != null ? facet.getResource(xml) : null;
        if (file == null || !file.exists()) {
            file = webResourcesFacet != null ? webResourcesFacet.getWebResource(xml) : null;
        }
        if (file == null || !file.exists()) {
            return Results.fail("Cannot find XML file " + xml);
        }

        // if we have a line number then use that to edit the existing value
        if (lineNumber != null) {
            List<String> lines = LineNumberHelper.readLines(file.getResourceInputStream());
            return editEndpointXml(lines, lineNumber, endpointUrl, uri, file, xml);
        } else {
            // we are in add mode, so parse dom to find <camelContext> and insert the endpoint where its needed
            Document root = XmlLineNumberParser.parseXml(file.getResourceInputStream());
            return addEndpointXml(root, endpointInstanceName, uri, file, xml);
        }
    }

    private Result editEndpointXml(List<String> lines, String lineNumber, String endpointUrl, String uri, FileResource file, String xml) {
        // the list is 0-based, and line number is 1-based
        int idx = Integer.valueOf(lineNumber) - 1;
        String line = lines.get(idx);

        // replace uri with new value
        line = StringHelper.replaceAll(line, endpointUrl, uri);
        lines.set(idx, line);

        // and save the file back
        String content = LineNumberHelper.linesToString(lines);
        file.setContents(content);

        return Results.success("Update endpoint uri: " + uri + " in file " + xml);
    }

    private Result addEndpointXml(Document root, String endpointInstanceName, String uri, FileResource file, String xml) throws Exception {
        String lineNumber;

        // The DOM api is so fucking terrible!
        if (root != null) {
            NodeList camels = root.getElementsByTagName("camelContext");
            // TODO: what about 2+ camel's ?
            if (camels != null && camels.getLength() == 1) {
                Node camel = camels.item(0);
                Node camelContext = null;
                boolean created = false;

                // find existing by id
                Node found = null;
                for (int i = 0; i < camel.getChildNodes().getLength(); i++) {
                    if ("camelContext".equals(camel.getNodeName())) {
                        camelContext = camel;
                    }

                    Node child = camel.getChildNodes().item(i);
                    if ("camelContext".equals(child.getNodeName())) {
                        camelContext = child;
                    }
                    if ("endpoint".equals(child.getNodeName())) {
                        // okay its an endpoint so if we can match by id attribute
                        String id = child.getAttributes().getNamedItem("id").getNodeValue();
                        if (endpointInstanceName.equals(id)) {
                            found = child;
                            break;
                        }
                    }
                }

                int extraSpaces = 0;
                int extraLines = 0;
                if (found == null) {
                    created = true;
                    found = insertEndpointBefore(camel);
                    if (found == null) {
                        // empty so use <camelContext> node
                        found = camelContext;
                        extraSpaces = 2;
                        extraLines = 1;
                    }
                }

                if (found == null) {
                    return Results.fail("Cannot find <camelContext> in XML file " + xml);
                }

                lineNumber = (String) found.getUserData(XmlLineNumberParser.LINE_NUMBER);

                // if we created a new endpoint, then insert a new line with the endpoint details
                List<String> lines = LineNumberHelper.readLines(file.getResourceInputStream());
                String line = String.format("<endpoint id=\"%s\" uri=\"%s\"/>", endpointInstanceName, uri);

                // the list is 0-based, and line number is 1-based
                int idx = lineNumber != null ? Integer.valueOf(lineNumber) - 1 : 0;
                idx += extraLines;
                int spaces = LineNumberHelper.leadingSpaces(lines, idx) + extraSpaces;
                line = LineNumberHelper.padString(line, spaces);
                if (created) {
                    lines.add(idx, line);
                } else {
                    lines.set(idx, line);
                }

                // and save the file back
                String content = LineNumberHelper.linesToString(lines);
                file.setContents(content);
            }
        }

        return Results.success("Added endpoint uri: " + uri + " in XML file " + xml);
    }

    protected Result executeJava(UIExecutionContext context, Map<Object, Object> attributeMap) throws Exception {
        String camelComponentName = mandatoryAttributeValue(attributeMap, "componentName");
        String endpointInstanceName = mandatoryAttributeValue(attributeMap, "instanceName");
        String routeBuilder = mandatoryAttributeValue(attributeMap, "routeBuilder");

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
        // and make sure its dependency is added
        result = ensureCamelArtifactIdAdded(project, details, dependencyInstaller);
        if (result != null) {
            return result;
        }

        // collect all the options that was set
        Map<String, String> options = new HashMap<String, String>();
        for (InputComponent input : inputs) {
            String key = input.getName();
            // only use the value if a value was set (and the value is not the same as the default value)
            if (input.hasValue()) {
                String value = input.getValue().toString();
                if (value != null) {
                    // do not add the value if it match the default value
                    boolean matchDefault = isDefaultValue(camelComponentName, key, value);
                    if (!matchDefault) {
                        options.put(key, value);
                    }
                }
            } else if (input.isRequired() && input.hasDefaultValue()) {
                // if its required then we need to grab the value
                String value = input.getValue().toString();
                if (value != null) {
                    options.put(key, value);
                }
            }
        }

        CamelCatalog catalog = new DefaultCamelCatalog();
        String uri = catalog.asEndpointUri(camelComponentName, options);
        if (uri == null) {
            return Results.fail("Cannot create endpoint uri");
        }
        // TODO: Remove this when upgrading to Camel 2.16.1
        // we do not want to use %23 for # syntax
        uri = uri.replaceAll("\\=\\%23", "=#");

        JavaResource existing = facet.getJavaResource(routeBuilder);
        if (existing == null || !existing.exists()) {
            return Results.fail("RouteBuilder " + routeBuilder + " does not exist");
        }

        JavaClassSource clazz = existing.getJavaType();

        // add the endpoint as a field
        // special for CDI as we use different set of annotations
        boolean updated = true;
        boolean cdi = findCamelArtifactDependency(project, "camel-cdi") != null;

        FieldSource field = clazz.getField(endpointInstanceName);
        AnnotationSource annotation;
        if (field == null) {
            field = clazz.addField();
            field.setName(endpointInstanceName);
            field.setType("org.apache.camel.Endpoint");
            field.setPrivate();
            updated = false;
        }
        if (cdi) {
            annotation = field.getAnnotation("org.apache.camel.cdi.Uri");
            if (annotation == null) {
                if (!field.hasAnnotation("javax.inject.Inject")) {
                    field.addAnnotation("javax.inject.Inject");
                }
                annotation = field.addAnnotation("org.apache.camel.cdi.Uri");
            }
        } else {
            annotation = field.getAnnotation("org.apache.camel.EndpointInject");
        }
        annotation.removeAllValues();
        annotation.setStringValue(uri);

        // make sure to import what we use
        clazz.addImport("org.apache.camel.Endpoint");
        if (cdi) {
            clazz.addImport("javax.inject.Inject");
            clazz.addImport("org.apache.camel.cdi.Uri");
        } else {
            clazz.addImport("org.apache.camel.EndpointInject");
        }

        facet.saveJavaSource(clazz);

        if (updated) {
            return Results.success("Updated endpoint " + endpointInstanceName + " in " + routeBuilder);
        } else {
            return Results.success("Added endpoint " + endpointInstanceName + " in " + routeBuilder);
        }
    }

    /**
     * To find the closet node that we need to insert the endpoints before, so the Camel schema is valid.
     */
    private Node insertEndpointBefore(Node camel) {
        // if there is endpoints then the cut-off is after the last
        Node endpoint = null;
        for (int i = 0; i < camel.getChildNodes().getLength(); i++) {
            Node found = camel.getChildNodes().item(i);
            String name = found.getNodeName();
            if ("endpoint".equals(name)) {
                endpoint = found;
            }
        }
        if (endpoint != null) {
            return endpoint;
        }

        Node last = null;
        // if no endpoints then try to find cut-off according the XSD rules
        for (int i = 0; i < camel.getChildNodes().getLength(); i++) {
            Node found = camel.getChildNodes().item(i);
            String name = found.getNodeName();
            if ("dataFormats".equals(name) || "redeliveryPolicyProfile".equals(name)
                    || "onException".equals(name) || "onCompletion".equals(name)
                    || "intercept".equals(name) || "interceptFrom".equals(name)
                    || "interceptSendToEndpoint".equals(name) || "restConfiguration".equals(name)
                    || "rest".equals(name) || "route".equals(name)) {
                return found;
            }
            if (found.getNodeType() == Node.ELEMENT_NODE) {
                last = found;
            }
        }

        return last;
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

    /**
     * Returns the optional String value of the given name
     */
    public static String optionalAttributeValue(Map<Object, Object> attributeMap, String name) {
        Object value = attributeMap.get(name);
        if (value != null) {
            String text = value.toString();
            if (!Strings.isBlank(text)) {
                return text;
            }
        }
        return null;
    }
}
