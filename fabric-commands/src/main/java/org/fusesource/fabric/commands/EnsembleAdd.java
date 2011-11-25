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
import org.apache.felix.gogo.commands.Option;

import java.util.List;

@Command(name = "ensemble-add", scope = "fabric", description = "Adds new agents to a ZooKeeper ensemble", detailedDescription = "classpath:ensemble.txt")
public class EnsembleAdd extends EnsembleCommandSupport {

    @Argument(required = true, multiValued = true, description = "List of agents to be added")
    private List<String> agents;

    @Override
    protected Object doExecute() throws Exception {
        service.addToCluster(agents);
        return null;
    }

}
