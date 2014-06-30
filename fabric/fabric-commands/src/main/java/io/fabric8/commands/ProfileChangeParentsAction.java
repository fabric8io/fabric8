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

import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.boot.commands.support.FabricCommand;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = "profile-change-parents", scope = "fabric", description = "Replace the profile's parents with the specified list of parents (where the parents are specified as a space-separated list)")
public class ProfileChangeParentsAction extends AbstractAction {

    @Option(name = "--version", description = "The profile version. Defaults to the current default version.")
    private String version;
    @Argument(index = 0, required = true, name = "profile", description = "Name of the profile.")
    private String name;
    @Argument(index = 1, name = "parents", description = "The list of new parent profiles.", required = true, multiValued = true)
    private List<String> parents;

    private final FabricService fabricService;

    ProfileChangeParentsAction(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    @Override
    protected Object doExecute() throws Exception {
        Version ver = version != null ? fabricService.getVersion(version) : fabricService.getDefaultVersion();

        Profile prof = FabricCommand.getProfile(ver, name);
        // we can only change parents to existing profiles
        Profile[] profs = FabricCommand.getExistingProfiles(fabricService, ver, parents);
        prof.setParents(profs);
        return null;
    }

}
