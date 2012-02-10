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

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.commands.support.FabricCommand;

import static org.fusesource.fabric.commands.support.CommandUtils.matchVersion;
import static org.fusesource.fabric.commands.support.CommandUtils.sortContainers;
import static org.fusesource.fabric.commands.support.CommandUtils.status;

@Command(name = "container-list", scope = "fabric", description = "List existing containers")
public class ContainerList extends FabricCommand {

    static final String FORMAT = "%-30s %-9s %-7s %-30s %s";
    static final String VERBOSE_FORMAT = "%-20s %-9s %-7s %-30s  %-30s %-90s %s";

    static final String[] HEADERS = {"[id]", "[version]", "[alive]", "[profiles]", "[provision status]"};
    static final String[] VERBOSE_HEADERS = {"[id]", "[version]", "[alive]", "[profiles]", "[ssh url]", "[jmx url]", "[provision status]"};

    @Option(name = "--version", description = "Optional version to use as filter")
    private String version;

    @Option(name = "-v", aliases = "--verbose", description = "Flag for verbose output", multiValued = false, required = false)
    private boolean verbose;

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        Container[] containers = fabricService.getContainers();
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
            if (container.isRoot()) {
                if (matchVersion(container, version)) {
                    out.println(String.format(FORMAT, container.getId(), container.getVersion().getName(), container.isAlive(), toString(container.getProfiles()), status(container)));
                }
                for (Container child : containers) {
                    if (child.getParent() == container) {
                        if (matchVersion(child, version)) {
                            out.println(String.format(FORMAT, "  " + child.getId(), child.getVersion().getName(), child.isAlive(), toString(child.getProfiles()), status(child)));
                        }
                    }
                }
            }
        }
    }

    protected void printContainersVerbose(Container[] containers, Version version, PrintStream out) {
        out.println(String.format(VERBOSE_FORMAT, VERBOSE_HEADERS));
        for (Container container : containers) {
            if (container.isRoot()) {
                if (matchVersion(container, version)) {
                    out.println(String.format(VERBOSE_FORMAT, container.getId(), container.getVersion().getName(), container.isAlive(), toString(container.getProfiles()), container.getSshUrl(), container.getJmxUrl(), status(container)));
                }
                for (Container child : containers) {
                    if (child.getParent() == container) {
                        if (matchVersion(child, version)) {
                            out.println(String.format(VERBOSE_FORMAT, "  " + child.getId(), child.getVersion().getName(), child.isAlive(), toString(child.getProfiles()), child.getSshUrl(), child.getJmxUrl(), status(child)));
                        }
                    }
                }
            }
        }
    }
    

}
