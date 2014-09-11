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
package io.fabric8.boot.commands;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.RuntimeProperties;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;

@Command(name = WelcomeCommand.FUNCTION_VALUE, scope = WelcomeCommand.SCOPE_VALUE, description = WelcomeCommand.DESCRIPTION)
final class WelcomeAction extends AbstractAction {

    private final FabricService fabricService;
    private final ConfigurationAdmin configAdmin;
    private final BundleContext bundleContext;
    private final RuntimeProperties runtimeProperties;

    WelcomeAction(BundleContext bundleContext, FabricService fabricService, ConfigurationAdmin configAdmin, RuntimeProperties runtimeProperties) {
        this.bundleContext = bundleContext;
        this.fabricService = fabricService;
        this.configAdmin = configAdmin;
        this.runtimeProperties = runtimeProperties;
    }

    @Override
    protected Object doExecute() throws Exception {
        String name = runtimeProperties.getRuntimeIdentity();

        String appName = runtimeProperties.getProperty("karaf.app.name");

        System.out.println("Welcome to " + appName);
        System.out.println("");

        // are we part of fabric?
        if (fabricService != null) {
            Container container = fabricService.getCurrentContainer();
            if (container != null) {
                boolean ensemble = container.isEnsembleServer();
                if (ensemble) {
                    System.out.println("This container \u001B[1m" + container.getId() + "\u001B[0m is a Fabric ensemble server.");
                } else {
                    System.out.println("This container \u001B[1m" + container.getId() + "\u001B[0m is joined to an existing Fabric.");
                }
            } else {
                System.out.println("This container \u001B[1m" + name + "\u001B[0m is a standalone container.");
            }
            String url = fabricService.getWebConsoleUrl();
            if (url != null) {
                System.out.println("Web management console available at: \u001B[1m" + url + "\u001B[0m");
            }
            System.out.println("");
        } else {
            // no we are standalone
            System.out.println("This container \u001B[1m" + name + "\u001B[0m is a standalone container.");
            System.out.println("");
            System.out.println("Create a new Fabric via '\u001B[1mfabric:create\u001B[0m'");
            System.out.println("or join an existing Fabric via '\u001B[1mfabric:join [someUrls]\u001B[0m'");
            System.out.println("");
        }

        return null;
    }

}
