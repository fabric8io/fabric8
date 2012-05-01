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

import org.apache.felix.gogo.commands.Command;
import org.fusesource.fabric.api.*;
import org.fusesource.fabric.commands.support.RequirementsListSupport;

import java.io.PrintStream;
import java.util.*;

@Command(name = "status", scope = "fabric", description = "Displays the current status of the fabric by comparing the requirements to the actual instance counts", detailedDescription = "classpath:status.txt")
public class Status extends RequirementsListSupport {

    @Override
    protected void printRequirements(PrintStream out, FabricRequirements requirements) {
        out.println(String.format("%-40s %-14s %s", "[profile]", "[instances]", "[health]"));
        List<ProfileRequirements> profileRequirements = requirements.getProfileRequirements();

        Map<String, Integer> counts = getProfileInstances();

        for (ProfileRequirements profile : profileRequirements) {
            String id = profile.getProfile();
            Integer counter = counts.get(id);
            int instances = getOrZero(counter);

            Integer minimumInstances = profile.getMinimumInstances();
            double health = profile.getHealth(instances);

            out.println(String.format("%-40s %-14s %s", id, instances, percentText(health)));
        }
    }

}
