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

import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import io.fabric8.api.Container;
import io.fabric8.api.Profile;
import io.fabric8.boot.commands.support.FabricCommand;

import static io.fabric8.utils.FabricValidations.validateContainersName;
import static io.fabric8.utils.FabricValidations.validateProfileName;

@Command(name = "container-change-profile", scope = "fabric", description = "Replaces a container's profiles with the specified list of profiles")
public class ContainerChangeProfile extends FabricCommand {

    @Argument(index = 0, name = "container", description = "The container name", required = true, multiValued = false)
    private String container;
    
    @Argument(index = 1, name = "profiles", description = "The profiles to deploy into the container", required = true, multiValued = true)
    private List<String> profiles;

    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        validateContainersName(container);
        validateProfileName(profiles);

        Container cont = getContainer(container);
        Profile[] profs = getProfiles(cont.getVersion(), this.profiles);
        cont.setProfiles(profs);
        return null;
    }

}
