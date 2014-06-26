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

import java.io.PrintStream;
import java.util.List;
import java.util.Locale;

import io.fabric8.api.Container;
import io.fabric8.api.DataStore;
import io.fabric8.api.FabricService;
import io.fabric8.api.Version;
import io.fabric8.commands.support.CommandUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = ContainerList.FUNCTION_VALUE, scope = ContainerList.SCOPE_VALUE, description = ContainerList.DESCRIPTION, detailedDescription = "classpath:containerList.txt")
public class ContainerListAction extends AbstractAction {

    static final String FORMAT = "%-30s %-9s %-10s %-11s %-60s %s";
    static final String VERBOSE_FORMAT = "%-30s %-9s %-10s %-11s %-60s %-13s %-11s %s";

    static final String[] HEADERS = {"[id]", "[version]", "[type]", "[connected]", "[profiles]", "[provision status]"};
    static final String[] VERBOSE_HEADERS = {"[id]", "[version]", "[type]", "[connected]", "[profiles]", "[blueprint]", "[spring]", "[provision status]"};

    @Option(name = "--version", description = "Optional version to use as filter")
    private String version;
    @Option(name = "-v", aliases = "--verbose", description = "Flag for verbose output", multiValued = false, required = false)
    private boolean verbose;
    @Argument(index = 0, name = "filter", description = "Filter by container ID or by profile name. When a profile name is specified, only the containers with that profile are listed.", required = false, multiValued = false)
    private String filter = null;

    private final FabricService fabricService;

    ContainerListAction(FabricService fabricService) {
        this.fabricService = fabricService;
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
            ver = fabricService.getVersion(version);
        }

        if (verbose) {
            printContainersVerbose(containers, ver, System.out);
        } else {
            printContainers(containers, ver, System.out);
        }
        return null;
    }

    private void printContainers(Container[] containers, Version version, PrintStream out) {
        out.println(String.format(FORMAT, (Object[])HEADERS));
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

                List<String> assignedProfiles = fabricService.getDataStore().getContainerProfiles(container.getId());
                String firstLine = String.format(FORMAT, indent + container.getId() + marker, container.getVersion().getId(), container.getType(),
                        container.isAlive(), assignedProfiles.get(0), CommandUtils.status(container));
                out.println(firstLine);

                // we want multiple profiles to be displayed on next lines
                for (int i = 1; i < assignedProfiles.size(); i++) {
                    String nextLine = String.format(FORMAT, "", "", "", "", assignedProfiles.get(i), "");
                    out.println(nextLine);
                }
            }
        }
    }

    private void printContainersVerbose(Container[] containers, Version version, PrintStream out) {
        String header = String.format(VERBOSE_FORMAT, (Object[])VERBOSE_HEADERS);

        out.println(header);
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

                String blueprintStatus = fabricService.getDataStore().getContainerAttribute(container.getId(), DataStore.ContainerAttribute.BlueprintStatus, "", false, false);
                String springStatus = fabricService.getDataStore().getContainerAttribute(container.getId(), DataStore.ContainerAttribute.SpringStatus, "", false, false);
                blueprintStatus = blueprintStatus.toLowerCase(Locale.ENGLISH);
                springStatus = springStatus.toLowerCase(Locale.ENGLISH);

                List<String> assignedProfiles = fabricService.getDataStore().getContainerProfiles(container.getId());
                String firstLine = String.format(VERBOSE_FORMAT, indent + container.getId() + marker, container.getVersion().getId(), container.getType(),
                        container.isAlive(), assignedProfiles.get(0), blueprintStatus, springStatus, CommandUtils.status(container));
                out.println(firstLine);

                // we want multiple profiles to be displayed on next lines
                for (int i = 1; i < assignedProfiles.size(); i++) {
                    String nextLine = String.format(VERBOSE_FORMAT, "", "", "", "", assignedProfiles.get(i), "", "", "");
                    out.println(nextLine);
                }
            }
        }
    }

}
