/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.commands;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Map;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Profile;

@Command(name = "list-profiles", scope = "fabric", description = "List existing profiles")
public class ListProfiles extends FabricCommand {

    @Option(name = "--version")
    private String version = "base";

    @Argument(index = 0)
    private String name;


    @Override
    protected Object doExecute() throws Exception {
        Map<String, Profile> profiles = profileService.getProfiles(version);
        printProfiles(profiles.values(), System.out);
        return null;
    }

    protected void printProfiles(Collection<Profile> profiles, PrintStream out) {
        out.println(String.format("%-30s %s", "[id]", "[parents]"));
        for (Profile profile : profiles) {
            out.println(String.format("%-30s %s", profile.getName(), toString(profile.getParents())));
        }
    }

}
