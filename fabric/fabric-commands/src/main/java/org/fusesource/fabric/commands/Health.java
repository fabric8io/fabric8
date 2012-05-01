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
import org.fusesource.fabric.api.*;
import org.fusesource.fabric.boot.commands.support.FabricCommand;
import org.fusesource.fabric.service.FabricServiceImpl;
import org.fusesource.fabric.zookeeper.utils.RegexSupport;
import org.linkedin.zookeeper.client.IZKClient;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Pattern;

import static org.fusesource.fabric.commands.support.CommandUtils.sortProfiles;
import static org.fusesource.fabric.zookeeper.utils.RegexSupport.*;

@Command(name = "health", scope = "fabric", description = "Displays the current health of the fabric by comparing the requirements to the actual instance counts", detailedDescription = "classpath:heath.txt")
public class Health extends FabricCommand {

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        PrintStream out = System.out;
        FabricRequirements requirements = fabricService.getRequirements();
        if (requirements == null) {
            out.println("No requirements are defined for this fabric. Please create a requirements JSON file in " + FabricServiceImpl.requirementsJsonPath);
        } else {
            printHeath(out, requirements);
        }
        return null;
    }

    protected void printHeath(PrintStream out, FabricRequirements requirements) {
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

    private int getOrZero(Integer counter) {
        return counter != null ? counter.intValue() : 0;
    }

    protected String percentText(double value) {
        return NumberFormat.getPercentInstance().format(value);
    }

    public Map<String,Integer> getProfileInstances() {
        Map<String,Integer> answer = new HashMap<String, Integer>();
        Container[] containers = getFabricService().getContainers();
        for (Container container : containers) {
            Profile[] profiles = container.getProfiles();
            for (Profile profile : profiles) {
                String key = profile.getId();
                int count = getOrZero(answer.get(key));
                answer.put(key, ++count);
            }
        }
        return answer;
    }
}
