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

import java.io.PrintStream;
import javax.inject.Inject;

import org.apache.camel.commands.jolokia.DefaultJolokiaCamelController;
import org.apache.camel.commands.jolokia.JolokiaCamelController;
import org.jboss.forge.addon.configuration.Configuration;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;

/**
 * Base class for all Jolokia Camel commands.
 */
public abstract class AbstractJolokiaCommand extends AbstractProjectCommand {

    public static String CATEGORY = "Camel";
    public static String CATEGORY_CATALOG = "CamelCatalog";

    @Inject
    protected ProjectFactory projectFactory; // helper to integrate with the filesystem

    @Inject
    protected Configuration configuration;

    @Override
    protected boolean isProjectRequired() {
        return false;
    }

    @Override
    public boolean isEnabled(UIContext context) {
        // all the jolokia commands is for CLI
        return super.isEnabled(context) && !context.getProvider().isGUI() && getJolokiaUrl() != null;
    }

    @Override
    protected ProjectFactory getProjectFactory() {
        return projectFactory;
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
    }

    protected String getJolokiaUrl() {
        return configuration.getString("CamelJolokiaUrl");
    }

    protected JolokiaCamelController getController() throws Exception {
        JolokiaCamelController controller = new DefaultJolokiaCamelController();

        // optional
        String username = configuration.getString("CamelJolokiaUsername");
        String password = configuration.getString("CamelJolokiaPassword");

        controller.connect(getJolokiaUrl(), username, password);
        return controller;
    }

    protected PrintStream getOutput(UIExecutionContext context) {
        return context.getUIContext().getProvider().getOutput().out();
    }

    protected PrintStream getError(UIExecutionContext context) {
        return context.getUIContext().getProvider().getOutput().err();
    }

}
