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

import org.apache.karaf.shell.console.Completer;
import org.apache.zookeeper.KeeperException;
import org.linkedin.zookeeper.client.ZKClient;

public class ZNodeCompleter implements Completer {
    private ZKClient zk;

    public ZNodeCompleter() {
        this.zk = zk;
    }

    public void setZooKeeper(ZKClient zk) {
        this.zk = zk;
    }

    @SuppressWarnings("unchecked")
    public int complete(String buffer, int cursor, List candidates) {
        // Guarantee that the final token is the one we're expanding
        if (buffer == null) {
            candidates.add("/");
            return 1;
        }
        buffer = buffer.substring(0, cursor);
        String path = buffer;
        int idx = path.lastIndexOf("/") + 1;
        String prefix = path.substring(idx);
        try {
            // Only the root path can end in a /, so strip it off every other prefix
            String dir = idx == 1 ? "/" : path.substring(0, idx - 1);
            List<String> children = zk.getChildren(dir, false);
            for (String child : children) {
                if (child.startsWith(prefix)) {
                    candidates.add(child);
                }
            }
        } catch (InterruptedException e) {
            return 0;
        } catch (KeeperException e) {
            return 0;
        }
        return candidates.size() == 0 ? buffer.length() : buffer.lastIndexOf("/") + 1;
    }
}
