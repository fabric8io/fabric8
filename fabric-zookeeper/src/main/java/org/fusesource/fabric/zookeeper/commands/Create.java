/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.zookeeper.commands;

import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;

@Command(name = "create", scope = "zk", description = "Create a node")
public class Create extends ZooKeeperCommandSupport {

    @Option(name = "-e", aliases = {"--ephemeral"}, description = "Create an ephemeral node")
    boolean ephemeral;

    @Option(name = "-s", aliases = {"--sequential"}, description = "Create a sequential node")
    boolean sequential;

    @Option(name = "-r", aliases = {"--recursive"}, description = "Automatically create parents")
    boolean recursive;

    @Option(name = "-a", aliases = {"--acl"}, description = "Node ACLs")
    String acl;

    @Argument(index = 0, required = true, description = "Path of the node to create")
    String path;

    @Argument(index = 1, required = false, description = "Data for the node")
    String data;

    @Override
    protected Object doExecute() throws Exception {
        List<ACL> acls = acl == null ? ZooDefs.Ids.OPEN_ACL_UNSAFE : parseACLs(acl);
        CreateMode mode;
        if (ephemeral && sequential) {
            mode = CreateMode.EPHEMERAL_SEQUENTIAL;
        } else if (ephemeral) {
            mode = CreateMode.EPHEMERAL;
        } else if (sequential) {
            mode = CreateMode.PERSISTENT_SEQUENTIAL;
        } else {
            mode = CreateMode.PERSISTENT;
        }
        if (recursive) {
            getZooKeeper().createWithParents(path, data, acls, mode);
        } else {
            getZooKeeper().create(path, data, acls, mode);
        }
        return null;
    }
}
