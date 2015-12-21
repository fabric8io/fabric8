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
import io.fabric8.forge.camel.commands.project.dto.EndpointDto;
import io.fabric8.forge.camel.commands.project.dto.OutputFormat;
import io.fabric8.forge.camel.commands.project.dto.ProjectDto;
import io.fabric8.utils.TablePrinter;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
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

import static io.fabric8.forge.camel.commands.project.helper.OutputFormatHelper.addTableTextOutput;
import static io.fabric8.forge.camel.commands.project.helper.OutputFormatHelper.toJson;

public class CamelGetOverviewCommand extends AbstractCamelProjectCommand {

    @Inject
    @WithAttributes(label = "Format", defaultValue = "Text", description = "Format output as text or json")
    private UISelectOne<OutputFormat> format;

    @Inject
    private DependencyInstaller dependencyInstaller;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(CamelGetOverviewCommand.class).name(
                "Camel: Get Overview").category(Categories.create(CATEGORY))
                .description("Gets the overview of the project from a camel perspective");
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
            return Results.fail("The project does not include camel-core");
        }

        ProjectDto camelProject = new ProjectDto();

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

        camelProject.addEndpoints(javaEndpointsCompleter.getEndpoints());
        camelProject.addEndpoints(xmlEndpointCompleter.getEndpoints());

        String result = formatResult(camelProject);
        return Results.success(result);
    }

    protected String formatResult(ProjectDto result) throws JsonProcessingException {
        OutputFormat outputFormat = format.getValue();
        switch (outputFormat) {
            case JSON:
                return toJson(result);
            default:
                return textResult(result);
        }
    }

    protected String textResult(ProjectDto camelProject) {
        StringBuilder buffer = new StringBuilder();

        List<EndpointDto> endpoints = camelProject.getEndpoints();
        if (!endpoints.isEmpty()) {
            TablePrinter table = new TablePrinter();
            table.columns("uri", "instance name", "file name");
            for (EndpointDto endpoint : endpoints) {
                table.row(endpoint.getEndpointComponentName(), endpoint.getEndpointInstance(), endpoint.getFileName());
            }
            addTableTextOutput(buffer, "Endpoints", table);
        }
        return buffer.toString();
    }

}
