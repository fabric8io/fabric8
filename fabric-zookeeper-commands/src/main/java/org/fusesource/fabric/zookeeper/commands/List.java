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

    @Argument(description = "Path of the node to list")
    String path = "/";

    @Option(name = "-r", aliases = {"--recursive"}, description = "Display children recursively")
    boolean recursive = false;

    @Option(name="-d", aliases={"--display"}, description="Display a node's value if set")
    boolean display = false;

    @Override
    protected Object doExecute() throws Exception {
        display(path);
        return null;
    }

    protected java.util.List<String> getPaths() throws Exception {
        if (recursive) {
            return getZooKeeper().getAllChildren(path);
        } else {
            return getZooKeeper().getChildren(path);
        }
    }

    protected void display(String path) throws Exception {
        java.util.List<String> paths = getPaths();

        for(String p : paths) {
            if (display) {
                byte[] data = getZooKeeper().getData(p);
                if (data != null) {
                    System.out.printf("%s = %s\n", p, new String(data));
                } else {
                    System.out.println(p);
                }
            } else {
                System.out.println(p);
            }
        }
    }
}
