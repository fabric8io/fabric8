/*
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

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import io.fabric8.api.Version;
import io.fabric8.boot.commands.support.FabricCommand;

@Command(name = "version-set-default", scope = "fabric", description = "Set the new default version (must be one of the existing versions)", detailedDescription = "classpath:versionSetDefault.txt")
public class VersionSetDefault extends FabricCommand {

    @Argument(index = 0, description = "Version number to use as new default version.", required = true)
    private String versionName;

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();

        Version version = fabricService.getVersion(versionName);
        if (version == null) {
            throw new IllegalArgumentException("Cannot find version: " + versionName);
        }

        Version currentDefault = fabricService.getDefaultVersion();
        if (version.compareTo(currentDefault) == 0) {
            System.out.println("Version " + version + " is already default version.");
        } else {
            fabricService.setDefaultVersion(version);
            System.out.println("Changed default version to " + version);
        }

        return null;
    }
}
