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
import org.apache.felix.gogo.commands.Option;
import io.fabric8.api.Version;
import io.fabric8.boot.commands.support.FabricCommand;

@Command(name = "version-create", scope = "fabric", description = "Create a new version, copying all of the profiles from the current latest version into the new version")
public class VersionCreate extends FabricCommand {

    @Option(name = "--parent", description = "The parent version. By default, use the latest version as the parent.")
    private String parentVersion;
    @Option(name = "--default", description = "Set the created version to be the new default version.")
    private Boolean defaultVersion;
    @Argument(index = 0, description = "The new version to create. If not specified, defaults to the next minor version.",  required = false)
    private String name;

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();

        Version latestVersion = null;

        Version[] versions = fabricService.getVersions();
        int vlength = versions.length;
        if (vlength > 0) {
            latestVersion = versions[vlength - 1];
        }
        if (name == null) {
            if (latestVersion == null) {
                throw new IllegalArgumentException("Cannot default the new version name as there are no versions available");
            }
            name = latestVersion.getSequence().next().getName();
        }

        Version parent;
        if (parentVersion == null) {
            parent = latestVersion;
            // TODO we maybe want to choose the version which is less than the 'name' if it was specified
            // e.g. if you create a version 1.1 then it should use 1.0 if there is already a 2.0
        } else {
            parent = fabricService.getVersion(parentVersion);
            if (parent == null) {
                throw new IllegalArgumentException("Cannot find parent version: " + parentVersion);
            }
        }
        
        Version created;
        if (parent != null) {
            created = fabricService.createVersion(parent, name);
            System.out.println("Created version: " + name + " as copy of: " + parent.getId());
        } else {
            created = fabricService.createVersion(name);
            System.out.println("Created version: " + name);
        }
        
        if (defaultVersion != null && defaultVersion == true) {
            fabricService.setDefaultVersion(created);
        }
        
        return null;
    }
}
