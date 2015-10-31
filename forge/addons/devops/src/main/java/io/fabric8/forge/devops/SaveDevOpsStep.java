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

import io.fabric8.devops.ProjectConfig;
import io.fabric8.devops.ProjectConfigs;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.util.ResourceUtil;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

public class SaveDevOpsStep extends AbstractDevOpsCommand implements UIWizardStep {
    private static final transient Logger LOG = LoggerFactory.getLogger(SaveDevOpsStep.class);

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(getClass())
                .category(Categories.create(AbstractDevOpsCommand.CATEGORY))
                .name(AbstractDevOpsCommand.CATEGORY + ": Save")
                .description("Saves the DevOps options");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        // noop
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        return null;
    }

    public static File getProjectConfigFile(Project project) {
        if (project == null) {
            return null;
        }
        Resource<?> root = project.getRoot();
        if (root == null) {
            return null;
        }
        Resource<?> configFileResource = root.getChild(ProjectConfigs.FILE_NAME);
        if (configFileResource == null) {
            return null;
        }
        return ResourceUtil.getContextFile(configFileResource);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        String fileName = ProjectConfigs.FILE_NAME;
        File configFile = getProjectConfigFile(getSelectedProject(context));
        if (configFile == null) {
            return Results.fail("This command requires a project");
        }
        ProjectConfig config = null;
        boolean hasFile = false;
        if (configFile.exists()) {
            config = ProjectConfigs.parseProjectConfig(configFile);
            hasFile = true;
        }
        if (config == null) {
            config = new ProjectConfig();
        }

        updateConfiguration(context, config);
        LOG.info("Result: " + config);

        if (config.isEmpty() && !hasFile) {
            return Results.success("No " + fileName + " need be generated as there is no configuration");
        } else {
            String message = "Updated";
            if (!configFile.exists()) {
                message = "Created";
            }
            ProjectConfigs.saveConfig(config, configFile);
            return Results.success(message + " " + fileName);
        }
    }

    protected void updateConfiguration(UIExecutionContext context, ProjectConfig config) {
        Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();
        ProjectConfigs.configureProperties(config, attributeMap);
    }

}
