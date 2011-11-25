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

import java.util.List;

@Command(name = "ensemble-remove", scope = "fabric", description = "Removes agents from a ZooKeeper ensemble", detailedDescription = "classpath:ensemble.txt")
public class EnsembleRemove extends EnsembleCommandSupport {

    @Argument(required = true, multiValued = true, description = "List of agents to be removed")
    private List<String> agents;

    @Override
    protected Object doExecute() throws Exception {
        service.removeFromCluster(agents);
        return null;
    }

}
