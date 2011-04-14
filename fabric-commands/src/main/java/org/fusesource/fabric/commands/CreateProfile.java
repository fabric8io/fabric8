/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.commands;

import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Profile;

@Command(name = "create-profile", scope = "fabric", description = "Create a new profile")
public class CreateProfile extends FabricCommand {

    @Option(name = "--version")
    private String version = "base";

    @Option(name = "--parents", multiValued = true, required = false)
    private List<String> parents;

    @Argument(index = 0)
    private String name;

    @Override
    protected Object doExecute() throws Exception {
        Profile[] parents = getProfiles(version, this.parents);
        Profile profile = profileService.createProfile(version, name);
        profile.setParents(parents);
        return null;
    }

}
