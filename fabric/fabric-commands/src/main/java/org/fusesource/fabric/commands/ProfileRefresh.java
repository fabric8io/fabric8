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
package org.fusesource.fabric.commands;


import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.boot.commands.support.FabricCommand;
import org.fusesource.fabric.zookeeper.ZkDefs;

import java.util.HashMap;
import java.util.Map;

@Command(name = "profile-refresh", scope = "fabric", description = "Performs a change to the profile, that triggers the deployment agent. It's intended to be used for scanning for snapshot changes", detailedDescription = "classpath:profileRefresh.txt")
public class ProfileRefresh extends FabricCommand {

	@Argument(index = 0, name = "profile", description = "The target profile to edit", required = true, multiValued = false)
	private String profileName;

	@Argument(index = 1, name = "version", description = "The version of the profile to edit. Defaults to the current default version.", required = false, multiValued = false)
	private String versionName = ZkDefs.DEFAULT_VERSION;

	@Override
	protected Object doExecute() throws Exception {
		checkFabricAvailable();
		Version version = versionName != null ? fabricService.getVersion(versionName) : fabricService.getDefaultVersion();
		Profile profile = fabricService.getProfile(version.getName(), profileName);
		if (profile == null) {
			throw new IllegalArgumentException("No profile found with name:" + profileName + " and version:" + version.getName());
		}
		Map<String, Map<String, String>> configuration = profile.getConfigurations();
		Map<String, String> agentConfiguration = configuration.get("org.fusesource.fabric.agent");
		if (agentConfiguration == null) {
			agentConfiguration = new HashMap<String, String>();
		}
		agentConfiguration.put("lastRefresh." + profileName, String.valueOf(System.currentTimeMillis()));
		profile.setConfigurations(configuration);
		return null;
	}
}
