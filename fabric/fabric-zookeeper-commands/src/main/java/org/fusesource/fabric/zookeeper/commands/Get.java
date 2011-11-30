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

@Command(name = "get", scope = "zk", description = "Get a node's data")
public class Get extends ZooKeeperCommandSupport {

    @Argument(description = "Path of the node to get")
    String path;

    @Override
    protected Object doExecute() throws Exception {
        System.out.println(getZooKeeper().getStringData(path));
        return null;
    }
}
