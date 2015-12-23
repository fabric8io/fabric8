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

import java.util.List;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.forge.camel.commands.project.completer.RouteBuilderEndpointsCompleter;
import io.fabric8.forge.camel.commands.project.completer.XmlEndpointsCompleter;
import io.fabric8.forge.camel.commands.project.dto.OutputFormat;
import io.fabric8.forge.camel.commands.project.helper.OutputFormatHelper;
import io.fabric8.forge.camel.commands.project.model.CamelEndpointDetails;
// TODO: Camel 2.16.2
//import org.apache.camel.catalog.EndpointValidationResult;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

public class CamelValidateEndpointCommand extends AbstractCamelProjectCommand {

    @Inject
    @WithAttributes(label = "Format", defaultValue = "Text", description = "Format output as text or json")
    private UISelectOne<OutputFormat> format;

    @Override
    public boolean isEnabled(UIContext context) {
        Project project = getSelectedProjectOrNull(context);
        // only enable if we do not have Camel yet
        if (project == null) {
            // must have a project
            return false;
        } else {
            return true;
        }
    }

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(CamelValidateEndpointCommand.class).name(
                "Camel: Validate Endpoint").category(Categories.create(CATEGORY))
                .description("Validate Camel Endpoints in the project");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(format);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project project = getSelectedProject(context);

        // does the project already have camel?
        Dependency core = findCamelCoreDependency(project);
        if (core == null) {
            return Results.success("Cannot find Apache Camel");
        }

        ResourcesFacet resourcesFacet = project.getFacet(ResourcesFacet.class);
        WebResourcesFacet webResourcesFacet = null;
        if (project.hasFacet(WebResourcesFacet.class)) {
            webResourcesFacet = project.getFacet(WebResourcesFacet.class);
        }

        // use value choices instead of completer as that works better in web console
        XmlEndpointsCompleter xmlEndpointCompleter = new XmlEndpointsCompleter(resourcesFacet, webResourcesFacet);
        JavaSourceFacet javaSourceFacet = project.getFacet(JavaSourceFacet.class);

        // use value choices instead of completer as that works better in web console
        RouteBuilderEndpointsCompleter javaEndpointsCompleter = new RouteBuilderEndpointsCompleter(javaSourceFacet);

        List<CamelEndpointDetails> javaEndpoints = javaEndpointsCompleter.getEndpoints();
        List<CamelEndpointDetails> xmlEndpoints = xmlEndpointCompleter.getEndpoints();

        StringBuilder sb = new StringBuilder();

        boolean failed = false;

        for (CamelEndpointDetails detail : javaEndpoints) {
            String uri = detail.getEndpointUri();
            // TODO: requires Camel 2.16.2+
            // TODO: add detail about where the file is located (maybe we can grab the source code lines +2/-2 and print that)
            //EndpointValidationResult result = getCamelCatalog().validateEndpointProperties(uri);
            // only capture failures
            //if (!result.isSuccess()) {
            //    failed = true;
            //    String msg = formatResult(result);
            //    if (msg != null) {
            //        sb.append(msg);
            //    }
            //}
        }
        for (CamelEndpointDetails detail : xmlEndpoints) {
            String uri = detail.getEndpointUri();
            // TODO: requires Camel 2.16.2+
            // TODO: add detail about where the file is located  (maybe we can grab the source code lines +2/-2 and print that)
            //EndpointValidationResult result = getCamelCatalog().validateEndpointProperties(uri);
            // only capture failures
            //if (!result.isSuccess()) {
            //    failed = true;
            //    String msg = formatResult(result);
            //    if (msg != null) {
            //        sb.append(msg);
            //    }
            //}
        }

        if (failed) {
            return Results.fail("Camel endpoint validation failed:\n" + sb.toString());
        } else {
            return Results.success("Camel endpoint validation success");
        }
    }

    /*protected String formatResult(EndpointValidationResult results) throws JsonProcessingException {
        OutputFormat outputFormat = format.getValue();
        switch (outputFormat) {
            case JSON:
                return OutputFormatHelper.toJson(results);
            default:
                return results.summaryErrorMessage();
        }
    }*/

}
