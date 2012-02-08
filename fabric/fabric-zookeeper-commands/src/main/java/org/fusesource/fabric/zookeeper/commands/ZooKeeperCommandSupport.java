/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.zookeeper.commands;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.linkedin.zookeeper.client.IZKClient;

public abstract class ZooKeeperCommandSupport extends OsgiCommandSupport {

    private IZKClient zooKeeper;
    private long maximumConnectionTimeout = 10 * 1000L;
    private long connectionRetryTime = 100L;

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

    protected String loadUrl(URL url) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        String line;
        StringBuffer content = new StringBuffer();
        while ((line = reader.readLine()) != null) {
            content.append(line);
            content.append(System.getProperty("line.separator"));
        }
        return content.toString();
    }

    /**
     * Lets check if we are connected and throw an exception if we are not.
     * Note that if start() has just been called on IZKClient then it will take a little
     * while for the connection to be established, so we keep checking up to the {@link #getMaximumConnectionTimeout()}
     * until we throw the exception
     */
    protected void checkZooKeeperConnected() throws Exception {
        IZKClient zkClient = getZooKeeper();
        long start = System.currentTimeMillis();
        do {
            if (zkClient.isConnected()) {
                return;
            }
            try {
                Thread.sleep(getConnectionRetryTime());
            } catch (InterruptedException e) {
                // ignore
            }
        } while (System.currentTimeMillis() < start + getMaximumConnectionTimeout());

        if (!zkClient.isConnected()) {
            throw new Exception("Could not connect to ZooKeeper " + zkClient + " at " + zkClient.getConnectString());
        }
    }

    public long getConnectionRetryTime() {
        return connectionRetryTime;
    }

    public void setConnectionRetryTime(long connectionRetryTime) {
        this.connectionRetryTime = connectionRetryTime;
    }

    public long getMaximumConnectionTimeout() {
        return maximumConnectionTimeout;
    }

    public void setMaximumConnectionTimeout(long maximumConnectionTimeout) {
        this.maximumConnectionTimeout = maximumConnectionTimeout;
    }
}
