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

import static io.fabric8.forge.camel.commands.project.helper.CamelCatalogHelper.endpointComponentName;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.JSonSchemaHelper;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;
import org.jboss.forge.roaster.model.util.Strings;

public class ShowEndpointPropertiesStep extends AbstractCamelProjectCommand implements UIWizardStep {

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(ShowEndpointPropertiesStep.class).name(
                "Camel: Edit Endpoint XML").category(Categories.create(CATEGORY))
                .description("Configure the endpoint options to use");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initializeUI(UIBuilder builder) throws Exception {
    	//noop
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        return null;
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();
        CamelCatalog catalog = new DefaultCamelCatalog();

        String camelComponentName = optionalAttributeValue(attributeMap, "componentName");
        String uri = mandatoryAttributeValue(attributeMap, "endpointUri");

        if (camelComponentName == null && uri != null) {
            camelComponentName = endpointComponentName(uri);
        }

        String json = catalog.componentJSonSchema(camelComponentName);
        if (json == null) {
            throw new IllegalArgumentException("Could not find catalog entry for component name: " + camelComponentName);
        }

        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("properties", json, true);
        Map<String, String> currentValues = uri != null ? catalog.endpointProperties(uri) : Collections.EMPTY_MAP;
        PrintStream out = getOutput(context);
        
        out.println("");
        out.println("Camel Endpoint Options for " + uri);
        out.println("=========================");
        if (data != null) {
            for (Map<String, String> propertyMap : data) {
            	if (currentValues.get(propertyMap.get("name")) != null) {
            		if (propertyMap.get("name") != null) {
	            	    out.print("Name: " + propertyMap.get("name"));
            		} else {
            		    out.print("Name: -");
            		}
	                out.print(" ");
	                out.println("");
	                if (propertyMap.get("kind") != null) {
	            	    out.print("Kind: " + propertyMap.get("kind"));
	                } else {
	                    out.print("Kind: -");
	                }
	                out.print(" ");
	                out.println("");
	                if (propertyMap.get("type") != null) {
	            	    out.print("Type: " + propertyMap.get("type"));
	                } else {
	                    out.print("Type: -");
	                }
	                out.print(" ");
	                out.println("");
	                if (propertyMap.get("javaType") != null) {
	            	    out.print("Java Type: " + propertyMap.get("javaType"));
	                } else {
	                    out.print("Java Type: -");
	                }
	                out.print(" ");
	                out.println("");
	                if (currentValues.get(propertyMap.get("name")) != null) {
	            	    out.print("Current value: " + currentValues.get(propertyMap.get("name")));
	                } else {
	                    out.print("Current value: -");
	                }
	                out.print(" ");
	                out.println("");
	                if (propertyMap.get("description") != null) {
	            	    out.print("Description: " + propertyMap.get("description"));
	                } else {
	                    out.print("Description: -");
	                }
	                out.print(" ");
	                out.println("");
	                if (propertyMap.get("enum") != null) {
	            	    out.print("Enumeration: " + propertyMap.get("enum"));
	                } else {
	                    out.print("Enumeration: -");
	                }
	                out.print(" ");
	                out.println("");
            	}
            	out.println("");
            }
        }
        return Results.success();
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
