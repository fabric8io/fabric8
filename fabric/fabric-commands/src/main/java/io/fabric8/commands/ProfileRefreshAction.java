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
import io.fabric8.api.Version;
import io.fabric8.utils.FabricValidations;
import io.fabric8.zookeeper.ZkDefs;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = "profile-refresh", scope = "fabric", description = "Performs a change to the profile, that triggers the deployment agent. It's intended to be used for scanning for snapshot changes", detailedDescription = "classpath:profileRefresh.txt")
public class ProfileRefreshAction extends AbstractAction {

	@Argument(index = 0, name = "profile", description = "The target profile to edit", required = true, multiValued = false)
	private String profileName;

	@Argument(index = 1, name = "version", description = "The version of the profile to edit. Defaults to the current default version.", required = false, multiValued = false)
	private String versionName = ZkDefs.DEFAULT_VERSION;

    private final FabricService fabricService;

    ProfileRefreshAction(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public FabricService getFabricService() {
        return fabricService;
    }

	@Override
	protected Object doExecute() throws Exception {
		FabricValidations.validateProfileName(profileName);
		Version version = versionName != null ? fabricService.getVersion(versionName) : fabricService.getDefaultVersion();
		Profile profile = version.getProfile(profileName);
		if (profile == null) {
			throw new IllegalArgumentException("No profile found with name:" + profileName + " and version:" + version.getId());
		}
        profile.refresh();
		return null;
	}
}
