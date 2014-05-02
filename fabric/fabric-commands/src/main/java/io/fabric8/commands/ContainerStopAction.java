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

import static io.fabric8.utils.FabricValidations.validateContainerName;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.boot.commands.support.FabricCommand;

import java.util.Collection;

import org.apache.felix.gogo.commands.Command;

@Command(name = "container-stop", scope = "fabric", description = "Shut down an existing container", detailedDescription = "classpath:containerStop.txt")
public final class ContainerStopAction extends AbstractContainerLifecycleAction {

    ContainerStopAction(FabricService fabricService) {
        super(fabricService);
    }

    protected Object doExecute() throws Exception {
        Collection<String> expandedNames = super.expandGlobNames(containers);
        for (String containerName: expandedNames) {
            validateContainerName(containerName);
            if (!force && FabricCommand.isPartOfEnsemble(fabricService, containerName)) {
                System.out.println("Container is part of the ensemble. If you still want to stop it, please use --force option.");
                return null;
            }

            Container found = FabricCommand.getContainer(fabricService, containerName);
            applyUpdatedCredentials(found);
            if (found.isAlive()) {
                found.stop(force);
                this.session.getConsole().println("Container '" + found.getId() + "' stopped successfully.");
            } else {
                System.err.println("Container '" + found.getId() + "' already stopped.");
            }
        }
        return null;
    }

}
