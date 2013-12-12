/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.api.Container;
import io.fabric8.api.Version;
import io.fabric8.commands.support.ContainerUpgradeSupport;
import static io.fabric8.utils.FabricValidations.validateContainersName;

@Command(name = "container-upgrade", scope = "fabric", description = "Upgrade containers to a new version", detailedDescription = "classpath:containerUpgrade.txt")
public class ContainerUpgrade extends ContainerUpgradeSupport {

    @Option(name = "--all", description = "Upgrade all containers")
    private boolean all;
    @Argument(index = 0, name = "version", description = "The version to upgrade to.", required = true)
    private String version;
    @Argument(index = 1, name = "container", description = "The list of containers to upgrade. An empty list implies the current container.", required = false, multiValued = true)
    private List<String> containerIds;

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        validateContainersName(containerIds);

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

        List<Container> toUpgrade = new ArrayList<Container>();
        List<Container> same = new ArrayList<Container>();
        for (String containerName : containerIds) {
            Container container = getContainer(containerName);

            // check first that all can upgrade
            int num = canUpgrade(version, container);
            if (num < 0) {
                throw new IllegalArgumentException("Container " + container.getId() + " already has a higher version " + container.getVersion()
                        + " than the requested version " + version + ".");
            } else if (num == 0) {
                // same version
                same.add(container);
            } else {
                // needs upgrade
                toUpgrade.add(container);
            }
        }
        
        // report same version
        for (Container container : same) {
            System.out.println("Container " + container.getId() + " is already at version " + version);
        }
        
        // report and do upgrades
        for (Container container : toUpgrade) {
            Version oldVersion = container.getVersion();
            // upgrade version first
            container.setVersion(version);
            // get the profile for version 1.1
            log.info("Upgraded container {} from {} to {}", new Object[]{container, oldVersion, version});
            System.out.println("Upgraded container " + container.getId() + " from version " + oldVersion + " to " + version);
        }

        if (all) {
            fabricService.setDefaultVersion(version);
            System.out.println("Changed default version to " + version);
        }

        return null;
    }

}
