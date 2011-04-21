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
import java.util.Map;

import org.apache.felix.gogo.commands.Command;
import org.fusesource.fabric.api.Agent;

@Command(name = "list-agents", scope = "fabric", description = "List existing agents")
public class ListAgents extends FabricCommand {

    @Override
    protected Object doExecute() throws Exception {
        Map<String, Agent> agents = agentService.getAgents();
        printAgents(agents, System.out);
        return null;
    }

    protected void printAgents(Map<String, Agent> agents, PrintStream out) {
        out.println(String.format("%-30s %-10s %s", "[id]", "[alive]", "[profiles]"));
        for (Agent agent : agents.values()) {
            if (agent.isRoot()) {
                out.println(String.format("%-30s %-10s %s", agent.getId(), agent.isAlive(), toString(agent.getProfileNames())));
                for (Agent child : agents.values()) {
                    if (child.getParent() == agent) {
                        out.println(String.format("%-30s %-10s %s", "  " + child.getId(), child.isAlive(), toString(child.getProfileNames())));
                    }
                }
            }
        }
    }

}
