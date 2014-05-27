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

import java.io.PrintStream;

import io.fabric8.api.FabricService;
import org.apache.felix.gogo.commands.Command;
import io.fabric8.api.Container;
import io.fabric8.api.Version;
import org.apache.karaf.shell.console.AbstractAction;

import static io.fabric8.commands.support.CommandUtils.countContainersByVersion;

@Command(name = "version-list", scope = "fabric", description = "List the existing versions")
public class VersionListAction extends AbstractAction {

    private static final String CONSOLE_FORMAT="%-15s %-9s %-14s %s";

    private final FabricService fabricService;

    VersionListAction(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    @Override
    protected Object doExecute() throws Exception {
        Container[] containers = getFabricService().getContainers();
        Version[] versions = getFabricService().getVersions();
        printVersions(containers, versions, getFabricService().getDefaultVersion(), System.out);
        return null;
    }

    protected void printVersions(Container[] containers, Version[] versions, Version defaultVersion, PrintStream out) {
        out.println(String.format(CONSOLE_FORMAT, "[version]", "[default]", "[# containers]", "[description]"));

        // they are sorted in the correct order by default
        for (Version version : versions) {
            boolean isDefault = defaultVersion.getId().equals(version.getId());
            int active = countContainersByVersion(containers, version);
            String description = version.getAttributes().get(Version.DESCRIPTION);
            out.println(String.format(CONSOLE_FORMAT, version.getId(), (isDefault ? "true" : "false"), active, (description != null ? description : "")));
        }
    }

}
