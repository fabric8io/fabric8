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
import io.fabric8.api.ProfileService;
import io.fabric8.api.Version;
import io.fabric8.utils.Strings;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = ProfileDelete.FUNCTION_VALUE, scope = ProfileDelete.SCOPE_VALUE, description = ProfileDelete.DESCRIPTION)
public class ProfileDeleteAction extends AbstractAction {

    @Option(name = "--version", description = "The profile version to delete. Defaults to the current default version.")
    private String versionId;
    @Option(name = "--force", description = "Force the removal of the profile from all assigned containers.")
    private boolean force;
    @Argument(index = 0, required = true, name = "profile", description = "Name of the profile to delete.")
    @CompleterValues(index = 0)
    private String name;

    private final FabricService fabricService;

    ProfileDeleteAction(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    @Override
    protected Object doExecute() throws Exception {
        // do not validate the name in case a profile was created somehow with invalid name

        ProfileService profileService = fabricService.adapt(ProfileService.class);
        Version version = versionId != null ? profileService.getRequiredVersion(versionId) : fabricService.getRequiredDefaultVersion();
        boolean deleted = false;
        for (Profile profile : version.getProfiles()) {
            String versionId = profile.getVersion();
            String profileId = profile.getId();
            if (name.equals(profileId)) {
                profileService.deleteProfile(fabricService, versionId, profileId, force);
                deleted = true;
            }
        }

        if (!deleted) {
            System.out.println("Profile " + name + " not found.");
        }
        return null;
    }

}
