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

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.boot.commands.support.FabricCommand;

import static org.fusesource.fabric.utils.FabricValidations.validateContainersName;

@Command(name = "container-delete", scope = "fabric", description = "Stops and deletes an existing container", detailedDescription = "classpath:containerDelete.txt")
public class ContainerDelete extends FabricCommand {

    @Argument(index = 0, description = "The name of the container to delete.", multiValued = false, required = true)
    private String name;

    @Option(name = "-r", aliases = {"--recursive"}, multiValued = false, required = false, description = "Recursively stops and deletes all child containers")
    protected Boolean recursive = Boolean.FALSE;

    @Option(name = "-f", aliases = {"--force"}, multiValued = false, required = false, description = "Forces the deletion of the container.")
    protected Boolean force = Boolean.FALSE;

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        validateContainersName(name);
        if (isPartOfEnsemble(name) && !force) {
            System.out.println("Container is part of the ensemble. If you still want to delete it, please use -f option.");
            return null;
        }

        Container found = getContainer(name);
        if (recursive) {
            for (Container child : found.getChildren()) {
                child.stop();
                child.destroy();
            }
        }
        found.stop();
        found.destroy();
        return null;
    }

}
