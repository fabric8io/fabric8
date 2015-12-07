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

import org.apache.camel.commands.jolokia.JolokiaCamelController;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

public class ConnectedCommand extends AbstractJolokiaCommand {

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(ConnectedCommand.class).name(
                "camel-connected").category(Categories.create(CATEGORY))
                .description("Checks the connection to the Jolokia agent");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        // noop
    }

    @Override
    public Result execute(UIExecutionContext uiExecutionContext) throws Exception {
        String url = getJolokiaUrl();
        if (url == null) {
            return Results.fail("Not connected to remote jolokia agent. Use camel-connect command first");
        }

        String username = configuration.getString("CamelJolokiaUsername");

        JolokiaCamelController controller = getController();

        // ping to see if the connection works
        boolean ok = controller.ping();
        if (ok) {
            return Results.success("Connected to " + url + (username != null ? " using " + username : ""));
        } else {
            return Results.fail("Error connecting to " + url);
        }
    }
}
