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
package io.fabric8.forge.devops;

import io.fabric8.forge.devops.setup.Fabric8SetupStep;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.result.navigation.NavigationResultBuilder;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizard;

public class DevOpsEditCommand extends AbstractDevOpsCommand implements UIWizard {

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(getClass())
                .category(Categories.create(AbstractDevOpsCommand.CATEGORY))
                .name(AbstractDevOpsCommand.CATEGORY + ": Edit")
                .description("Edit the DevOps configuration for this project");
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        NavigationResultBuilder builder = NavigationResultBuilder.create();
        try {
            Project project = getSelectedProject(context.getUIContext());
            if (project != null) {
                builder.add(Fabric8SetupStep.class);
            }
        } catch (IllegalStateException e) {
            // ignore lack of project
        }
        builder.add(DevOpsEditStep.class);
        builder.add(SaveDevOpsStep.class);
        return builder.build();
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        return Results.success();
    }
}
