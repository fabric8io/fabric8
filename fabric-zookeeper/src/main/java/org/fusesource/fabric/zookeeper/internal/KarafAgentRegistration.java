/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.zookeeper.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Properties;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.osgi.framework.ServiceException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;

import static org.fusesource.fabric.zookeeper.ZkPath.*;

public class KarafAgentRegistration implements LifecycleListener, ZooKeeperAware {

    private ConfigurationAdmin configurationAdmin;
    private IZKClient zooKeeper;

    public IZKClient getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(IZKClient zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public void onConnected() {
        try {
            String name = System.getProperty("karaf.name");
            String nodeAlive = AGENT_ALIVE.getPath(name);
            Stat stat = zooKeeper.exists(nodeAlive);
            if (stat != null) {
                if (stat.getEphemeralOwner() != zooKeeper.getSessionId()) {
                    zooKeeper.delete(nodeAlive);
                    zooKeeper.createWithParents(nodeAlive, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                }
            } else {
                zooKeeper.createWithParents(nodeAlive, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            }

            String domainsNode = AGENT_DOMAINS.getPath(name);
            stat = zooKeeper.exists(domainsNode);
            if (stat != null) {
                zooKeeper.deleteWithChildren(domainsNode);
            }

            String jmxUrl = getJmxUrl();
            if (jmxUrl != null) {
                zooKeeper.createOrSetWithParents(AGENT_JMX.getPath(name), getJmxUrl(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            String sshUrl = getSshUrl();
            if (sshUrl != null) {
                zooKeeper.createOrSetWithParents(AGENT_SSH.getPath(name), getSshUrl(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            zooKeeper.createOrSetWithParents(AGENT_IP.getPath(name), getLocalHostAddress(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            zooKeeper.createOrSetWithParents(AGENT_ROOT.getPath(name), getRootName(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            zooKeeper.createOrSetWithParents(AGENT_DOMAINS.getPath(name), "", ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
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
        if (config.getProperties() != null) {
            String jmx = (String) config.getProperties().get("serviceUrl");
            jmx = jmx.replace("service:jmx:rmi://localhost:", "service:jmx:rmi://" + getLocalHostAddress() + ":");
            return jmx;
        } else {
            return null;
        }
    }

    private String getSshUrl() throws IOException {
        Configuration config = configurationAdmin.getConfiguration("org.apache.karaf.shell");
        if (config != null) {
            String host = (String) config.getProperties().get("sshHost");
            String port = (String) config.getProperties().get("sshPort");
            return getExternalAddresses(host, port);
        } else {
            return null;
        }
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
