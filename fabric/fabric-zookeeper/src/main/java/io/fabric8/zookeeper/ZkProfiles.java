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
package io.fabric8.zookeeper;

import java.util.regex.Pattern;

public final class ZkProfiles {

	private static final Pattern ENSEMBLE_PROFILE_PATTERN = Pattern.compile("fabric-ensemble-[0-9]+|fabric-ensemble-[0-9]+-[0-9]+");

	private ZkProfiles() {
		//Utility Class
	}

	/**
	 * Returns the path of the profile.
	 * @param version
	 * @param id
	 * @return
	 */
	public static String getPath(String version, String id) {
		if (ENSEMBLE_PROFILE_PATTERN.matcher(id).matches()) {
			return ZkPath.CONFIG_ENSEMBLE_PROFILE.getPath(id);
		} else return ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, id);
	}
}
