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

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.boot.commands.support.FabricCommand;
import io.fabric8.utils.FabricValidations;

import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = ContainerRemoveProfile.FUNCTION_VALUE, scope = ContainerRemoveProfile.SCOPE_VALUE, description = ContainerRemoveProfile.DESCRIPTION)
public class ContainerRemoveProfileAction extends AbstractAction {

    @Argument(index = 0, name = "container", description = "The container name", required = true, multiValued = false)
    private String container;

    @Argument(index = 1, name = "profiles", description = "The profiles to remove from the container", required = true, multiValued = true)
    private List<String> profiles;

    private final FabricService fabricService;

    ContainerRemoveProfileAction(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    protected Object doExecute() throws Exception {
        FabricValidations.validateContainerName(container);
        //Do not validate profile names, because we want to be able to remove non-existent profiles.

        Container cont = FabricCommand.getContainer(fabricService, container);
        // allow to remove non-existing profiles
        Profile[] profs = FabricCommand.getProfiles(fabricService, cont.getVersion(), profiles);
        cont.removeProfiles(profs);
        return null;
    }
}
