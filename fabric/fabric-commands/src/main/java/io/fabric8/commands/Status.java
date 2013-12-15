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
import io.fabric8.api.*;
import io.fabric8.boot.commands.support.FabricCommand;

import java.io.PrintStream;
import java.util.*;

@Command(name = "status", scope = "fabric", description = "Displays the current status of the fabric by comparing the requirements to the actual instance counts", detailedDescription = "classpath:status.txt")
public class Status extends FabricCommand {

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        PrintStream out = System.out;
        FabricStatus status = getFabricService().getFabricStatus();
        printStatus(out, status);
        return null;
    }

    protected void printStatus(PrintStream out, FabricStatus status) {
        out.println(String.format("%-40s %-14s %s", "[profile]", "[instances]", "[health]"));

        Collection<ProfileStatus> statuses = status.getProfileStatusMap().values();
        for (ProfileStatus profile : statuses) {
            String id = profile.getProfile();
            int instances = profile.getCount();

            Integer minimum = profile.getMinimumInstances();
            Integer maximum = profile.getMaximumInstances();
            double health = profile.getHealth(instances);

            out.println(String.format("%-40s %-14s %s", id, instances, percentText(health)));
        }
    }

}
