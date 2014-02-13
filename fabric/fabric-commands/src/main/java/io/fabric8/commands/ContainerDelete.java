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
package io.fabric8.commands;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.api.Container;

import static io.fabric8.utils.FabricValidations.validateContainersName;

@Command(name = "container-delete", scope = "fabric", description = "Stops and deletes an existing container", detailedDescription = "classpath:containerDelete.txt")
public class ContainerDelete extends ContainerLifecycleCommand {

    @Option(name = "-r", aliases = {"--recursive"}, multiValued = false, required = false, description = "Recursively stops and deletes all child containers")
    protected boolean recursive = false;

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        validateContainersName(container);
        if (isPartOfEnsemble(container) && !force) {
            System.out.println("Container is part of the ensemble. If you still want to delete it, please use -f option.");
            return null;
        }

        Container found = getContainer(container);
        applyUpdatedCredentials(found);
        if (recursive || force) {
            for (Container child : found.getChildren()) {
                child.destroy(force);
            }
        }
        found.destroy(force);
        return null;
    }

}
