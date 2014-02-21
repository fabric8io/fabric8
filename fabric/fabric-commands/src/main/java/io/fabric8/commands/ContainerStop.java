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
import io.fabric8.api.Container;
import java.util.Collection;

import static io.fabric8.utils.FabricValidations.validateContainersName;

@Command(name = "container-stop", scope = "fabric", description = "Shut down an existing container", detailedDescription = "classpath:containerStop.txt")
public class ContainerStop extends ContainerLifecycleCommand {

    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        Collection<String> expandedNames = super.expandGlobNames(containers);
        for (String containerName: expandedNames) {
            validateContainersName(containerName);
            if (isPartOfEnsemble(containerName) && !force) {
                System.out.println("Container is part of the ensemble. If you still want to stop it, please use -f option.");
                return null;
            }

            Container found = getContainer(containerName);
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
