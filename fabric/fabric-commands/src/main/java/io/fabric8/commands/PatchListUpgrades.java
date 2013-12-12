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
import java.util.Set;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.boot.commands.support.FabricCommand;

@Command(name = "patch-list-upgrades", scope = "fabric", description = "Display the list of possible patch upgrades")
public class PatchListUpgrades extends FabricCommand {

    @Option(name = "--version", description = "Only list upgrades for the given version")
    private String version;

    @Option(name = "--profile", description = "Only list upgrades for the given profile (if no version is specified, the default one is used")
    private String profile;

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

        Map<String, Set<String>> upgrades;
        if (p != null) {
            upgrades = fabricService.getPatchService().getPossibleUpgrades(p);
        } else if (v != null) {
            upgrades = fabricService.getPatchService().getPossibleUpgrades(v);
        } else {
            upgrades = fabricService.getPatchService().getPossibleUpgrades();
        }
        for (Map.Entry<String, Set<String>> entry : upgrades.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                System.out.println(entry.getKey());
                for (String version : entry.getValue()) {
                    System.out.println("\t" + version);
                }
            }
        }
        return null;
    }

}
