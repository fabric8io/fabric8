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
import org.apache.felix.gogo.commands.Option;

@Command(name = "list", scope = "zk", description = "List a node's children")
public class List extends ZooKeeperCommandSupport {

    @Option(name = "-r", aliases = {"--recursive"}, description = "Display children recursively")
    boolean recursive;

    @Argument(description = "Path of the node to list", required = true)
    String path;

    @Override
    protected Object doExecute() throws Exception {
        display(path);
        return null;
    }

    protected void display(String path) throws Exception {
        java.util.List<String> children = getZooKeeper().getChildren(path);
        for (String child : children) {
            String cp = path.endsWith("/") ? path + child : path + "/" + child;
            System.out.println(cp);
            if (recursive) {
                display(cp);
            }
        }
    }
}
