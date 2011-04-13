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

import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.linkedin.zookeeper.client.IZKClient;

public abstract class ZooKeeperCommandSupport extends OsgiCommandSupport {

    private IZKClient zooKeeper;

    public IZKClient getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(IZKClient zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    protected static String getPermString(int perms) {
        StringBuilder p = new StringBuilder();
        if ((perms & ZooDefs.Perms.CREATE) != 0) {
            p.append('c');
        }
        if ((perms & ZooDefs.Perms.DELETE) != 0) {
            p.append('d');
        }
        if ((perms & ZooDefs.Perms.READ) != 0) {
            p.append('r');
        }
        if ((perms & ZooDefs.Perms.WRITE) != 0) {
            p.append('w');
        }
        if ((perms & ZooDefs.Perms.ADMIN) != 0) {
            p.append('a');
        }
        return p.toString();
    }

    protected static List<ACL> parseACLs(String aclString) {
        List<ACL> acl;
        String acls[] = aclString.split(",");
        acl = new ArrayList<ACL>();
        for (String a : acls) {
            int firstColon = a.indexOf(':');
            int lastColon = a.lastIndexOf(':');
            if (firstColon == -1 || lastColon == -1 || firstColon == lastColon) {
                System.err
                        .println(a + " does not have the form scheme:id:perm");
                continue;
            }
            ACL newAcl = new ACL();
            newAcl.setId(new Id(a.substring(0, firstColon), a.substring(
                    firstColon + 1, lastColon)));
            newAcl.setPerms(getPermFromString(a.substring(lastColon + 1)));
            acl.add(newAcl);
        }
        return acl;
    }

    protected static int getPermFromString(String permString) {
        int perm = 0;
        for (int i = 0; i < permString.length(); i++) {
            switch (permString.charAt(i)) {
                case 'r':
                    perm |= ZooDefs.Perms.READ;
                    break;
                case 'w':
                    perm |= ZooDefs.Perms.WRITE;
                    break;
                case 'c':
                    perm |= ZooDefs.Perms.CREATE;
                    break;
                case 'd':
                    perm |= ZooDefs.Perms.DELETE;
                    break;
                case 'a':
                    perm |= ZooDefs.Perms.ADMIN;
                    break;
                default:
                    System.err
                            .println("Unknown perm type: " + permString.charAt(i));
            }
        }
        return perm;
    }
}
