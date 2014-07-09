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

import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileService;
import io.fabric8.api.Version;
import io.fabric8.boot.commands.support.FabricCommand;

import java.util.Arrays;
import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = "profile-change-parents", scope = "fabric", description = "Replace the profile's parents with the specified list of parents (where the parents are specified as a space-separated list)")
public class ProfileChangeParentsAction extends AbstractAction {

    @Option(name = "--version", description = "The profile version. Defaults to the current default version.")
    private String versionId;
    @Argument(index = 0, required = true, name = "profile", description = "Name of the profile.")
    private String profileId;
    @Argument(index = 1, name = "parents", description = "The list of new parent profiles.", required = true, multiValued = true)
    private List<String> parentIds;

    private final ProfileService profileService;
    private final FabricService fabricService;

    ProfileChangeParentsAction(FabricService fabricService) {
        this.profileService = fabricService.adapt(ProfileService.class);
        this.fabricService = fabricService;
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    @Override
    protected Object doExecute() throws Exception {
        Version version = versionId != null ? profileService.getRequiredVersion(versionId) : fabricService.getDefaultVersion();
        Profile profile = version.getRequiredProfile(profileId);
        
        // we can only change parents to existing profiles
        Profile[] parents = FabricCommand.getExistingProfiles(fabricService, version, parentIds);
        ProfileBuilder builder = ProfileBuilder.Factory.createFrom(profile);
        builder.setParents(Arrays.asList(parents));
        profileService.updateProfile(builder.getProfile());
        return null;
    }

}
