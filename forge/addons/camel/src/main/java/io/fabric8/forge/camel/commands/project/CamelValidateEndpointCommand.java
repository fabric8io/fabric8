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

import io.fabric8.forge.camel.commands.project.completer.RouteBuilderEndpointsCompleter;
import io.fabric8.forge.camel.commands.project.completer.XmlEndpointsCompleter;
import io.fabric8.forge.camel.commands.project.model.CamelEndpointDetails;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

public class CamelValidateEndpointCommand extends AbstractCamelProjectCommand {

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

        int count = 0;

        for (CamelEndpointDetails detail : javaEndpoints) {
            String uri = detail.getEndpointUri();
            // TODO: requires Camel 2.16.2+
            // TODO: add detail about where the file is located (maybe we can grab the source code lines +2/-2 and print that)
            String msg = null;
            //String msg = getCamelCatalog().validateProperties(uri).summaryErrorMessage();
            //if (msg != null) {
            //    sb.append(msg);
            //}
            count++;
        }
        for (CamelEndpointDetails detail : xmlEndpoints) {
            String uri = detail.getEndpointUri();
            // TODO: requires Camel 2.16.2+
            // TODO: add detail about where the file is located  (maybe we can grab the source code lines +2/-2 and print that)
            String msg = null;
            //String msg = getCamelCatalog().validateProperties(uri).summaryErrorMessage();
            //if (msg != null) {
            //    sb.append(msg);
            //}
            count++;
        }

        if (sb.length() > 0) {
            return Results.fail("Camel endpoint validation failed\n" + sb.toString());
        } else {
            return Results.success("Camel endpoint validation success");
        }
    }

}
