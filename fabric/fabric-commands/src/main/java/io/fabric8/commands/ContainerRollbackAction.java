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
package io.fabric8.commands;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Version;
import io.fabric8.boot.commands.support.FabricCommand;
import io.fabric8.commands.support.ContainerUpgradeSupport;
import io.fabric8.utils.FabricValidations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = ContainerRollback.FUNCTION_VALUE, scope = ContainerRollback.SCOPE_VALUE, description = ContainerRollback.DESCRIPTION, detailedDescription = "classpath:containerUpgrade.txt")
public final class ContainerRollbackAction extends AbstractAction {

    @Option(name = "--all", description = "Roll back all containers")
    private boolean all;
    @Argument(index = 0, name = "version", description = "The version to roll back to.", required = true)
    private String version;
    @Argument(index = 1, name = "container", description = "The list of containers to roll back. An empty list implies the current container.", required = false, multiValued = true)
    private List<String> containerIds;

    protected final FabricService fabricService;

    ContainerRollbackAction(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    @Override
    protected Object doExecute() throws Exception {
        FabricValidations.validateContainerNames(containerIds);

        // check and validate version
        Version version = fabricService.getVersion(this.version);

        if (containerIds == null || containerIds.isEmpty()) {
            if (all) {
                containerIds = new ArrayList<String>();
                for (Container container : fabricService.getContainers()) {
                    containerIds.add(container.getId());
                }
            } else {
                containerIds = Arrays.asList(fabricService.getCurrentContainer().getId());
            }
        } else {
            if (all) {
                throw new IllegalArgumentException("Can not use --all with a list of containers simultaneously");
            }
        }

        List<Container> toRollback = new ArrayList<Container>();
        List<Container> same = new ArrayList<Container>();
        for (String containerName : containerIds) {
            Container container = FabricCommand.getContainer(fabricService, containerName);

            // check first that all can rollback
            int num = ContainerUpgradeSupport.canRollback(version, container);
            if (num < 0) {
                throw new IllegalArgumentException("Container " + container.getId() + " has already lower version " + container.getVersion()
                        + " than the requested version " + version + " to rollback.");
            } else if (num == 0) {
                // same version
                same.add(container);
            } else {
                // needs rollback
                toRollback.add(container);
            }
        }

        // report same version
        for (Container container : same) {
            System.out.println("Container " + container.getId() + " is already version " + version);
        }

        // report and do rollbacks
        for (Container container : toRollback) {
            Version oldVersion = container.getVersion();
            // rollback version first
            container.setVersion(version);
            log.debug("Rolled back container {} from {} to {}", new Object[]{container, oldVersion, version});
            System.out.println("Rolled back container " + container.getId() + " from version " + oldVersion + " to " + version);
        }

		if (all) {
			fabricService.setDefaultVersion(version);
			System.out.println("Changed default version to " + version);
		}

        return null;
    }

}
