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

import java.io.PrintStream;
import javax.inject.Inject;

import org.jboss.forge.addon.configuration.Configuration;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.UIProvider;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.output.UIOutput;

/**
 * Base class for all Jolokia Camel commands.
 */
public abstract class AbstractJolokiaCommand extends AbstractProjectCommand {

    public static String CATEGORY = "Camel";

    @Inject
    protected ProjectFactory projectFactory; // helper to integrate with the filesystem

    @Inject
    protected Configuration configuration;

    protected UIProvider uiProvider;

    @Override
    protected boolean isProjectRequired() {
        return false;
    }

    @Override
    protected ProjectFactory getProjectFactory() {
        return projectFactory;
    }

    protected UIProvider getUiProvider() {
        return uiProvider;
    }

    protected UIOutput getOutput() {
        UIProvider provider = getUiProvider();
        return provider != null ? provider.getOutput() : null;
    }

    protected PrintStream getOut() {
        UIOutput output = getOutput();
        if (output != null) {
            return output.out();
        } else {
            return System.out;
        }
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
    }

    protected String getJolokiaUrl() {
        return configuration.getString("CamelJolokiaUrl");
    }

}
