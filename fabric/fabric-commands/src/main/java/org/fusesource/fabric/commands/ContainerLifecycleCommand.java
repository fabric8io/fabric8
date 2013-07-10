package org.fusesource.fabric.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.CreateContainerMetadata;
import org.fusesource.fabric.boot.commands.support.FabricCommand;

public abstract class ContainerLifecycleCommand extends FabricCommand {

    @Option(name = "--user", description = "The username to use.")
    String user;

    @Option(name = "--password", description = "The password to use.")
    String password;

    @Argument(index = 0, name = "container", description = "The container name", required = true, multiValued = false)
    String container = null;

    void applyUpdatedCredentials(Container container) {
        if (user != null || password != null) {
            CreateContainerMetadata metadata = container.getMetadata();
            if (metadata != null) {
                metadata.updateCredentials(user, password);
                fabricService.getDataStore().setContainerMetadata(container.getMetadata());
            }
        }
    }
}
