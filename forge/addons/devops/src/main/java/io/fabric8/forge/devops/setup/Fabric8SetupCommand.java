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
package io.fabric8.forge.devops.setup;

import java.util.Arrays;
import java.util.Locale;
import javax.inject.Inject;

import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
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

@FacetConstraint({MavenFacet.class, MavenPluginFacet.class, ResourcesFacet.class})
public class Fabric8SetupCommand extends AbstractFabricProjectCommand implements UIWizard {

    // TODO: Jube does not currently work so disable jube until working again
    // private String[] platforms = new String[]{"Docker", "Jube", "Docker-and-Jube"};
    private String[] platforms = new String[]{"Docker"};

    @Inject
    @WithAttributes(label = "platform", required = true, description = "The runtime platform")
    private UISelectOne<String> platform;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(Fabric8SetupCommand.class).name(
                "Fabric8: Setup").category(Categories.create(CATEGORY))
                .description("Setup Fabric8 and Docker in your project");
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        context.getUIContext().getAttributeMap().put("platform", platform.getValue());
        return Results.navigateTo(Fabric8SetupStep.class);
    }

    @Override
    public void initializeUI(final UIBuilder builder) throws Exception {
        builder.add(platform);

        platform.setValueChoices(Arrays.asList(platforms));

        // TODO: Jube does not currently work so disable jube until working again
        platform.setDefaultValue("Docker");
        // if windows use jube, otherwise docker
        //if (isPlatform("windows")) {
        //    platform.setDefaultValue("Jube");
        //} else {
        //    platform.setDefaultValue("Docker");
        //}
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
