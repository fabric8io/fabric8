/*
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
import org.fusesource.fabric.commands.support.FabricCommand;

@Command(name = "agent-start", scope = "fabric", description = "Start an existing agent")
public class AgentStart extends FabricCommand {

    @Argument(index = 0, name="agent", description="The agent name", required = true, multiValued = false)
    private String agent = null;

    protected Object doExecute() throws Exception {
        Agent a = fabricService.getAgent(agent);
        if (a == null) {
            throw new IllegalArgumentException("Agent " + agent + " does not exist.");
        }
        a.start();
        return null;
    }

}
