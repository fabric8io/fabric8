/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.zookeeper.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;

@Command(name = "set", scope = "zk", description = "set a node's data")
public class Set extends ZooKeeperCommandSupport {

    @Argument(description = "Path of the node to set", index = 0)
    String path;

    @Argument(description = "The new data", index = 1)
    String data;

    @Override
    protected Object doExecute() throws Exception {
        getZooKeeper().setData(path, data);
        return null;
    }
}
