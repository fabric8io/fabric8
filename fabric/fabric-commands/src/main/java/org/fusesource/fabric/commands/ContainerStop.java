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

@Command(name = "container-stop", scope = "fabric", description = "Shut down an existing container")
public class ContainerStop extends FabricCommand {

    @Option(name = "-f", aliases = {"--force"}, multiValued = false, required = false, description = "Forces stopping of the container.")
    protected Boolean force = Boolean.FALSE;

    @Argument(index = 0, name = "container", description = "The container name", required = true, multiValued = false)
    private String container = null;

    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        validateContainersName(container);
        if (isPartOfEnsemble(container) && !force) {
            System.out.println("Container is part of the ensemble. If you still want to stop it, please use -f option.");
            return null;
        }

        Container found = getContainer(container);
        if (found.isAlive()) {
            found.stop();
        } else {
            System.err.println("Container " + container + " is already stopped");
        }
        return null;
    }

}
