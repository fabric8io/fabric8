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
package io.fabric8.commands;

import java.io.PrintStream;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.boot.commands.support.FabricCommand;

import static io.fabric8.commands.support.CommandUtils.sortProfiles;

@Command(name = "profile-list", scope = "fabric", description = "Lists all profiles that belong to the specified version (where the version defaults to the current default version)")
public class ProfileList extends FabricCommand {

    @Option(name = "--version", description = "Specifies the version of the profiles to list. Defaults to the current default version.")
    private String version;

    @Option(name = "--hidden", description = "Display hidden profiles")
    private boolean hidden;

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        Version ver = version != null ? fabricService.getVersion(version) : fabricService.getDefaultVersion();
        Profile[] profiles = ver.getProfiles();
        // we want the list to be sorted
        profiles = sortProfiles(profiles);
        printProfiles(profiles, System.out);
        return null;
    }

    protected void printProfiles(Profile[] profiles, PrintStream out) {
        out.println(String.format("%-40s %-14s %s", "[id]", "[# containers]", "[parents]"));
        for (Profile profile : profiles) {
            if (hidden || !profile.isHidden()) {
                int active = profile.getAssociatedContainers().length;
                out.println(String.format("%-40s %-14s %s", profile.getId(), active, toString(profile.getParents())));
            }
        }
    }

}
