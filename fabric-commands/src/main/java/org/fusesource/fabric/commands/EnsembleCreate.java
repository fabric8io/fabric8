/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.commands;

import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.commands.support.EnsembleCommandSupport;

@Command(name = "ensemble-create", scope = "fabric", description = "Create a new ZooKeeper ensemble", detailedDescription = "classpath:ensemble.txt")
public class EnsembleCreate extends EnsembleCommandSupport {

    @Option(name = "--clean", description = "Clean local zookeeper cluster and configurations")
    private boolean clean;

    @Argument(required = false, multiValued = true, description = "List of agents")
    private List<String> agents;

    @Override
    protected Object doExecute() throws Exception {
        if (clean) {
            service.clean();
        } else {
            if (agents == null || agents.isEmpty()) {
                throw new IllegalStateException("No agents specified.");
            }
        }
        if (agents != null && !agents.isEmpty()) {
            service.createCluster(agents);
        }
        return null;
    }

}
