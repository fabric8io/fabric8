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
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.FabricService;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

public abstract class AbstractContainerLifecycleAction extends AbstractAction {

    @Option(name = "--user", description = "The username to use.")
    String user;

    @Option(name = "--password", description = "The password to use.")
    String password;

    @Option(name = "-f", aliases = {"--force"}, multiValued = false, required = false, description = "Force the execution of the command regardless of the known state of the container")
    boolean force = false;

    @Argument(index = 0, name = "container", description = "The container names", required = true, multiValued = true)
    List<String> containers = null;

    protected final FabricService fabricService;

    AbstractContainerLifecycleAction(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    void applyUpdatedCredentials(Container container) {
        if (user != null || password != null) {
            CreateContainerMetadata<?> metadata = container.getMetadata();
            if (metadata != null) {
                metadata.updateCredentials(user, password);
                fabricService.getDataStore().setContainerMetadata(container.getMetadata());
            }
        }
    }

    /**
     * Returns a list of all available containers matching simple pattern where {@code *} matches any substring
     * and {@code ?} matches single character.
     */
    protected List<String> matchedAvailableContainers(String pattern) {
        LinkedList<String> result = new LinkedList<String>();
        for (Container c: this.fabricService.getContainers()) {
            String name = c.getId();
            if (this.matches(pattern, name))
                result.add(name);
        }
        return result;
    }

    /**
     * Simple "glob" pattern matching
     */
    protected boolean matches(String globPattern, String name) {
        String re = "^" + globPattern.replace(".", "\\.").replace("?", ".?").replace("*", ".*") + "$";
        return name.matches(re);
    }

    /**
     * <p>Converts a list of possibly wildcard container names into list of available container names.</p>
     * <p>It also checks if the expanded list has at least one element</p>
     */
    protected Collection<String> expandGlobNames(List<String> containerNames) {
        Collection<String> expandedNames = new LinkedHashSet<String>();
        if (containerNames == null) {
            this.session.getConsole().println("Please specify container name(s).");
            return expandedNames;
        }
        boolean globUsed = false;
        for (String name: containerNames) {
            if (name.contains("*") || name.contains("?")) {
                globUsed = true;
                expandedNames.addAll(this.matchedAvailableContainers(name));
            } else {
                expandedNames.add(name);
            }
        }
        if (expandedNames.size() == 0) {
            if (globUsed) {
                this.session.getConsole().println("Please specify container name(s). Your pattern didn't match any container name.");
            } else {
                this.session.getConsole().println("Please specify container name(s).");
            }
        } else {
            this.session.getConsole().println("The list of container names: " + expandedNames.toString());
        }

        return expandedNames;
    }

}
