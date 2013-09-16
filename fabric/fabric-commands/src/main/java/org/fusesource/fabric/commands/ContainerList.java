/*
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

import java.io.PrintStream;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.boot.commands.support.FabricCommand;

import static org.fusesource.fabric.commands.support.CommandUtils.filterContainers;
import static org.fusesource.fabric.commands.support.CommandUtils.matchVersion;
import static org.fusesource.fabric.commands.support.CommandUtils.sortContainers;
import static org.fusesource.fabric.commands.support.CommandUtils.status;

@Command(name = "container-list", scope = "fabric", description = "List the containers in the current fabric")
public class ContainerList extends FabricCommand {

    static final String FORMAT = "%-30s %-9s %-7s %-50s %s";
    static final String VERBOSE_FORMAT = "%-20s %-9s %-7s %-30s  %-30s %-90s %s";

    static final String[] HEADERS = {"[id]", "[version]", "[alive]", "[profiles]", "[provision status]"};
    static final String[] VERBOSE_HEADERS = {"[id]", "[version]", "[alive]", "[profiles]", "[ssh url]", "[jmx url]", "[provision status]"};

    @Option(name = "--version", description = "Optional version to use as filter")
    private String version;
    @Option(name = "-v", aliases = "--verbose", description = "Flag for verbose output", multiValued = false, required = false)
    private boolean verbose;
    @Argument(index = 0, name = "filter", description = "Filter by container ID or by profile name. When a profile name is specified, only the containers with that profile are listed.", required = false, multiValued = false)
    private String filter = null;

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        Container[] containers = fabricService.getContainers();

        // filter unwanted containers, and split list into parent/child,
        // so we can sort the list as we want it 
        containers = filterContainers(containers, filter);

        // we want the list to be sorted
        containers = sortContainers(containers);

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

    protected void printContainers(Container[] containers, Version version, PrintStream out) {
        out.println(String.format(FORMAT, HEADERS));
        for (Container container : containers) {
            if (matchVersion(container, version)) {
                String indent = "";
                for (Container c = container; !c.isRoot(); c = c.getParent()) {
                    indent+="  ";
                }
                //Mark local container with a star symobl
                String marker = "";
                if (container.getId().equals(fabricService.getCurrentContainer().getId())) {
                    marker = "*";
                }
                out.println(String.format(FORMAT, indent + container.getId() + marker, container.getVersion().getId(), container.isAlive(), toString(container.getProfiles()), status(container)));
            }
        }
    }

    protected void printContainersVerbose(Container[] containers, Version version, PrintStream out) {
        out.println(String.format(VERBOSE_FORMAT, VERBOSE_HEADERS));
        for (Container container : containers) {
            if (matchVersion(container, version)) {
                String indent = "";
                for (Container c = container; !c.isRoot(); c = c.getParent()) {
                    indent += "  ";
                }
                //Mark local container with a star symobl
                String marker = "";
                if (container.getId().equals(fabricService.getCurrentContainer().getId())) {
                    marker = "*";
                }
                out.println(String.format(VERBOSE_FORMAT, indent + container.getId() + marker,  container.getVersion().getId(), container.isAlive(), toString(container.getProfiles()), container.getSshUrl(), container.getJmxUrl(), status(container)));
            }
        }
    }
}
