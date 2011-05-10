/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.zookeeper.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.gogo.commands.Command;

@Command(name = "export", scope = "zk", description = "Export data from zookeeper")
public class Export extends ZooKeeperCommandSupport {

    @Override
    protected Object doExecute() throws Exception {
        display("/");
        return null;
    }

    protected void display(String path) throws Exception {
        List<String> paths = new ArrayList<String>();
        for (String child : getZooKeeper().getChildren(path)) {
            String cp = path.endsWith("/") ? path + child : path + "/" + child;
            if (getZooKeeper().exists(cp).getEphemeralOwner() == 0) {
                paths.add(cp);
            }
        }
        String data = getZooKeeper().getStringData(path);
        if (data != null && !data.isEmpty() || paths.isEmpty()) {
            System.out.println(data != null && !data.isEmpty() ? path + " = " + data : path);
        }
        for (String child : paths) {
            display(child);
        }
    }
}
