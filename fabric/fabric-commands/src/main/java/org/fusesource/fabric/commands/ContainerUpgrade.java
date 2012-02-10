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
package org.fusesource.fabric.commands;

import java.util.Arrays;
import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.commands.support.FabricCommand;

@Command(name = "container-upgrade", scope = "fabric", description = "Upgrades containers to a new version")
public class ContainerUpgrade extends FabricCommand {

    @Option(name = "--version", description = "The version to upgrade", required = true)
    private String version;

    @Argument(index = 0, name = "container", description = "The list of containers to upgrade. Empty list assumes current container only.", required = false, multiValued = true)
    private List<String> containerIds;

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        // check and validate version
        Version version = fabricService.getVersion(this.version);

        if (containerIds == null || containerIds.isEmpty()) {
            containerIds = Arrays.asList(fabricService.getCurrentContainer().getId());
        }

        // check that all container current version is <= the version to upgrade
        for (String containerName : containerIds) {
            Container container = fabricService.getContainer(containerName);

            // version should be upgradeable, so compare current version to the upgraded version
            Version current = container.getVersion();
            int num = version.compareTo(current);

            if (num < 0) {
                throw new IllegalArgumentException("Container " + container.getId() + " has already higher version " + current
                        + " than the requested version " + version + " to be updated.");
            } else if (num > 0) {
                // upgrade container
                container.setVersion(version);
                System.out.println("Upgraded container " + container.getId() + " from version " + current + " to " + version);
            } else {
                // already same version
                System.out.println("Container " + container.getId() + " is already version " + current);
            }
        }
        return null;
    }
}
