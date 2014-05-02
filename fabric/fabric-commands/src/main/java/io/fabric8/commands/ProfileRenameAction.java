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
import io.fabric8.api.Version;
import io.fabric8.utils.FabricValidations;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = "profile-rename", scope = "fabric", description = "Rename the specified version of the source profile (where the version defaults to the current default version)")
public class ProfileRenameAction extends AbstractAction {

    @Option(name = "--version", description = "The profile version to rename. Defaults to the current default version.")
    private String version;

    @Option(name = "-f", aliases = "--force", description = "Flag to allow replacing the target profile (if exists).")
    private boolean force;

    @Argument(index = 0, required = true, name = "profile name", description = "Name of the profile.")
    @CompleterValues(index = 0)
    private String profileName;

    @Argument(index = 1, required = true, name = "new profile name", description = "New name of the profile.")
    @CompleterValues(index = 1)
    private String newName;

    private final FabricService fabricService;

    ProfileRenameAction(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    @Override
    protected Object doExecute() throws Exception {
        FabricValidations.validateProfileName(profileName);
        FabricValidations.validateProfileName(newName);
        Version ver = version != null ? fabricService.getVersion(version) : fabricService.getDefaultVersion();

        if (!ver.hasProfile(profileName)) {
            System.out.println("Profile " + profileName + " not found.");
            return null;
        } else if (ver.hasProfile(newName)) {
            if (!force) {
                System.out.println("New name " + newName + " already exists. Use --force if you want to overwrite.");
                return null;
            }
        }

        ver.renameProfile(profileName, newName, force);
        return null;
    }

}
