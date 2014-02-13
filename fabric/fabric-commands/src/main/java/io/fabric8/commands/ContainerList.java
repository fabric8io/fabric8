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
package io.fabric8.commands;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.api.Container;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.boot.commands.support.FabricCommand;
import org.fusesource.jansi.Ansi;

import static io.fabric8.commands.support.CommandUtils.filterContainers;
import static io.fabric8.commands.support.CommandUtils.matchVersion;
import static io.fabric8.commands.support.CommandUtils.sortContainers;
import static io.fabric8.commands.support.CommandUtils.status;

@Command(name = "container-list", scope = "fabric", description = "List the containers in the current fabric")
public class ContainerList extends FabricCommand {
    static final String FORMAT = "%-30s %-9s %-11s %-50s %s";
    static final String VERBOSE_FORMAT = "%-20s %-9s %-11s %-30s  %-30s %-90s %s";

    static final String[] HEADERS = {"[id]", "[version]", "[connected]", "[profiles]", "[provision status]"};
    static final String[] VERBOSE_HEADERS = {"[id]", "[version]", "[connected]", "[profiles]", "[ssh url]", "[jmx url]", "[provision status]"};

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
        displayMissingProfiles(findMissingProfiles(containers), System.out);
        return null;
    }

    protected void printContainers(Container[] containers, Version version, PrintStream out) {
        Set<String> missingProfiles = findMissingProfiles(containers);
        String header = String.format(FORMAT, HEADERS);

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

                String assignedProfiles = toString(fabricService.getDataStore().getContainerProfiles(container.getId()));
                String highlightedProfiles = new String(assignedProfiles);
                String line = String.format(FORMAT, indent + container.getId() + marker, container.getVersion().getId(), container.isAlive(), assignedProfiles, status(container));

                int pStart = Math.max(header.indexOf(HEADERS[3]), line.indexOf(assignedProfiles));
                int pEnd = pStart + assignedProfiles.length();

                for (String p : missingProfiles) {
                    String highlighted = Ansi.ansi().fg(Ansi.Color.RED).a(p).toString() + Ansi.ansi().reset().toString();
                    highlightedProfiles = highlightedProfiles.replaceAll(p, highlighted);
                }

                line = replaceAll(line, pStart, pEnd, assignedProfiles, highlightedProfiles);
                out.println(line);
            }
        }
    }

    protected void printContainersVerbose(Container[] containers, Version version, PrintStream out) {
        Set<String> missingProfiles = findMissingProfiles(containers);
        String header = String.format(VERBOSE_FORMAT, VERBOSE_HEADERS);

        out.println(header);
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
                String assignedProfiles = toString(container.getProfiles());
                String highlightedProfiles = new String(assignedProfiles);
                String line = String.format(VERBOSE_FORMAT, indent + container.getId() + marker, container.getVersion().getId(), container.isAlive(), assignedProfiles, container.getSshUrl(), container.getJmxUrl(), status(container));
                int pStart = Math.max(header.indexOf(HEADERS[3]), line.indexOf(assignedProfiles));
                int pEnd = pStart + assignedProfiles.length();

                for (String p : missingProfiles) {
                    String highlighted = Ansi.ansi().fg(Ansi.Color.RED).a(p).toString() + Ansi.ansi().reset().toString();
                    highlightedProfiles = highlightedProfiles.replaceAll(p, highlighted);
                }

                line = replaceAll(line, pStart, pEnd, assignedProfiles, highlightedProfiles);
                out.println(line);
            }
        }
    }

    private static String replaceAll(String source, int start, int end, String pattern, String replacement) {
        return source.substring(0, start) + source.substring(start, end).replaceAll(pattern, replacement) + source.substring(end);
    }

    private static void displayMissingProfiles(Set<String> missingProfiles, PrintStream out) {
        if (!missingProfiles.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("The following profiles are assigned but not found:");
            for (String profile : missingProfiles) {
                sb.append(" ").append(profile);
            }
            sb.append(".");
            out.println(sb.toString());
        }
    }

    private Set<String> findMissingProfiles(Container[] containers) {
        Set<String> missingProfiles = new HashSet<String>();
        for (Container container : containers) {
            for (String p : fabricService.getDataStore().getContainerProfiles(container.getId())) {
                if (!container.getVersion().hasProfile(p)) {
                    missingProfiles.add(p);
                }
            }
        }
        return missingProfiles;
    }
}
