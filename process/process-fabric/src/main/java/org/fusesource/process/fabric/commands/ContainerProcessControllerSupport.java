package org.fusesource.process.fabric.commands;

import org.apache.felix.gogo.commands.Argument;
import org.fusesource.process.fabric.ContainerInstallOptions;
import org.fusesource.process.manager.Installation;

import java.util.Map;

public abstract class ContainerProcessControllerSupport extends ContainerProcessCommandSupport {

    @Argument(index = 1, required = true, multiValued = true, name = "id", description = "The id of the managed processes to control")
    protected int[] ids;

    protected abstract void doControlCommand(Installation installation) throws Exception;

    void doWithAuthentication(String jmxUser, String jmxPassword) throws Exception {
        {
            ContainerInstallOptions options = ContainerInstallOptions.builder()
                    .container(container)
                    .user(jmxUser)
                    .password(jmxPassword)
                    .build();

            Map<Integer, Installation> map = getContainerProcessManager().listInstallationMap(options);
            for (int id : ids) {
                Installation installation = map.get(id);
                if (installation == null) {
                    System.out.println("No such process number: " + id);
                } else {
                    doControlCommand(installation);
                }
            }
        }
    }
}
