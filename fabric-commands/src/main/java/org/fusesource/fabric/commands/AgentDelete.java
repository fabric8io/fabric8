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
import org.fusesource.fabric.commands.support.FabricCommand;

@Command(name = "agent-delete", scope = "fabric", description = "Delete an existing agent")
public class AgentDelete extends FabricCommand {

    @Argument(index = 0)
    private String name;

    @Override
    protected Object doExecute() throws Exception {
        Agent agent = fabricService.getAgent(name);
        if( agent==null ) {
            throw new IllegalArgumentException("Agent does not exist: "+name);
        }
        agent.destroy();
        return null;
    }

}
