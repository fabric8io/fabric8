package io.fabric8.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.api.Container;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.boot.commands.support.FabricCommand;

public abstract class ContainerLifecycleCommand extends FabricCommand {

    @Option(name = "--user", description = "The username to use.")
    String user;

    @Option(name = "--password", description = "The password to use.")
    String password;

    @Option(name = "-f", aliases = {"--force"}, multiValued = false, required = false, description = "Force the execution of the command regardless of the known state of the container")
    boolean force = false;

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
