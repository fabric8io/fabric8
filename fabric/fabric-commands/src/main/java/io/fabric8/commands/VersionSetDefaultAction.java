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
import io.fabric8.api.ProfileService;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = "version-set-default", scope = "fabric", description = "Set the new default version (must be one of the existing versions)", detailedDescription = "classpath:versionSetDefault.txt")
public class VersionSetDefaultAction extends AbstractAction {

    @Argument(index = 0, description = "Version number to use as new default version.", required = true)
    private String versionId;

    private final FabricService fabricService;
    private final ProfileService profileService;

    VersionSetDefaultAction(FabricService fabricService) {
        this.fabricService = fabricService;
        this.profileService = fabricService.adapt(ProfileService.class);
    }

    @Override
    protected Object doExecute() throws Exception {
        profileService.getRequiredVersion(versionId);
        String defaultId = fabricService.getDefaultVersionId();
        if (versionId.compareTo(defaultId) == 0) {
            System.out.println("Version " + versionId + " is already default version.");
        } else {
            fabricService.setDefaultVersionId(versionId);
            System.out.println("Changed default version to " + versionId);
        }

        return null;
    }
}
