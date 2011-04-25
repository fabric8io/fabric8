/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.zookeeper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.fusesource.fabric.util.ZkPath.*;

public class KarafAgentRegistration implements LifecycleListener, ZooKeeperAware {

    private ConfigurationAdmin configurationAdmin;
    private IZKClient zooKeeper;
    private String name;
    //private String nodeAlive;
    //private String nodeConfig;

    public IZKClient getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(IZKClient zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    public void setName(String name) {
        this.name = name;
    }

    /*
    public String getNodeAlive() {
        return nodeAlive;
    }

    public void setNodeAlive(String nodeAlive) {
        this.nodeAlive = nodeAlive;
    }

    public String getNodeConfig() {
        return nodeConfig;
    }

    public void setNodeConfig(String nodeConfig) {
        this.nodeConfig = nodeConfig;
    }
    */

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public void onConnected() {
        try {
            String aliveNode = AGENT_ALIVE.getPath(name);
            Stat stat = zooKeeper.exists(aliveNode);
            if (stat != null) {
                if (stat.getEphemeralOwner() != zooKeeper.getSessionId()) {
                    zooKeeper.delete(aliveNode);
                    zooKeeper.createWithParents(aliveNode, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                }
            } else {
                zooKeeper.createWithParents(aliveNode, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            }
            zooKeeper.createOrSetWithParents(AGENT_JMX.getPath(name), getJmxUrl(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            zooKeeper.createOrSetWithParents(AGENT_SSH.getPath(name), getSshUrl(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            zooKeeper.createOrSetWithParents(AGENT_IP.getPath(name), getLocalHostAddress(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            zooKeeper.createOrSetWithParents(AGENT_ROOT.getPath(name), getRootName(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (Exception e) {
            // TODO
            e.printStackTrace();
        }
    }

    private String getRootName() throws IOException {
        String home = System.getProperty("karaf.home");
        String base = System.getProperty("karaf.base");
        if (home.equals(base)) {
            return "";
        }
        File f = new File(home, "etc/system.properties");
        InputStream is = new FileInputStream(f);
        try {
            Properties p = new Properties();
            p.load(is);
            return p.getProperty("karaf.name");
        } finally {
            is.close();
        }
    }

    private String getJmxUrl() throws IOException {
        Configuration config = configurationAdmin.getConfiguration("org.apache.karaf.management");
        String jmx = (String) config.getProperties().get("serviceUrl");
        jmx = jmx.replace("service:jmx:rmi://localhost:", "service:jmx:rmi://" + getLocalHostAddress() + ":");
        return jmx;
    }

    private String getSshUrl() throws IOException {
        Configuration config;
        config = configurationAdmin.getConfiguration("org.apache.karaf.shell");
        String host = (String) config.getProperties().get("sshHost");
        String port = (String) config.getProperties().get("sshPort");
        return getExternalAddresses(host, port);
    }

    private static String getLocalHostAddress() throws UnknownHostException {
        return InetAddress.getByName(InetAddress.getLocalHost().getCanonicalHostName()).getHostAddress();
    }

    private static String getExternalAddresses(String host, String port) throws UnknownHostException {
        InetAddress ip = InetAddress.getByName(host);
        if (ip.isAnyLocalAddress()) {
            return getLocalHostAddress() + ":" + port;
        } else if (!ip.isLoopbackAddress()) {
            return ip.getHostAddress() + ":" + port;
        }
        return null;
    }

    public void onDisconnected() {
    }
}
