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
import io.fabric8.api.DataStore;
import io.fabric8.api.FabricService;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.boot.commands.support.FabricCommand;

import java.util.Collection;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

@Command(name = ContainerDelete.FUNCTION_VALUE, scope = ContainerDelete.SCOPE_VALUE, description = ContainerDelete.DESCRIPTION, detailedDescription = "classpath:containerDelete.txt")
public class ContainerDeleteAction extends AbstractContainerLifecycleAction {

    protected final RuntimeProperties runtimeProperties;

    @Option(name = "-r", aliases = {"--recursive"}, multiValued = false, required = false, description = "Recursively stops and deletes all child containers")
    protected boolean recursive = false;

    ContainerDeleteAction(FabricService fabricService, RuntimeProperties runtimeProperties) {
        super(fabricService);
        this.runtimeProperties = runtimeProperties;
    }

    @Override
    protected Object doExecute() throws Exception {
        Collection<String> expandedNames = super.expandGlobNames(containers);
        for (String containerName : expandedNames) {
            validateContainerName(containerName);
            if (FabricCommand.isPartOfEnsemble(fabricService, containerName) && !force) {
                System.out.println("Container is part of the ensemble. If you still want to delete it, please use --force option.");
                return null;
            }

            String runtimeIdentity = runtimeProperties.getRuntimeIdentity();
            if (containerName.equals(runtimeIdentity) && !force) {
                System.out.println("You shouldn't delete current container. If you still want to delete it, please use --force option.");
                return null;
            }

            Container found = FabricCommand.getContainerIfExists(fabricService, containerName);
            if (found != null) {
                applyUpdatedCredentials(found);
                if (recursive || force) {
                    for (Container child : found.getChildren()) {
                        child.destroy(force);
                    }
                }
                found.destroy(force);
            } else if (force) {
                //We also want to try and delete any leftover entries
                fabricService.adapt(DataStore.class).deleteContainer(fabricService, containerName);
            }
        }
        return null;
    }

}
