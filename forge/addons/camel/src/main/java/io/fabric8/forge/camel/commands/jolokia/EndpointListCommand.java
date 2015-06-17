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

public class EndpointListCommand extends AbstractJolokiaCommand {

    @Inject
    @WithAttributes(label = "name", required = true, description = "The name of the Camel context")
    private UIInput<String> name;

    @Inject
    @WithAttributes(label = "decode", required = false, defaultValue = "true", description = "Whether to decode the endpoint uri so its human readable")
    private UIInput<String> decode;

    @Inject
    @WithAttributes(label = "explain", required = false, defaultValue = "false", description = "Whether to explain the endpoint options")
    private UIInput<String> explain;

    @Inject
    @WithAttributes(label = "verbose", required = false, defaultValue = "false", description = "Verbose output")
    private UIInput<String> verbose;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(ConnectCommand.class).name(
                "camel-endpoint-list").category(Categories.create(CATEGORY))
                .description("List all endpoints available in a CamelContext");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        name.setCompleter(new CamelContextCompleter(getController()));
        builder.add(name).add(decode).add(explain).add(verbose);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        String url = getJolokiaUrl();
        if (url == null) {
            return Results.fail("Not connected to remote jolokia agent. Use camel-connect command first");
        }

        boolean val = "true".equals(decode.getValue());
        boolean val2 = "true".equals(verbose.getValue());
        boolean val3 = "true".equals(explain.getValue());

        org.apache.camel.commands.EndpointListCommand command = new org.apache.camel.commands.EndpointListCommand(name.getValue(), val, val2, val3);
        command.execute(getController(), getOutput(context), getError(context));

        return Results.success();
    }
}
