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
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.boot.commands.support.FabricCommand;
import static org.fusesource.fabric.utils.FabricValidations.validateProfileName;

@Command(name = "profile-display", scope = "fabric", description = "Displays information about the specified version of the specified profile (where the version defaults to the current default version)")
public class ProfileDisplay extends FabricCommand {

    @Option(name = "--version", description = "Select a specific profile version. Defaults to the current default version.")
    private String version;
    @Option(name = "--overlay", aliases = "-o", description = "Shows the effective profile settings, taking into account the settings inherited from parent profiles.")
    private Boolean overlay = false;
    @Argument(index = 0, required = true, name = "profile", description = "The name of the profile.")
    @CompleterValues(index = 0)
    private String name;

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        validateProfileName(name);
        Version ver = version != null ? fabricService.getVersion(version) : fabricService.getDefaultVersion();

        for (Profile profile : ver.getProfiles()) {
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

    private void displayProfile(Profile p) {
        PrintStream output = session.getConsole();

        output.println("Profile id: " + p.getId());
        output.println("Version   : " + p.getVersion());

        output.println("Attributes: ");
        Properties props = p.getAttributes();
        for (String key : props.stringPropertyNames()) {
            output.println("\t" + key + ": " + props.getProperty(key));
        }

        output.printf("Containers: %s\n", toString(p.getAssociatedContainers()));

        Profile profile = overlay ? p.getOverlay() : p;

        Map<String, Map<String, String>> configuration = profile.getConfigurations();
        Map<String,String> agentConfiguration = profile.getContainerConfiguration();
        List<String> agentProperties = new ArrayList<String>();
        List<String> systemProperties = new ArrayList<String>();
        List<String> configProperties = new ArrayList<String>();
        for (Map.Entry<String, String> entry : agentConfiguration.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value.contains(",")) {
                value = "\t" + value.replace(",", ",\n\t\t");
            }

            if (key.startsWith("system.")) {
                systemProperties.add("  " + key.substring("system.".length()) + " = " + value);
            }
            else if (key.startsWith("config.")) {
                configProperties.add("  " + key.substring("config.".length()) + " = " + value);
            }
            else if (!key.startsWith("feature.") && !key.startsWith("repository") &&
                        !key.startsWith("bundle.") && !key.startsWith("fab.") &&
                        !key.startsWith("override.")) {
                agentProperties.add("  " + key + " = " + value);
            }
        }

        if (configuration.containsKey(AGENT_PID)) {
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
            if (profile.getFabs().size() > 0) {
                printConfigList("Fabs : ", output, profile.getFabs());
            }
            if (profile.getOverrides().size() > 0) {
                printConfigList("Overrides : ", output, profile.getOverrides());
            }

            if (agentProperties.size() > 0) {
                printConfigList("Agent Properties : ", output, agentProperties);
            }

            if (systemProperties.size() > 0) {
                printConfigList("System Properties : ", output, systemProperties);
            }

            if (configProperties.size() > 0) {
                printConfigList("Config Properties : ", output, configProperties);
            }

            configuration.remove(AGENT_PID);
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
