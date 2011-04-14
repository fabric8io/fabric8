/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.commands;

import java.util.Map;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.api.AgentService;

@Command(name = "list-agents", scope = "fabric")
public class ListAgents extends OsgiCommandSupport {

    private AgentService agentService;

    public AgentService getAgentService() {
        return agentService;
    }

    public void setAgentService(AgentService agentService) {
        this.agentService = agentService;
    }

    @Override
    protected Object doExecute() throws Exception {
        Map<String, Agent> agents = agentService.getAgents();
        for (Agent agent : agents.values()) {
            System.out.println(agent.getId() + ": alive=" + agent.isAlive() + (agent.getParent() != null ? ", parent=" + agent.getParent().getId() : ""));
        }
        return null;
    }
}
