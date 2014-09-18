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

import static io.fabric8.commands.ProfileListAction.activeContainerCountText;
import static io.fabric8.commands.support.CommandUtils.countContainersByVersion;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.ProfileService;
import io.fabric8.api.Version;

import java.io.PrintStream;
import java.util.List;

import io.fabric8.utils.TablePrinter;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = VersionList.FUNCTION_VALUE, scope = VersionList.SCOPE_VALUE, description = VersionList.DESCRIPTION)
public class VersionListAction extends AbstractAction {

    private static final String CONSOLE_FORMAT="%-15s %-9s %-14s %s";

    private final ProfileService profileService;
    private final FabricService fabricService;

    VersionListAction(FabricService fabricService) {
        this.profileService = fabricService.adapt(ProfileService.class);
        this.fabricService = fabricService;
    }

    @Override
    protected Object doExecute() throws Exception {
        Container[] containers = fabricService.getContainers();
        List<String> versions = profileService.getVersions();
        printVersions(containers, versions, fabricService.getDefaultVersionId(), System.out);
        return null;
    }

    protected void printVersions(Container[] containers, List<String> versions, String defaultVersionId, PrintStream out) {
        TablePrinter table = new TablePrinter();
        table.columns("version", "default", "# containers", "description");

        // they are sorted in the correct order by default
        for (String versionId : versions) {
            boolean isDefault = versionId.equals(defaultVersionId);
            Version version = profileService.getRequiredVersion(versionId);
            int active = countContainersByVersion(containers, version);
            String description = version.getAttributes().get(Version.DESCRIPTION);
            table.row(version.getId(), (isDefault ? "true" : ""), activeContainerCountText(active), description);
        }
        table.print();
    }

}
