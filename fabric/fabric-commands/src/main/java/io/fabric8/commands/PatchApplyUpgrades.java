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

import java.util.Map;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.boot.commands.support.FabricCommand;

@Command(name = "patch-apply-upgrades", scope = "fabric", description = "Apply the given upgrades")
public class PatchApplyUpgrades extends FabricCommand {

    @Option(name = "--version", description = "Only apply upgrades for the given version")
    private String version;

    @Option(name = "--profile", description = "Only apply upgrades for the given profile (if no version is specified, the default one is used")
    private String profile;

    @Argument
    private Map<String, String> upgrades;

    @Override
    protected Object doExecute() throws Exception {
        Version v = null;
        if (version != null && !version.isEmpty()) {
            v = fabricService.getVersion(version);
        }
        Profile p = null;
        if (profile != null && !profile.isEmpty()) {
            if (v == null) {
                v = fabricService.getDefaultVersion();
            }
            p = v.getProfile(profile);
        }

        if (p != null) {
            fabricService.getPatchService().applyUpgrades(p, upgrades);
        } else if (v != null) {
            fabricService.getPatchService().applyUpgrades(v, upgrades);
        } else {
            fabricService.getPatchService().applyUpgrades(upgrades);
        }

        return null;
    }

}
