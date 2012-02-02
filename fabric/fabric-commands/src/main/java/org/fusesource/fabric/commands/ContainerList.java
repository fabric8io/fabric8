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

import org.apache.felix.gogo.commands.Command;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.commands.support.FabricCommand;

import java.io.PrintStream;

@Command(name = "container-list", scope = "fabric", description = "List existing containers")
public class ContainerList extends FabricCommand {

    @Override
    protected Object doExecute() throws Exception {
        Container[] containers = fabricService.getContainers();
        printContainers(containers, System.out);
        return null;
    }

    protected void printContainers(Container[] containers, PrintStream out) {
        out.println(String.format("%-30s %-10s %-30s %-100s", "[id]", "[alive]", "[profiles]", "[provision status]"));
        for (Container container : containers) {
            if (container.isRoot()) {
                out.println(String.format("%-30s %-10s %-30s %-100s", container.getId(), container.isAlive(), toString(container.getProfiles()), container.getProvisionStatus()));
                for (Container child : containers) {
                    if (child.getParent() == container) {
                        out.println(String.format("%-30s %-10s %-30s %-100s", "  " + child.getId(), child.isAlive(), toString(child.getProfiles()), container.getProvisionStatus()));
                    }
                }
            }
        }
    }

}
