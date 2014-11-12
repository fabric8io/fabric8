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
import io.fabric8.api.Version;
import io.fabric8.api.VersionBuilder;
import io.fabric8.api.VersionSequence;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;
import io.fabric8.api.gravia.IllegalStateAssertion;

@Command(name = VersionCreate.FUNCTION_VALUE, scope = VersionCreate.SCOPE_VALUE, description = VersionCreate.DESCRIPTION)
public class VersionCreateAction extends AbstractAction {

    @Option(name = "--parent", description = "The parent version. By default, use the latest version as the parent.")
    private String parentVersion;
    @Option(name = "--default", description = "Set the created version to be the new default version.")
    private Boolean defaultVersion;
    @Option(name = "--description", description = "The description notes of this version.")
    private String description;
    @Argument(index = 0, description = "The new version to create. If not specified, defaults to the next minor version.", required = false)
    private String versionId;

    private final FabricService fabricService;

    VersionCreateAction(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    @Override
    protected Object doExecute() throws Exception {
        
        String latestVersion = null;

        ProfileService profileService = fabricService.adapt(ProfileService.class);
        List<String> versions = profileService.getVersions();
        if (versions.size() > 0) {
            latestVersion = versions.get(versions.size() - 1);
        }
        
        if (versionId == null) {
            IllegalStateAssertion.assertNotNull(latestVersion, "Cannot default the new version name as there are no versions available");
            VersionSequence sequence = new VersionSequence(latestVersion);
            versionId = sequence.next().getName();
        }

        // TODO we maybe want to choose the version which is less than the 'name' if it was specified
        // e.g. if you create a version 1.1 then it should use 1.0 if there is already a 2.0
        
        String sourceId = null;
        if (parentVersion == null) {
            sourceId = latestVersion;
        } else {
            IllegalStateAssertion.assertTrue(profileService.hasVersion(parentVersion), "Cannot find parent version: " + parentVersion);
            sourceId = parentVersion;
        }
        
        Version targetVersion;
        if (sourceId != null) {
            Map<String, String> attributes = description != null ? Collections.singletonMap(Version.DESCRIPTION, description) : null;
            targetVersion = profileService.createVersionFrom(sourceId, versionId, attributes);
            System.out.println("Created version: " + versionId + " as copy of: " + sourceId);
        } else {
            VersionBuilder builder = VersionBuilder.Factory.create(versionId);
            if (description != null) {
                builder.addAttribute(Version.DESCRIPTION, description);
            }
            targetVersion = profileService.createVersion(builder.getVersion());
            System.out.println("Create version: " + versionId);
        }
        
        if (defaultVersion == Boolean.TRUE) {
            fabricService.setDefaultVersionId(targetVersion.getId());
        }

        return null;
    }
}
