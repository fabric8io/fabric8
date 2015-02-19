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
package io.fabric8.forge.camel.commands.jolokia;

import javax.inject.Inject;

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

public class CatalogComponentLabelListCommand extends AbstractJolokiaCommand {

    @Inject
    @WithAttributes(label = "verbose", required = false, defaultValue = "false", description = "Verbose output")
    private UIInput<String> verbose;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(ConnectCommand.class).name(
                "camel-catalog-component-label-list").category(Categories.create(CATEGORY))
                .description("Lists all Camel component labels from the Camel catalog");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(verbose);
    }

    @Override
    public boolean isEnabled(UIContext context) {
        // we dont want this in GUI as it dont add value there
        return !context.getProvider().isGUI() && getJolokiaUrl() != null;
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        String url = getJolokiaUrl();
        if (url == null) {
            return Results.fail("Not connected to remote jolokia agent. Use camel-connect command first");
        }

        boolean val = "true".equals(verbose.getValue());

        org.apache.camel.commands.CatalogComponentLabelListCommand command = new org.apache.camel.commands.CatalogComponentLabelListCommand(val);
        command.execute(getController(), getOutput(context), getError(context));

        return Results.success();
    }
}
