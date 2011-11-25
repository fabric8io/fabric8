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

import java.io.PrintStream;
import java.util.List;

@Command(name = "ensemble-list", scope = "fabric", description = "Lists the agents in the ZooKeeper ensemble", detailedDescription = "classpath:ensemble.txt")
public class EnsembleList extends EnsembleCommandSupport {

    @Override
    protected Object doExecute() throws Exception {
        PrintStream out = System.out;
        List<String> agents = service.getClusterAgents();
        if (agents != null) {
            out.println("[id]");
            for (String agent : agents) {
                out.println(agent);
            }
        }
        return null;
    }

}
