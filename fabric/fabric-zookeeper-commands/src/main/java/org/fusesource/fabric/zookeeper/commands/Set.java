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

import java.net.URL;

@Command(name = "set", scope = "zk", description = "set a node's data")
public class Set extends ZooKeeperCommandSupport {

    @Option(name = "-i", aliases = {"--import"}, description = "Import data from an url")
    boolean importUrl;

    @Argument(description = "Path of the node to set", index = 0)
    String path;

    @Argument(description = "The new data, or url if 'import' option is used", index = 1)
    String data;

    @Override
    protected Object doExecute() throws Exception {

        String nodeData = data;

        if (importUrl) {
            nodeData = loadUrl(new URL(data));
        }

        getZooKeeper().setData(path, nodeData);
        return null;
    }
}
