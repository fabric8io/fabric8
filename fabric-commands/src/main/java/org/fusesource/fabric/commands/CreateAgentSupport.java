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
    @Option(name = "--version")
    protected String version = "base";
    @Option(name = "--profile", multiValued = true, required = false)
    protected List<String> profiles;
    @Option(name = "--enable-debuging", multiValued = false, required = false)
    protected Boolean debugAgent = Boolean.FALSE;
    @Option(name = "--cluster-server", multiValued = false, required = false)
    protected Boolean isClusterServer = Boolean.FALSE;

    @Argument(index = 0, required = true, description = "The name of the agent to be created. When creating multiple agents it serves as a prefix")
    protected String name;
    @Argument(index = 1, required = false, description = "The number of agents that should be created")
    protected int number = 1;

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
