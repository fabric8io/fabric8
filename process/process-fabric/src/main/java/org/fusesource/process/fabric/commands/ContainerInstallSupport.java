package org.fusesource.process.fabric.commands;

import com.google.common.base.Preconditions;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.utils.shell.ShellUtils;
import org.fusesource.process.fabric.ContainerProcessManager;
import org.fusesource.process.manager.commands.support.InstallSupport;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public abstract class ContainerInstallSupport extends InstallSupport {

    @Option(name="-u", aliases={"--user"}, required = false, description = "The jmx user of the target container")
    protected String user;
    @Option(name="-p", aliases={"--password"}, required = false, description = "The jmx password of the target container")
    protected String password;

    @Argument(index = 0, required = true, name = "container", description = "The container to manage the process")
    protected String container;


    private ContainerProcessManager containerProcessManager;

    @Override
    protected void checkRequirements() {
        Preconditions.checkNotNull(containerProcessManager, "containerProcessManager");
    }

    @Override
    protected URL getControllerURL() throws MalformedURLException {
        URL controllerUrl = null;
        if (controllerJson != null) {
            controllerUrl = new URL(controllerJson);
        } else if (controllerKind != null) {
            String name = controllerKind + ".json";
            controllerUrl = new URL("profile:" + name);
            if (controllerUrl == null) {
                throw new IllegalStateException("Cannot find controller kind: " + name + " on the classpath");
            }
        }
        return controllerUrl;
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
