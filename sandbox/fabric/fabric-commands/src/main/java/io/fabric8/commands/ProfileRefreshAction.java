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
import io.fabric8.api.Profiles;
import io.fabric8.api.Version;
import io.fabric8.api.ZkDefs;
import io.fabric8.utils.FabricValidations;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = ProfileRefresh.FUNCTION_VALUE, scope = ProfileRefresh.SCOPE_VALUE, description = ProfileRefresh.DESCRIPTION, detailedDescription = "classpath:profileRefresh.txt")
public class ProfileRefreshAction extends AbstractAction {

	@Argument(index = 0, name = "profile", description = "The target profile to edit", required = true, multiValued = false)
	private String profileName;

	@Argument(index = 1, name = "version", description = "The version of the profile to edit. Defaults to the current default version.", required = false, multiValued = false)
	private String versionId = ZkDefs.DEFAULT_VERSION;

    private final FabricService fabricService;

    ProfileRefreshAction(FabricService fabricService) {
        this.fabricService = fabricService;
    }

	@Override
	protected Object doExecute() throws Exception {
        ProfileService profileService = fabricService.adapt(ProfileService.class);
		Version version = versionId != null ? profileService.getRequiredVersion(versionId) : fabricService.getRequiredDefaultVersion();
		Profile profile = version.getProfile(profileName);
        if (profile != null) {
            Profiles.refreshProfile(fabricService, profile);
        } else {
            System.out.println("Profile " + profileName + " not found.");
        }
		return null;
	}
}
