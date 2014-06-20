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
import io.fabric8.commands.support.CommandUtils;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import io.fabric8.common.util.*;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;
import org.fusesource.jansi.Ansi;

@Command(name = ContainerList.FUNCTION_VALUE, scope = ContainerList.SCOPE_VALUE, description = ContainerList.DESCRIPTION)
public class ContainerListAction extends AbstractAction {

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
        displayMissingProfiles(findMissingProfiles(containers), System.out);
        return null;
    }

    private void printContainers(Container[] containers, Version version, PrintStream out) {
        Set<String> missingProfiles = findMissingProfiles(containers);
        String header = String.format(FORMAT, (Object[])HEADERS);
        int count=0;
        out.println(String.format(FORMAT, (Object[])HEADERS));
        for (Container container : containers) {
            count++;
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

                String assignedProfiles = FabricCommand.toString(fabricService.getDataStore().getContainerProfiles(container.getId()));
                String str="";
                for( String s: assignedProfiles.split(",") ) {
                        str += Strings.rpadByMaxSize(" ", s, 31) + "," ;
                }

                String highlightedProfiles ="";
                String secondlinehighlightedProfiles="";
                highlightedProfiles = str.split(",")[0];
                if(str.split(",").length>1) {
                    secondlinehighlightedProfiles = Strings.rpad(" ", System.getProperty("line.separator") ,52) + Arrays.join(Strings.rpad(" ",System.getProperty("line.separator") , 52), java.util.Arrays.asList(str.split(",")).subList(1, str.split(",").length).toArray() );
                }

                if(count!=1) {
                    highlightedProfiles = Strings.rpad(" ", highlightedProfiles, 16);
                }
                String status= CommandUtils.status(container) + secondlinehighlightedProfiles ;
                String line = String.format(FORMAT, indent + container.getId() + marker, container.getVersion().getId(), container.isAlive(), highlightedProfiles, status);
    
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

    private void printContainersVerbose(Container[] containers, Version version, PrintStream out) {
        Set<String> missingProfiles = findMissingProfiles(containers);
        String header = String.format(VERBOSE_FORMAT, (Object[])VERBOSE_HEADERS);

        out.println(header);
        for (Container container : containers) {
            if (CommandUtils.matchVersion(container, version)) {
                String indent = "";
                for (Container c = container; !c.isRoot(); c = c.getParent()) {
                    indent += "  ";
                }
                //Mark local container with a star symobl
                String marker = "";
                if (container.getId().equals(fabricService.getCurrentContainer().getId())) {
                    marker = "*";
                }
                String assignedProfiles = FabricCommand.toString(container.getProfiles());
                String highlightedProfiles = new String(assignedProfiles);
                String line = String.format(VERBOSE_FORMAT, indent + container.getId() + marker, container.getVersion().getId(), container.isAlive(), assignedProfiles, container.getSshUrl(), container.getJmxUrl(), CommandUtils.status(container));
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

    private String replaceAll(String source, int start, int end, String pattern, String replacement) {
        return source.substring(0, start) + source.substring(start, end).replaceAll(pattern, replacement) + source.substring(end);
    }

    private void displayMissingProfiles(Set<String> missingProfiles, PrintStream out) {
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
