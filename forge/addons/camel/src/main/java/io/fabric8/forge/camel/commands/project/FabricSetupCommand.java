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

import java.util.Arrays;
import java.util.Locale;
import javax.inject.Inject;

import io.fabric8.forge.camel.commands.jolokia.ConnectCommand;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizard;

@FacetConstraint({MavenFacet.class, MavenPluginFacet.class})
public class FabricSetupCommand extends AbstractFabricProjectCommand implements UIWizard {

    private String[] platforms = new String[]{"Docker", "Jube", "Docker-and-Jube"};

    @Inject
    @WithAttributes(label = "platform", required = true, description = "The runtime platform")
    private UISelectOne<String> platform;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(ConnectCommand.class).name(
                "Fabric: Setup").category(Categories.create(CATEGORY))
                .description("Setup Fabric8 in your project");
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        context.getUIContext().getAttributeMap().put("platform", platform.getValue());

        if ("Docker-and-Jube".equals(platform.getValue())) {
            return Results.navigateTo(DockerStepCommand.class);
        } else if ("Docker".equals(platform.getValue())) {
            return Results.navigateTo(DockerStepCommand.class);
        } else {
            return Results.navigateTo(JubeStepCommand.class);
        }
    }

    @Override
    public void initializeUI(final UIBuilder builder) throws Exception {
        builder.add(platform);

        platform.setValueChoices(Arrays.asList(platforms));

        // if windows use jube, otherwise docker
        if (isPlatform("windows")) {
            platform.setDefaultValue("Jube");
        } else {
            platform.setDefaultValue("Docker");
        }
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        return Results.success();
    }

    public static boolean isPlatform(String platform) {
        String osName = System.getProperty("os.name").toLowerCase(Locale.US);
        return osName.contains(platform.toLowerCase(Locale.US));
    }
}
