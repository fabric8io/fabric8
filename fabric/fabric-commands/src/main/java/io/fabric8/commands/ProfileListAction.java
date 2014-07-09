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

import static io.fabric8.commands.support.CommandUtils.sortProfiles;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileService;
import io.fabric8.api.Version;

import java.io.PrintStream;
import java.util.List;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = "profile-list", scope = "fabric", description = "Lists all profiles that belong to the specified version (where the version defaults to the current default version)")
public class ProfileListAction extends AbstractAction {

    @Option(name = "--version", description = "Specifies the version of the profiles to list. Defaults to the current default version.")
    private String version;

    @Option(name = "--hidden", description = "Display hidden profiles")
    private boolean hidden;

    private final FabricService fabricService;

    ProfileListAction(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    @Override
    protected Object doExecute() throws Exception {
        ProfileService profileService = fabricService.adapt(ProfileService.class);
        Version ver = version != null ? profileService.getVersion(version) : fabricService.getDefaultVersion();
        List<Profile> profiles = ver.getProfiles();
        profiles = sortProfiles(profiles);
        printProfiles(profileService, profiles, System.out);
        return null;
    }

    protected void printProfiles(ProfileService profileService, List<Profile> profiles, PrintStream out) {
        out.println(String.format("%-50s %-14s %s", "[id]", "[# containers]", "[parents]"));
        for (Profile profile : profiles) {
        	String versionId = profile.getVersion();
        	String profileId = profile.getId();
            // skip profiles that do not exists (they may have been deleted)
            if (profileService.hasProfile(versionId, profileId) && (hidden || !profile.isHidden())) {
                int active = fabricService.getAssociatedContainers(versionId, profileId).length;
                out.println(String.format("%-50s %-14s %s", profileId, active, profile.getParents()));
            }
        }
    }
}
