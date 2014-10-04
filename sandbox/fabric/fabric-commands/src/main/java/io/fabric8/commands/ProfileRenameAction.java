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
import io.fabric8.api.Profiles;
import io.fabric8.api.Version;
import io.fabric8.api.ProfileService;
import io.fabric8.utils.FabricValidations;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = ProfileRename.FUNCTION_VALUE, scope = ProfileRename.SCOPE_VALUE, description = ProfileRename.DESCRIPTION)
public class ProfileRenameAction extends AbstractAction {

    @Option(name = "--version", description = "The profile version to rename. Defaults to the current default version.")
    private String versionId;

    @Option(name = "-f", aliases = "--force", description = "Flag to allow replacing the target profile (if exists).")
    private boolean force;

    @Argument(index = 0, required = true, name = "profile name", description = "Name of the profile.")
    @CompleterValues(index = 0)
    private String profileName;

    @Argument(index = 1, required = true, name = "new profile name", description = "New name of the profile.")
    @CompleterValues(index = 1)
    private String newName;

    private final ProfileService profileService;
    private final FabricService fabricService;

    ProfileRenameAction(FabricService fabricService) {
        this.profileService = fabricService.adapt(ProfileService.class);
        this.fabricService = fabricService;
    }

    @Override
    protected Object doExecute() throws Exception {
        // do not validate the old name in case a profile was created somehow with invalid name
        // but validate the new name
        try {
            FabricValidations.validateProfileName(newName);
        } catch (IllegalArgumentException e) {
            // we do not want exception in the server log, so print the error message to the console
            System.out.println(e.getMessage());
            return null;
        }

        Version version;
        if (versionId != null) {
            version = profileService.getRequiredVersion(versionId);
        } else {
            version = fabricService.getDefaultVersion();
        }
        if (!version.hasProfile(profileName)) {
            System.out.println("Profile " + profileName + " not found.");
            return null;
        } else if (version.hasProfile(newName)) {
            if (!force) {
                System.out.println("New name " + newName + " already exists. Use --force if you want to overwrite.");
                return null;
            }
        }

        Profiles.renameProfile(fabricService, versionId, profileName, newName, force);
        return null;
    }

}
