package org.fusesource.process.fabric.commands;

import com.google.common.base.Preconditions;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import io.fabric8.api.FabricAuthenticationException;
import io.fabric8.utils.shell.ShellUtils;
import org.fusesource.process.fabric.ContainerProcessManager;

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
}
