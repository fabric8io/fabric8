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

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileService;
import io.fabric8.api.Version;
import io.fabric8.commands.support.CommandUtils;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = VersionInfo.FUNCTION_VALUE, scope = VersionInfo.SCOPE_VALUE, description = VersionInfo.DESCRIPTION)
public class VersionInfoAction extends AbstractAction {

    static final String FORMAT = "%-30s %s";

    @Argument(index = 0, name = "version", description = "The version name.", required = true, multiValued = false)
    private String versionId;

    private final ProfileService profileService;
    private final FabricService fabricService;

    VersionInfoAction(FabricService fabricService) {
        this.profileService = fabricService.adapt(ProfileService.class);
        this.fabricService = fabricService;
    }

    @Override
    protected Object doExecute() throws Exception {
        if (!profileService.hasVersion(versionId)) {
            System.out.println("Version " + versionId + " does not exists!");
            return null;
        }
        Version version = profileService.getRequiredVersion(versionId);
        String description = version.getAttributes().get(Version.DESCRIPTION);
        String derivedFrom = null;
        boolean defaultVersion = version.getId().equals(fabricService.getDefaultVersion().getId());
        List<Profile> profiles = CommandUtils.sortProfiles(version.getProfiles());

        List<Container> containerList = new ArrayList<Container>();
        for (String c : fabricService.getDataStore().getContainers()) {
            Container container = fabricService.getContainer(c);
            if (version.getId().equals(container.getVersion().getId())) {
                containerList.add(container);
            }
        }
        Container[] containers = CommandUtils.sortContainers(containerList.toArray(new io.fabric8.api.Container[containerList.size()]));

        System.out.println(String.format(FORMAT, "Name:", version.getId()));
        System.out.println(String.format(FORMAT, "Description:", (description != null ? description : "")));
        System.out.println(String.format(FORMAT, "Derived From:", (derivedFrom) != null ? derivedFrom : ""));
        System.out.println(String.format(FORMAT, "Default Version:", defaultVersion));
        if (containers.length == 0) {
            System.out.println(String.format(FORMAT, "Containers:", ""));
        } else {
            for (int i = 0; i < containers.length; i++) {
                if (i == 0) {
                    System.out.println(String.format(FORMAT, "Containers (" + containers.length + "):", containers[i].getId()));
                } else {
                    System.out.println(String.format(FORMAT, "", containers[i].getId()));
                }
            }
        }
        if (profiles.size() == 0) {
            System.out.println(String.format(FORMAT, "Profiles:", ""));
        } else {
            for (int i = 0; i < profiles.size(); i++) {
                if (i == 0) {
                    System.out.println(String.format(FORMAT, "Profiles (" + profiles.size() + "):", profiles.get(i).getId()));
                } else {
                    System.out.println(String.format(FORMAT, "", profiles.get(i).getId()));
                }
            }
        }

        return null;
    }
}
