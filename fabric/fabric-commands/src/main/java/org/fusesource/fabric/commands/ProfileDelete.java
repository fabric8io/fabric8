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
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.commands.support.FabricCommand;

@Command(name = "profile-delete", scope = "fabric", description = "Delete an existing profile")
public class ProfileDelete extends FabricCommand {

    @Option(name = "--version")
    private String version = "base";

    @Argument(index = 0, required = true, name = "profile")
    @CompleterValues(index = 0)
    private String name;

    @Override
    protected Object doExecute() throws Exception {
        Version version = fabricService.getVersion(this.version);

        for (Profile profile : version.getProfiles()) {
            if (name.equals(profile.getId())) {
                profile.delete();
            }
        }
        return null;
    }

}
