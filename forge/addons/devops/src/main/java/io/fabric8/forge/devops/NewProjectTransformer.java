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

import org.jboss.forge.addon.projects.ui.NewProjectWizard;
import org.jboss.forge.addon.ui.command.UICommand;
import org.jboss.forge.addon.ui.command.UICommandTransformer;
import org.jboss.forge.addon.ui.context.UIContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transforms the NewProjectWizard to add our new DevOps command step
 */
public class NewProjectTransformer implements UICommandTransformer {
    private static final transient Logger LOG = LoggerFactory.getLogger(NewProjectTransformer.class);

    public NewProjectTransformer() {
    }

    public UICommand transform(UIContext context, UICommand original) {
        if (NewProjectWizard.class.isAssignableFrom(original.getMetadata(context).getType())) {
            return new NewDevopsProjectCommand();
        }
        return original;
    }
}