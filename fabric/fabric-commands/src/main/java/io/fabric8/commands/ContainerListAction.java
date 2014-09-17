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
import io.fabric8.api.DataStore;
import io.fabric8.api.FabricService;
import io.fabric8.api.ProfileService;
import io.fabric8.api.Version;
import io.fabric8.commands.support.CommandUtils;

import java.io.PrintStream;
import java.util.List;
import java.util.Locale;

import io.fabric8.utils.TablePrinter;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = ContainerList.FUNCTION_VALUE, scope = ContainerList.SCOPE_VALUE, description = ContainerList.DESCRIPTION, detailedDescription = "classpath:containerList.txt")
public class ContainerListAction extends AbstractAction {

    @Option(name = "--version", description = "Optional version to use as filter")
    private String version;
    @Option(name = "-v", aliases = "--verbose", description = "Flag for verbose output", multiValued = false, required = false)
    private boolean verbose;
    @Argument(index = 0, name = "filter", description = "Filter by container ID or by profile name. When a profile name is specified, only the containers with that profile are listed.", required = false, multiValued = false)
    private String filter = null;

    private final FabricService fabricService;
    private final ProfileService profileService;
    private final DataStore dataStore;

    ContainerListAction(FabricService fabricService) {
        this.fabricService = fabricService;
        this.profileService = fabricService.adapt(ProfileService.class);
        this.dataStore = fabricService.adapt(DataStore.class);
    }

    @Override
    protected Object doExecute() throws Exception {
        Container[] containers = fabricService.getContainers();

        // filter unwanted containers, and split list into parent/child,
        // so we can sort the list as we want it
        containers = CommandUtils.filterContainers(containers, filter);

        // we want the list to be sorted
        containers = CommandUtils.sortContainers(containers);

        Version ver = null;
        if (version != null) {
            // limit containers to only with same version
            ver = profileService.getRequiredVersion(version);
        }

        if (verbose) {
            printContainersVerbose(containers, ver, System.out);
        } else {
            printContainers(containers, ver, System.out);
        }
        return null;
    }

    private void printContainers(Container[] containers, Version version, PrintStream out) {
        TablePrinter table = new TablePrinter();
        table.columns("id", "version", "type", "connected", "profiles", "provision status");
        for (Container container : containers) {
            if (CommandUtils.matchVersion(container, version)) {
                String indent = "";
                for (Container c = container; !c.isRoot(); c = c.getParent()) {
                    indent+="  ";
                }
                //Mark local container with a star symbol
                String marker = "";
                if (container.getId().equals(fabricService.getCurrentContainer().getId())) {
                    marker = "*";
                }

                List<String> assignedProfiles = dataStore.getContainerProfiles(container.getId());
                table.row(indent + container.getId() + marker, container.getVersion().getId(), container.getType(),
                        aliveText(container), assignedProfiles.get(0), CommandUtils.status(container));

                // we want multiple profiles to be displayed on next lines
                for (int i = 1; i < assignedProfiles.size(); i++) {
                    table.row("", "", "", "", assignedProfiles.get(i), "");
                }
            }
        }
        table.print();
    }

    protected static String aliveText(Container container) {
        return container.isAlive() ? "yes" : "";
    }

    private void printContainersVerbose(Container[] containers, Version version, PrintStream out) {
        TablePrinter table = new TablePrinter();
        table.columns("id", "version", "type", "connected", "profiles", "blueprint", "spring", "provision status");
        for (Container container : containers) {
            if (CommandUtils.matchVersion(container, version)) {
                String indent = "";
                for (Container c = container; !c.isRoot(); c = c.getParent()) {
                    indent += "  ";
                }
                //Mark local container with a star symbol
                String marker = "";
                if (container.getId().equals(fabricService.getCurrentContainer().getId())) {
                    marker = "*";
                }

                String blueprintStatus = dataStore.getContainerAttribute(container.getId(), DataStore.ContainerAttribute.BlueprintStatus, "", false, false);
                String springStatus = dataStore.getContainerAttribute(container.getId(), DataStore.ContainerAttribute.SpringStatus, "", false, false);
                blueprintStatus = blueprintStatus.toLowerCase(Locale.ENGLISH);
                springStatus = springStatus.toLowerCase(Locale.ENGLISH);

                List<String> assignedProfiles = dataStore.getContainerProfiles(container.getId());
                table.row(indent + container.getId() + marker, container.getVersion().getId(), container.getType(),
                        aliveText(container), assignedProfiles.get(0), blueprintStatus, springStatus, CommandUtils.status(container));

                // we want multiple profiles to be displayed on next lines
                for (int i = 1; i < assignedProfiles.size(); i++) {
                    table.row("", "", "", "", assignedProfiles.get(i), "", "", "");
                }
            }
        }
        table.print();

    }

}
