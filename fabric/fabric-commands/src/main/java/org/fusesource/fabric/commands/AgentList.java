/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.commands;

import org.apache.felix.gogo.commands.Command;
import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.commands.support.FabricCommand;
import org.fusesource.fabric.zookeeper.ZkDefs;

import java.io.PrintStream;

@Command(name = "agent-list", scope = "fabric", description = "List existing agents")
public class AgentList extends FabricCommand {

    @Override
    protected Object doExecute() throws Exception {
        Agent[] agents = fabricService.getAgents();
        printAgents(agents, System.out);
        return null;
    }

    protected String getProvisionedStatus(Agent agent) {
        String provisioned = agent.getProvisionResult();
        String result = "not provisioned";

        if (provisioned != null) {
            result = provisioned;
            if (result.equals(ZkDefs.ERROR) && agent.getProvisionException() != null) {
                result += " - " + agent.getProvisionException().split(System.getProperty("line.separator"))[0];
            }
        }

        return result;
    }

    protected void printAgents(Agent[] agents, PrintStream out) {
        out.println(String.format("%-30s %-10s %-30s %-100s", "[id]", "[alive]", "[profiles]", "[provision status]"));
        for (Agent agent : agents) {
            if (agent.isRoot()) {
                out.println(String.format("%-30s %-10s %-30s %-100s", agent.getId(), agent.isAlive(), toString(agent.getProfiles()), getProvisionedStatus(agent)));
                for (Agent child : agents) {
                    if (child.getParent() == agent) {
                        out.println(String.format("%-30s %-10s %-30s %-100s", "  " + child.getId(), child.isAlive(), toString(child.getProfiles()), getProvisionedStatus(child)));
                    }
                }
            }
        }
    }

}
