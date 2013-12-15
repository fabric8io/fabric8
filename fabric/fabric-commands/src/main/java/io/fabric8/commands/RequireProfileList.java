/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.fabric8.commands;

import org.apache.felix.gogo.commands.Command;
import io.fabric8.api.FabricRequirements;
import io.fabric8.api.ProfileRequirements;
import io.fabric8.commands.support.RequirementsListSupport;

import java.io.PrintStream;
import java.util.List;

@Command(name = "require-profile-list", scope = "fabric", description = "Lists the requirements for profiles in the fabric", detailedDescription = "classpath:status.txt")
public class RequireProfileList extends RequirementsListSupport {

    @Override
    protected void printRequirements(PrintStream out, FabricRequirements requirements) {
        out.println(String.format("%-40s %-14s %-14s %s", "[profile]", "[# minimum]", "[# maximum]", "[depends on]"));
        List<ProfileRequirements> profileRequirements = requirements.getProfileRequirements();
        for (ProfileRequirements profile : profileRequirements) {
            out.println(String.format("%-40s %-14s %-14s %s", profile.getProfile(),
                    getStringOrBlank(profile.getMinimumInstances()),
                    getStringOrBlank(profile.getMaximumInstances()),
                    getStringOrBlank(profile.getDependentProfiles())));
        }
    }

    protected Object getStringOrBlank(Object value) {
        if (value == null) {
            return "";
        }
        else {
            return value.toString();
        }
    }
}
