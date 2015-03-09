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

import javax.inject.Inject;

import io.fabric8.forge.camel.commands.jolokia.ConnectCommand;
import org.jboss.forge.addon.maven.plugins.ConfigurationElement;
import org.jboss.forge.addon.maven.plugins.ConfigurationElementBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

public class DockerSetupCommand extends AbstractDockerProjectCommand {

    @Inject
    @WithAttributes(label = "from", required = false, description = "The docker image to use as base line")
    private UIInput<String> from;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(ConnectCommand.class).name(
                "Docker: Setup").category(Categories.create(CATEGORY))
                .description("Setup Docker in your project");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        // TODO: list of known images from we support, and allow to type in image
        builder.add(from);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project project = getSelectedProject(context);

        MavenPluginFacet pluginFacet = project.getFacet(MavenPluginFacet.class);
        MavenPluginBuilder plugin = MavenPluginBuilder.create()
                .setCoordinate(createCoordinate("org.jolokia", "docker-maven-plugin", VersionHelper.dockerVersion()));

        if (pluginFacet.hasPlugin(plugin.getCoordinate())) {
            return Results.success("Docker is already setup");
        }


        ConfigurationElement assembly = ConfigurationElementBuilder.create().setName("assembly").addChild("descriptorRef").setText("artifact-with-dependencies");
        ConfigurationElement from = ConfigurationElementBuilder.create().setName("from").setText("fabric8/tomcat-8.0");

        ConfigurationElement image = ConfigurationElementBuilder.create().setName("image").addChild("name").setText("${docker.registryPrefix}fabric8/${project.artifactId}:${project.version}");
        image.getChildren().add(from);
        image.getChildren().add(assembly);

        ConfigurationElement images = ConfigurationElementBuilder.create().setName("images").addChild(image);

        // TODO: all all properties with docker.env. as prefix as ENV

        /*

        <configuration>
          <images>
            <image>
              <name>${docker.image}</name>
              <build>
                <from>${docker.from}</from>
                <assembly>
                  <descriptorRef>${docker.assemblyDescriptorRef}</descriptorRef>
                </assembly>
                <env>
                  <MAIN>${docker.env.MAIN}</MAIN>
                </env>
              </build>
            </image>
          </images>
        </configuration>
         */

        // add docker-maven-plugin using latest version
        plugin.createConfiguration().addConfigurationElement(images);
        pluginFacet.addPlugin(plugin);

        return Results.success("Added Docker to the project");
    }

}
