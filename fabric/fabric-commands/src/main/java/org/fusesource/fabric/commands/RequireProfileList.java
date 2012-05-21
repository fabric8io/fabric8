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
package org.fusesource.fabric.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricRequirements;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.ProfileRequirements;
import org.fusesource.fabric.boot.commands.support.FabricCommand;
import org.fusesource.fabric.commands.support.ChangeRequirementSupport;
import org.fusesource.fabric.commands.support.RequirementsListSupport;
import org.fusesource.fabric.service.FabricServiceImpl;

import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
