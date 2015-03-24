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
package io.fabric8.forge.camel.commands.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import io.fabric8.forge.camel.commands.project.helper.CamelCommandsHelper;
import io.fabric8.forge.camel.commands.project.helper.CamelProjectHelper;
import io.fabric8.forge.camel.commands.project.helper.LineNumberHelper;
import io.fabric8.forge.camel.commands.project.helper.XmlLineNumberParser;
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
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.InputComponentFactory;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jboss.forge.roaster.model.util.Strings;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static io.fabric8.forge.camel.commands.project.helper.CamelCommandsHelper.ensureCamelArtifactIdAdded;
import static io.fabric8.forge.camel.commands.project.helper.CamelCommandsHelper.loadCamelComponentDetails;
import static io.fabric8.forge.camel.commands.project.helper.UIHelper.createUIInput;

public class ConfigureEndpointPropertiesStep extends AbstractCamelProjectCommand implements UIWizardStep {

    @Inject
    private InputComponentFactory componentFactory;

    @Inject
    private DependencyInstaller dependencyInstaller;

    @Inject
    private DependencyResolver dependencyResolver;

    private List<InputComponent> inputs = new ArrayList<>();

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        // lets create a field for each property on the component

        Map<Object, Object> attributeMap = builder.getUIContext().getAttributeMap();
        String camelComponentName = mandatoryAttributeValue(attributeMap, "componentName");
        CamelCatalog catalog = new DefaultCamelCatalog();
        String json = catalog.componentJSonSchema(camelComponentName);
        if (json == null) {
            throw new IllegalArgumentException("Could not find catalog entry for component name: " + camelComponentName);
        }

        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("properties", json, true);
        if (data != null) {
            Set<String> namesAdded = new HashSet<>();
            for (Map<String, String> propertyMap : data) {
                String name = propertyMap.get("name");
                String kind = propertyMap.get("kind");
                String type = propertyMap.get("type");
                String javaType = propertyMap.get("javaType");
                String deprecated = propertyMap.get("deprecated");
                String required = propertyMap.get("required");
                String defaultValue = propertyMap.get("defaultValue");
                String description = propertyMap.get("description");
                String enums = propertyMap.get("enum");

                if (!Strings.isNullOrEmpty(name)) {
                    Class<Object> inputClazz = CamelCommandsHelper.loadValidInputTypes(javaType, type);
                    if (inputClazz != null) {
                        if (namesAdded.add(name)) {
                            InputComponent input = createUIInput(componentFactory, name, inputClazz, required, defaultValue, enums, description);
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
            // only use the value if a value was set
            if (input.hasValue()) {
                String value = input.getValue().toString();
                if (value != null) {
                    options.put(key, value);
                }
            }
        }

        // TODO: require Camel 2.15.1
            /*CamelCatalog catalog = new DefaultCamelCatalog();
            String uri = catalog.asEndpointUri(camelComponentName, options);
            if (uri == null) {
                return Results.fail("Cannot create endpoint uri");
            }*/
        String uri = "We need Camel 2.15.1";

        JavaResource existing = facet.getJavaResource(routeBuilder);
        if (existing == null || !existing.exists()) {
            return Results.fail("RouteBuilder " + routeBuilder + " does not exist");
        }

        JavaClassSource clazz = existing.getJavaType();
        MethodSource configure = clazz.getMethod("configure");
        String body = configure.getBody();

        // make sure to import the Camel endpoint
        clazz.addImport("org.apache.camel.Endpoint");

        // insert the endpoint code
        StringBuilder sb = new StringBuilder(body);
        String line = String.format("Endpoint %s = endpoint(\"%s\");\n\n", endpointInstanceName, uri);
        sb.insert(0, line);
        body = sb.toString();

        // set the updated body
        configure.setBody(body);

        // update source code
        facet.saveJavaSource(clazz);

        return Results.success("Added endpoint " + endpointInstanceName + " to " + routeBuilder);
    }

    protected Result executeXml(UIExecutionContext context, Map<Object, Object> attributeMap) throws Exception {
        String camelComponentName = mandatoryAttributeValue(attributeMap, "componentName");
        String endpointInstanceName = mandatoryAttributeValue(attributeMap, "instanceName");
        String xml = mandatoryAttributeValue(attributeMap, "xml");

        Project project = getSelectedProject(context);
        ResourcesFacet facet = project.getFacet(ResourcesFacet.class);

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
            // only use the value if a value was set
            if (input.hasValue()) {
                String value = input.getValue().toString();
                if (value != null) {
                    options.put(key, value);
                }
            }
        }

        // TODO: require Camel 2.15.1
            /*CamelCatalog catalog = new DefaultCamelCatalog();
            String uri = catalog.asEndpointUri(camelComponentName, options);
            if (uri == null) {
                return Results.fail("Cannot create endpoint uri");
            }*/
        String uri = "We need Camel 2.15.1";

        FileResource file = facet.getResource(xml);
        if (!file.exists()) {
            return Results.fail("Cannot find XML file " + xml);
        }

        Document root = XmlLineNumberParser.parseXml(file.getResourceInputStream());

        String lineNumber;
        String columnNumber;

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
                if (found == null) {
                    created = true;
                    found = insertEndpointBefore(camel);
                    if (found == null) {
                        // empty so use <camelContext> node
                        found = camelContext;
                        extraSpaces = 2;
                    }
                }

                if (found == null) {
                    return Results.fail("Cannot find <camelContext> in XML file " + xml);
                }

                lineNumber = (String) found.getUserData(XmlLineNumberParser.LINE_NUMBER);

                // if we created a new endpoint, then insert a new line with the endpoint details
                if (created) {
                    List<String> lines = LineNumberHelper.readLines(file.getResourceInputStream());
                    String line = String.format("<endpoint id=\"%s\" uri=\"%s\"/>", endpointInstanceName, uri);

                    // the list is 0-based, and line number is 1-based
                    int idx = lineNumber != null ? Integer.valueOf(lineNumber) - 1 : 0;
                    int spaces = LineNumberHelper.leadingSpaces(lines, idx) + extraSpaces;
                    line = LineNumberHelper.padString(line, spaces);
                    // insert after
                    lines.add(idx + 1, line);

                    // and save the file back
                    String content = LineNumberHelper.linesToString(lines);
                    file.setContents(content);

                    return Results.success("Added endpoint: " + endpointInstanceName + " with uri: " + uri);
                } else {
                    // update existing
                    List<String> lines = LineNumberHelper.readLines(file.getResourceInputStream());
                    String line = String.format("<endpoint id=\"%s\" uri=\"%s\"/>", endpointInstanceName, uri);

                    // the list is 0-based, and line number is 1-based
                    int idx = lineNumber != null ? Integer.valueOf(lineNumber) - 1 : 0;
                    int spaces = LineNumberHelper.leadingSpaces(lines, idx) + extraSpaces;
                    line = LineNumberHelper.padString(line, spaces);
                    lines.set(idx, line);

                    // and save the file back
                    String content = LineNumberHelper.linesToString(lines);
                    file.setContents(content);

                    return Results.success("Update endpoint: " + endpointInstanceName + " with uri: " + uri);
                }
            }

            return Results.fail("Cannot find <camelContext> in XML file " + xml);
        } else {
            return Results.fail("Cannot parse XML file " + xml);
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
}
