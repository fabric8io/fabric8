/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Command(name = "display-profile", scope = "fabric", description = "Displays profile information")
public class DisplayProfile extends FabricCommand {

    @Option(name = "--version")
    private String version = "base";

    @Option(name = "--overlay", aliases = "-o")
    private Boolean overlay = false;

    @Argument(index = 0)
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

    private void displayProfile(Profile profile) {
        PrintStream output = session.getConsole();

        output.println("Profile id: " + profile.getId());
        output.println("Version   : " + profile.getVersion());

        output.println("Parents   : " + toString(profile.getParents()) + "\n");

        Map<String, Map<String, String>> configuration = overlay ? profile.getOverlay().getConfigurations() : profile.getConfigurations();

        if (configuration.containsKey("org.fusesource.fabric.agent")) {
            output.println("\nAgent settings");
            Map<String, String> agentCfg = configuration.get("org.fusesource.fabric.agent");

            displayAgentConfig(agentCfg);

            configuration.remove("org.fusesource.fabric.agent");
        }

        output.println("\nConfiguration details");
        for (Map.Entry<String, Map<String, String>> cfg : configuration.entrySet()) {
            output.println("PID: " + cfg.getKey());

            for (Map.Entry<String, String> values : cfg.getValue().entrySet()) {
                output.println("  " + values.getKey() + " " + values.getValue());
            }
            output.println("\n");
        }
    }

    private Map<String, String> matchKeys(Map<String, String> cfg, String prefix) {
        Map<String, String> matched = new LinkedHashMap<String, String>();

        for (Map.Entry<String, String> entry : cfg.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                String key = entry.getKey().substring(prefix.length());

                if (entry.getValue() == null || entry.getValue().isEmpty()) {
                    matched.put(key, key);
                } else {
                    matched.put(key, entry.getValue());
                }
            }
        }
        return matched;
    }

    private void displayAgentConfig(Map<String, String> configuration) {
        PrintStream output = session.getConsole();

        Map<String, String> repositories = matchKeys(configuration, "repository.");
        Map<String, String> features = matchKeys(configuration, "feature.");
        Map<String, String> bundles = matchKeys(configuration, "bundle.");

        if (repositories.size() > 0) {
            output.println("\nRepositories:");
            for (String key : repositories.keySet()) {
                output.println(key + " " + repositories.get(key));
            }
        } else {
            output.println("\nNo repositories defined for profile");
        }

        if (features.size() > 0) {
            output.println("\nFeatures:");
            for (String key : features.keySet()) {
                output.println(key + " " + features.get(key));
            }
        } else {
            output.println("\nNo features defined for profile");
        }

        if (bundles.size() > 0) {
            output.println("\nBundles:");
            for (String key : bundles.keySet()) {
                output.println(key + " " + bundles.get(key));
            }
        } else {
            output.println("\nNo bundles defined for profile");
        }
    }

}
