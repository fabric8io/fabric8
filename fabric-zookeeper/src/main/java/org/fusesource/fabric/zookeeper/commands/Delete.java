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

@Command(name = "delete", scope = "zk", description = "Delete a node")
public class Delete extends ZooKeeperCommandSupport {

    @Option(name = "-v", aliases = {"--version "}, description = "Version to delete")
    int version = -1;

    @Option(name = "-r", aliases = {"--recursive"}, description = "Automatically create parents")
    boolean recursive;

    @Argument(description = "Path of the node to delete")
    String path;

    @Override
    protected Object doExecute() throws Exception {
        if (recursive) {
            if (version >= 0) {
                throw new UnsupportedOperationException("Unable to delete a version recursively");
            }
            getZooKeeper().deleteWithChildren(path);
        } else {
            getZooKeeper().delete(path, version);
        }
        return null;
    }
}
