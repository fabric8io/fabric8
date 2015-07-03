/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.forge.devops;

import org.jboss.forge.addon.projects.ui.NewProjectWizard;
import org.jboss.forge.addon.ui.command.AbstractUICommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
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

/**
 * Extends the NewProjectWizard to add an extra DevOps tab
 */
public class NewDevopsProjectCommand extends AbstractUICommand implements UIWizard {

    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(getClass())
                .category(Categories.create(AbstractOpenShiftCommand.CATEGORY))
                .name(AbstractOpenShiftCommand.CATEGORY + ": Project New")
                .description("Create a new project with DevOps configuration");
    }

	@Override
	public void initializeUI(UIBuilder builder) throws Exception {
	}

	@Override
	public Result execute(UIExecutionContext context) throws Exception {
		return Results.success("Command 'fabric8-new-project' successfully executed!");
	}

	@Override
	public NavigationResult next(UINavigationContext context) throws Exception {
		NavigationResultBuilder builder = NavigationResultBuilder.create();
		builder.add(NewProjectWizard.class);
		builder.add(ConfigureDevOpsStep.class);
		return builder.build();
	}
}