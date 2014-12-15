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

public class ConnectCommand extends AbstractJolokiaCommand {

    @Inject
    @WithAttributes(label = "Url", required = true,
            description = "url to remote jolokia agent",
            requiredMessage = "You must provide an url to connect to the remote jolokia agent")
    private UIInput<String> url;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(ConnectCommand.class).name(
                "camel-connect").category(Categories.create(CATEGORY))
                .description("Connects to a Jolokia agent");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(url);
    }

    @Override
    public Result execute(UIExecutionContext uiExecutionContext) throws Exception {
        configuration.setProperty("CamelJolokiaUrl", url.getValue());
        return Results.success("Connected to " + url.getValue());
    }
}
