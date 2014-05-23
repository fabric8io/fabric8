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
package io.fabric8.process.fabric.commands;

import com.google.common.base.Preconditions;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import io.fabric8.api.FabricAuthenticationException;
import io.fabric8.utils.shell.ShellUtils;
import io.fabric8.process.fabric.ContainerProcessManager;

import java.io.IOException;

public abstract class ContainerProcessCommandSupport extends OsgiCommandSupport {

    @Option(name="-c", aliases={"--controllerUrl"}, required = false, description = "The optional JSON document URL containing the controller configuration")
    protected String controllerJson;
    @Option(name="-k", aliases={"--kind"}, required = false, description = "The kind of controller to create")
    protected String controllerKind;

    @Option(name="-u", aliases={"--user"}, required = false, description = "The jmx user of the target container")
    protected String user;
    @Option(name="-p", aliases={"--password"}, required = false, description = "The jmx password of the target container")
    protected String password;

    @Argument(index = 0, required = true, name = "container", description = "The container to manage the process")
    protected String container;


    private ContainerProcessManager containerProcessManager;

    abstract void doWithAuthentication(String jmxUser, String jmxPassword) throws Exception;

    protected void checkRequirements() {
        Preconditions.checkNotNull(containerProcessManager, "containerProcessManager");
    }


    @Override
    protected Object doExecute() throws Exception {
        checkRequirements();
        try {
            String jmxUser = user != null ? user : ShellUtils.retrieveFabricUser(session);
            String jmxPassword = password != null ? password : ShellUtils.retrieveFabricUserPassword(session);
            doWithAuthentication(jmxUser, jmxPassword);
        } catch (FabricAuthenticationException ex) {
            //If authentication fails, prompts for credentials and try again.
            user = null;
            password = null;
            promptForJmxCredentialsIfNeeded();
            doWithAuthentication(user, password);
            ShellUtils.storeFabricCredentials(session, user, password);
        }
        return null;
    }

    protected void promptForJmxCredentialsIfNeeded() throws IOException {
        // If the username was not configured via cli, then prompt the user for the values
        if (user == null) {
            log.debug("Prompting user for jmx login");
            user = ShellUtils.readLine(session, "Jmx Login for " + container + ": ", false);
        }

        if (password == null) {
            password = ShellUtils.readLine(session, "Jmx Password for " + container + ": ", true);
        }
    }

    public ContainerProcessManager getContainerProcessManager() {
        return containerProcessManager;
    }

    public void setContainerProcessManager(ContainerProcessManager containerProcessManager) {
        this.containerProcessManager = containerProcessManager;
    }

    protected Container getContainerObject() {
        return getContainerProcessManager().getFabricService().getContainer(container);
    }
}
