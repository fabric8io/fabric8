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

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.commands.support.FabricCommand;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

@Command(name = "profile-display", scope = "fabric", description = "Displays profile information")
public class ProfileDisplay extends FabricCommand {

    @Option(name = "--version")
    private String version = "base";

    @Option(name = "--overlay", aliases = "-o")
    private Boolean overlay = false;

    @Argument(index = 0, required = true, name = "profile")
    @CompleterValues(index = 0)
    private String name;

    @Override
    protected Object doExecute() throws Exception {
        Version version = fabricService.getVersion(this.version);

        for (Profile profile : version.getProfiles()) {
            if (name.equals(profile.getId())) {
                displayProfile(profile);
            }
        }
        return null;
    }

    private String toString(Container[] containers) {
        StringBuffer rc = new StringBuffer();
        for (Container container : containers) {
            rc.append(container.getId());
            rc.append(" ");
        }
        return rc.toString().trim();
    }

    private static void printConfigList(String header, PrintStream out, List<String> list) {
        out.println(header);
        for (String str : list) {
            out.printf("\t%s\n", str);
        }
        out.println();
    }

    private void displayProfile(Profile profile) {
        PrintStream output = session.getConsole();

        output.println("Profile id: " + profile.getId());
        output.println("Version   : " + profile.getVersion());

        output.println("Parents   : " + toString(profile.getParents()));

        output.printf("Associated Containers : %s\n", toString(profile.getAssociatedContainers()));

        Map<String, Map<String, String>> configuration = overlay ? profile.getOverlay().getConfigurations() : profile.getConfigurations();

        if (configuration.containsKey(CONTAINER_PID)) {
            output.println("\nContainer settings");
            output.println("----------------------------");

            if (profile.getRepositories().size() > 0) {
                printConfigList("Repositories : ", output, profile.getRepositories());
            }
            if (profile.getFeatures().size() > 0) {
                printConfigList("Features : ", output, profile.getFeatures());
            }
            if (profile.getBundles().size() > 0) {
                printConfigList("Bundles : ", output, profile.getBundles());
            }
            configuration.remove(CONTAINER_PID);
        }

        output.println("\nConfiguration details");
        output.println("----------------------------");
        for (Map.Entry<String, Map<String, String>> cfg : configuration.entrySet()) {
            output.println("PID: " + cfg.getKey());

            for (Map.Entry<String, String> values : cfg.getValue().entrySet()) {
                output.println("  " + values.getKey() + " " + values.getValue());
            }
            output.println("\n");
        }
    }

}
