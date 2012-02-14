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

import java.io.PrintStream;

import org.apache.felix.gogo.commands.Command;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.commands.support.FabricCommand;

@Command(name = "version-list", scope = "fabric", description = "List existing versions")
public class VersionList extends FabricCommand {

    @Override
    protected Object doExecute() throws Exception {
        getZooKeeper().checkConnected(0L);
        Container[] containers = fabricService.getContainers();
        Version[] versions = fabricService.getVersions();
        printVersions(containers, versions, fabricService.getDefaultVersion(), System.out);
        return null;
    }

    protected void printVersions(Container[] containers, Version[] versions, Version defaultVersion, PrintStream out) {
        out.println(String.format("%-15s %-9s %-14s", "[version]", "[default]", "[# containers]"));

        // they are sorted in the correct order by default
        for (Version version : versions) {
            boolean isDefault = defaultVersion.getName().equals(version.getName());
            int active = countActiveContainers(containers, version);
            out.println(String.format("%-15s %-9s %-14s", version.getName(), (isDefault ? "true" : "false"), active));
        }
    }
    
    private static int countActiveContainers(Container[] containers, Version version) {
        int answer = 0;
        for (Container container : containers) {
            if (container.getVersion().getName().equals(version.getName())) {
                answer++;
            }
        }
        return answer;
    }
}
