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
package org.fusesource.fabric.commands;

import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.commands.support.FabricCommand;
import org.fusesource.fabric.zookeeper.ZkDefs;

@Command(name = "profile-create", scope = "fabric", description = "Create a new profile")
public class ProfileCreate extends FabricCommand {

    @Option(name = "--version")
    private String version = ZkDefs.DEFAULT_VERSION;

    @Option(name = "--parents", multiValued = true, required = false)
    private List<String> parents;

    @Argument(index = 0)
    private String name;

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        Profile[] parents = getProfiles(version, this.parents);
        Profile profile = fabricService.getVersion(version).createProfile(name);
        profile.setParents(parents);
        return null;
    }

}
