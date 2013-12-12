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

import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.boot.commands.support.FabricCommand;
import static io.fabric8.utils.FabricValidations.validateProfileName;

@Command(name = "profile-create", scope = "fabric", description = "Create a new profile with the specified name and version", detailedDescription = "classpath:profileCreate.txt")
public class ProfileCreate extends FabricCommand {

    @Option(name = "--version", description = "The profile version. Defaults to the current default version.")
    private String version;
    @Option(name = "--parents", multiValued = true, required = false, description = "Optionally specifies one or multiple parent profiles. To specify multiple parent profiles, specify this flag multiple times on the command line. For example, --parents foo --parents bar.")
    private List<String> parents;
    @Argument(index = 0)
    private String name;

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        validateProfileName(name);
        Version ver = version != null ? fabricService.getVersion(version) : fabricService.getDefaultVersion();
        
        Profile[] parents = getProfiles(ver, this.parents);
        Profile profile = fabricService.getVersion(ver.getId()).createProfile(name);
        profile.setParents(parents);
        return null;
    }

}
