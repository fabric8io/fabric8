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

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import io.fabric8.api.Containers;
import io.fabric8.api.ProfileRequirements;
import io.fabric8.boot.commands.support.FabricCommand;

import java.io.PrintStream;

import static io.fabric8.utils.FabricValidations.validateProfileName;

@Command(name = "profile-scale", scope = "fabric", description = "Scales up or down the required number of instances of a profile (defaults to adding one container)")
public class ProfileScale extends FabricCommand {

    @Argument(index = 0, required = true, name = "profile", description = "The name of the profile to scale up or down.")
    @CompleterValues(index = 0)
    private String name;
    @Argument(index = 1, required = false, name = "count", description = "The number of instances to increase or decrease (defaults to +1).")
    private int count = 1;

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        validateProfileName(name);

        fabricService.scaleProfile(name, count);
        ProfileRequirements profileRequirements = fabricService.getRequirements().getOrCreateProfileRequirement(name);
        Integer minimumInstances = profileRequirements.getMinimumInstances();
        int size = Containers.containersForProfile(fabricService.getContainers(), name).size();
        PrintStream output = session.getConsole();
        output.println("Profile " + name + " " + (minimumInstances != null
                ? "now requires " + minimumInstances + " container(s)"
                : "does not require any containers") + " currently has " + size + " container(s) running");
        return null;
    }
}
