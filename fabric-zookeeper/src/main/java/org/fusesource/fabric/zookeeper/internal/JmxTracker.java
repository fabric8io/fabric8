/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.zookeeper.internal;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.util.Arrays;
import java.util.List;

import static org.fusesource.fabric.zookeeper.ZkPath.AGENT_DOMAIN;
import static org.fusesource.fabric.zookeeper.ZkPath.AGENT_DOMAINS;

/**
 * Utility class which tracks registered MBeans and copy domain names to zookeeper.
 *
 * @author ldywicki
 */
public class JmxTracker implements LifecycleListener, NotificationListener, ZooKeeperAware {

    private transient Logger logger = LoggerFactory.getLogger(JmxTracker.class);

    private MBeanServer server;
    private IZKClient zooKeeper;
    private String name = System.getProperty("karaf.name");
    private boolean registered;

    public JmxTracker(MBeanServer server) {
        this.server = server;
    }

    @Override
    public void onConnected() {
        try {
            List<String> unnecessary = zooKeeper.getChildren(AGENT_DOMAINS.getPath(name));

            for (String domain : server.getDomains()) {
                sync(domain);
                unnecessary.remove(domain);
            }

            for (String child : unnecessary) {
                // remove domains which are not registered right now
                calculate(child, true);
            }

            // track registered mbeans
            server.addNotificationListener(new ObjectName("JMImplementation:type=MBeanServerDelegate"), this, null, null);
            this.registered = true;
        } catch (Exception e) {
            logger.error("Error while connecting to zookeeper", e);
        }
    }

    @Override
    public void onDisconnected() {
        try {
            if (registered) {
                server.removeNotificationListener(new ObjectName("JMImplementation:type=MBeanServerDelegate"), this);
                registered = false;
            } else {
                logger.debug("JMX listener not registered, skipping deregistration");
            }
        } catch (Exception e) {
            logger.warn("An error occured during disconnecting to zookeeper", e);
        }
    }


    @Override
    public void handleNotification(Notification notif, Object o) {
        // handle mbeans registration and de-registration events

        if (notif instanceof MBeanServerNotification && zooKeeper.isConnected()) {

            MBeanServerNotification notification = (MBeanServerNotification) notif;
            String domain = notification.getMBeanName().getDomain();

            try {
                if (MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(notification.getType())) {
                    sync(domain);
                } else if (MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(notification.getType())) {
                    calculate(domain, false);
                }
            } catch (Exception e) {
                logger.error("Exception while jmx domain {} synchronization",  domain, e);
            }
        }
    }

    private void sync(String domain) throws Exception {
        String path = AGENT_DOMAIN.getPath(name, domain);
        if (zooKeeper.exists(path) == null) {
            logger.debug("New domain {} was registered, add it to zookeeper", domain);
            zooKeeper.createOrSetWithParents(path, "", ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
    }

    private void calculate(String domain, boolean noCheck) throws Exception {
        String path = AGENT_DOMAIN.getPath(name, domain);
        if (Arrays.binarySearch(server.getDomains(), domain) < 0 || noCheck) {
            logger.debug("Domain {} is not present any more in JMX tree. Try remove it from zookeeper", domain);
            // domain is no present any more
            zooKeeper.delete(path);
        }
    }

    @Override
    public void setZooKeeper(IZKClient zooKeeper) {
        this.zooKeeper = zooKeeper;
    }
}
