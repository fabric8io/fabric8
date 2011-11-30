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
import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.commands.support.AgentCreateSupport;

@Command(name = "agent-create-child", scope = "fabric", description = "Creates one or more child agents")
public class AgentCreateChild extends AgentCreateSupport {

    @Argument(index = 0, required = true, description = "Parent agent ID")
    protected String parent;
    @Argument(index = 1, required = true, description = "The name of the agent to be created. When creating multiple agents it serves as a prefix")
    protected String name;
    @Argument(index = 2, required = false, description = "The number of agents that should be created")
    protected int number = 1;

    @Override
    protected Object doExecute() throws Exception {
        String url = "child://" + parent;
        Agent[] children = fabricService.createAgents(url, name, isClusterServer, debugAgent, number);
        setProfiles(children);
        return null;
    }


}
