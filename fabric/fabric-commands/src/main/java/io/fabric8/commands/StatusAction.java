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
import java.text.NumberFormat;
import java.util.Collection;

import io.fabric8.api.FabricService;
import io.fabric8.api.FabricStatus;
import io.fabric8.api.ProfileStatus;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = "status", scope = "fabric", description = "Displays the current status of the fabric by comparing the requirements to the actual instance counts", detailedDescription = "classpath:status.txt")
public class StatusAction extends AbstractAction {

    private final FabricService fabricService;

    StatusAction(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    @Override
    protected Object doExecute() throws Exception {
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
            double health = profile.getHealth(instances);
            out.println(String.format("%-40s %-14s %s", id, instances, percentText(health)));
        }
    }

    String percentText(double value) {
        return NumberFormat.getPercentInstance().format(value);
    }

}
