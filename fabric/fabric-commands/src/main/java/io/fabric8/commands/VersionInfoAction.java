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
import io.fabric8.api.Profile;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.Version;
import io.fabric8.commands.support.CommandUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = VersionInfo.FUNCTION_VALUE, scope = VersionInfo.SCOPE_VALUE, description = VersionInfo.DESCRIPTION)
public class VersionInfoAction extends AbstractAction {

    static final String FORMAT = "%-30s %s";

    @Argument(index = 0, name = "version", description = "The version name.", required = false, multiValued = false)
    private String versionName;

    private final FabricService fabricService;
    private final RuntimeProperties runtimeProperties;

    VersionInfoAction(FabricService fabricService, RuntimeProperties runtimeProperties) {
        this.fabricService = fabricService;
        this.runtimeProperties = runtimeProperties;
    }

    @Override
    protected Object doExecute() throws Exception {
        if (!versionExists(versionName)) {
            System.out.println("Container " + versionName + " does not exists!");
            return null;
        }
        Version version = fabricService.getVersion(versionName);
        String description = version.getAttributes().get(Version.DESCRIPTION);

        StringBuilder sbContainers = new StringBuilder("");
        for (String c : fabricService.getDataStore().getContainers()) {
            if (version.getId().equals(fabricService.getContainer(c).getVersion().getId())) {
                sbContainers.append(c);
                sbContainers.append(", ");
            }
        }
        String containers = sbContainers.toString();
        if (containers.endsWith(", ")) {
            containers = containers.substring(0, containers.lastIndexOf(", "));
        }

        boolean defaultVersion = version.getId().equals(fabricService.getDefaultVersion().getId());

        Profile[] profiles = CommandUtils.sortProfiles(version.getProfiles());
        StringBuilder sbProfiles = new StringBuilder("");
        for (int i = 0; i < profiles.length; i++) {
            if (i != 0) {
                sbProfiles.append(", ");
            }
            sbProfiles.append(profiles[i].getId());

        }

        System.out.println(String.format(FORMAT, "Name:", version.getId()));
        System.out.println(String.format(FORMAT, "Description:", (description != null ? description : "")));
        System.out.println(String.format(FORMAT, "Default Version:", defaultVersion));
        System.out.println(String.format(FORMAT, "Containers:", containers));
        System.out.println(String.format(FORMAT, "Profiles:", sbProfiles.toString()));

        return null;
    }

    private boolean versionExists(String versionName) {
        Version[] versions = fabricService.getVersions();
        for (Version v : versions) {
            if (versionName.equals(v.getId())) {
                return true;
            }
        }
        return false;
    }
}
