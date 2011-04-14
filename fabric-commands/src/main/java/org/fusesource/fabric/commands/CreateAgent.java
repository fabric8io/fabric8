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
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.api.AgentService;

@Command(name = "create-agents", scope = "fabric")
public class CreateAgent extends OsgiCommandSupport {

    private AgentService agentService;

    @Argument(index = 0)
    private String name;

    @Argument(index = 1, required = true)
    private String parent;


    public AgentService getAgentService() {
        return agentService;
    }

    public void setAgentService(AgentService agentService) {
        this.agentService = agentService;
    }

    @Override
    protected Object doExecute() throws Exception {
        Agent agent = agentService.getAgents().get( parent );
        if (agent == null) {
            throw new Exception("Unknown agent: " + parent);
        }
        agentService.createChild(agent, name);
        return null;
    }
}
