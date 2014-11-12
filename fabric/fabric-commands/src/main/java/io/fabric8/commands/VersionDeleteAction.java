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

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;

import io.fabric8.api.Container;
import io.fabric8.api.Version;
import io.fabric8.api.ProfileService;

import org.apache.karaf.shell.console.AbstractAction;
import io.fabric8.api.gravia.IllegalStateAssertion;

@Command(name = VersionDelete.FUNCTION_VALUE, scope = VersionDelete.SCOPE_VALUE, description = VersionDelete.DESCRIPTION)
public class VersionDeleteAction extends AbstractAction {

    @Argument(index = 0, name = "version", description = "The version to delete", required = true, multiValued = false)
    private String versionId;

    private final ProfileService profileService;
    private final FabricService fabricService;

    VersionDeleteAction(FabricService fabricService) {
        this.profileService = fabricService.adapt(ProfileService.class);
        this.fabricService = fabricService;
    }

    @Override
    protected Object doExecute() throws Exception {
        Version version = profileService.getRequiredVersion(versionId);
        
        StringBuilder sb = new StringBuilder();
        for (Container container : fabricService.getContainers()) {
            if (version.equals(container.getVersion())) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(container.getId());
            }
        }
        IllegalStateAssertion.assertTrue(sb.length() == 0, "Version " + versionId + " is still in used by the following containers: " + sb.toString());
        
        profileService.deleteVersion(versionId);
        return null;
    }
}
