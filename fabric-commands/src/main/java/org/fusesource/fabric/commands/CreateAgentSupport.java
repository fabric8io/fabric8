/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.api.Profile;

import java.util.Collections;
import java.util.List;

public abstract class CreateAgentSupport extends FabricCommand {
    @Option(name = "--version", description = "The version id in the registry")
    protected String version = "base";
    @Option(name = "--profile", multiValued = true, required = false, description = "The profile IDs to associate with the new agent(s)")
    protected List<String> profiles;
    @Option(name = "--enable-debuging", multiValued = false, required = false, description = "Enable debugging")
    protected Boolean debugAgent = Boolean.FALSE;
    @Option(name = "--ensemble-server", multiValued = false, required = false, description = "Whether the agent should be a new ZooKeeper ensemble server")
    protected Boolean isClusterServer = Boolean.FALSE;

    public List<String> getProfileNames() {
        List<String> names = this.profiles;
        if (names == null || names.isEmpty()) {
            names = Collections.singletonList("default");
        }
        return names;
    }

    protected void setProfiles(Agent[] children) {
        List<String> names = getProfileNames();
        try {
            Profile[] profiles = getProfiles(version, names);
            for (Agent child : children) {
                child.setProfiles(profiles);
            }
        } catch (Exception ex) {

        }
    }


}
