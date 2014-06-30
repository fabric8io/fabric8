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

import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.boot.commands.support.FabricCommand;
import io.fabric8.utils.FabricValidations;

@Command(name = ContainerChangeProfile.FUNCTION_VALUE, scope = ContainerChangeProfile.SCOPE_VALUE, description = ContainerChangeProfile.DESCRIPTION)
public class ContainerChangeProfileAction extends AbstractAction {

    @Argument(index = 0, name = "container", description = "The container name", required = true, multiValued = false)
    private String container;

    @Argument(index = 1, name = "profiles", description = "The profiles to deploy into the container", required = true, multiValued = true)
    private List<String> profiles;

    private final FabricService fabricService;

    ContainerChangeProfileAction(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    protected Object doExecute() throws Exception {
        FabricValidations.validateContainerName(container);
        FabricValidations.validateProfileNames(profiles);

        Container cont = FabricCommand.getContainer(fabricService, container);
        // we can only change to existing profiles
        Profile[] profs = FabricCommand.getExistingProfiles(fabricService, cont.getVersion(), profiles);
        cont.setProfiles(profs);
        return null;
    }

}
