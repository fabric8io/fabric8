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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.commands.support.FabricCommand;

@Command(name = "profile-list", scope = "fabric", description = "List existing profiles")
public class ProfileList extends FabricCommand {

    @Option(name = "--version")
    private String version;

    @Override
    protected Object doExecute() throws Exception {
        Version ver = version != null ? fabricService.getVersion(version) : fabricService.getDefaultVersion();
        Profile[] profiles = ver.getProfiles();
        // we want the list to be sorted
        profiles = sortProfiles(profiles);
        printProfiles(profiles, System.out);
        return null;
    }

    protected void printProfiles(Profile[] profiles, PrintStream out) {
        out.println(String.format("%-40s %-14s %s", "[id]", "[# containers]", "[parents]"));
        for (Profile profile : profiles) {
            int active = profile.getAssociatedContainers().length;
            out.println(String.format("%-40s %-14s %s", profile.getId(), active, toString(profile.getParents())));
        }
    }

    private static Profile[] sortProfiles(Profile[] profiles) {
        if (profiles == null || profiles.length <= 1) {
            return profiles;
        }
        List<Profile> list = new ArrayList<Profile>(profiles.length);
        list.addAll(Arrays.asList(profiles));

        Collections.sort(list, new Comparator<Profile>() {
            @Override
            public int compare(Profile p1, Profile p2) {
                return p1.getId().compareTo(p2.getId());
            }
        });

        return list.toArray(new Profile[0]);
    }

}
