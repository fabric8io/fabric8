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

import org.apache.camel.commands.jolokia.NoopStringEscape;
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

public class RouteInfoCommand extends AbstractJolokiaCommand {

    @Inject
    @WithAttributes(label = "name", required = true, description = "The name of the Camel context")
    private UIInput<String> name;

    @Inject
    @WithAttributes(label = "route", required = true, description = "The id of the route")
    private UIInput<String> route;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(ConnectCommand.class).name(
                "camel-route-info").category(Categories.create(CATEGORY))
                .description("Display information about a Camel route.");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        name.setCompleter(new CamelContextCompleter(getController()));
        route.setCompleter(new RouteCompleter(getController()));
        builder.add(name).add(route);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        String url = getJolokiaUrl();
        if (url == null) {
            return Results.fail("Not connected to remote jolokia agent. Use camel-connect command first");
        }

        org.apache.camel.commands.RouteInfoCommand command = new org.apache.camel.commands.RouteInfoCommand(route.getValue(), name.getValue());
        command.setStringEscape(new NoopStringEscape());

        command.execute(getController(), getOutput(context), getError(context));
        return Results.success();
    }
}
