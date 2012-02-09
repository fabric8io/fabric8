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
package org.fusesource.fabric.zookeeper.internal;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.management.MBeanServer;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fusesource.fabric.zookeeper.ZkPath.*;

public class KarafContainerRegistration implements LifecycleListener, ZooKeeperAware, NotificationListener {

    private transient Logger logger = LoggerFactory.getLogger(KarafContainerRegistration.class);

    private ConfigurationAdmin configurationAdmin;
    private IZKClient zooKeeper;
    private BundleContext bundleContext;
    private Set<String> domains = new CopyOnWriteArraySet<String>();

    private volatile MBeanServer mbeanServer;
    private volatile boolean connected;

    public IZKClient getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(IZKClient zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void onConnected() {
        try {
            connected = true;
            String name = System.getProperty("karaf.name");
            String nodeAlive = CONTAINER_ALIVE.getPath(name);
            Stat stat = zooKeeper.exists(nodeAlive);
            if (stat != null) {
                if (stat.getEphemeralOwner() != zooKeeper.getSessionId()) {
                    zooKeeper.delete(nodeAlive);
                    zooKeeper.createWithParents(nodeAlive, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                }
            } else {
                zooKeeper.createWithParents(nodeAlive, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            }

            String domainsNode = CONTAINER_DOMAINS.getPath(name);
            stat = zooKeeper.exists(domainsNode);
            if (stat != null) {
                zooKeeper.deleteWithChildren(domainsNode);
            }

            String jmxUrl = getJmxUrl();
            if (jmxUrl != null) {
                zooKeeper.createOrSetWithParents(CONTAINER_JMX.getPath(name), getJmxUrl(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            String sshUrl = getSshUrl();
            if (sshUrl != null) {
                zooKeeper.createOrSetWithParents(CONTAINER_SSH.getPath(name), getSshUrl(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            zooKeeper.createOrSetWithParents(CONTAINER_IP.getPath(name), getLocalHostAddress(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

            String version = System.getProperty("fabric.version", ZkDefs.DEFAULT_VERSION);
            String profiles = System.getProperty("fabric.profiles");

            if (profiles != null) {
                String versionNode = CONFIG_CONTAINER.getPath(name);
                String profileNode = CONFIG_VERSIONS_CONTAINER.getPath(version, name);

                if (zooKeeper.exists(versionNode) == null) {
                    zooKeeper.createOrSetWithParents(versionNode, version, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
                if (zooKeeper.exists(profileNode) == null) {
                    zooKeeper.createOrSetWithParents(profileNode, profiles, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
            }
            registerDomains();
        } catch (Exception e) {
            logger.warn("Error updating Fabric Container informations", e);
        }
    }

    private String getJmxUrl() throws IOException {
        Configuration config = configurationAdmin.getConfiguration("org.apache.karaf.management");
        if (config.getProperties() != null) {
            String jmx = (String) config.getProperties().get("serviceUrl");
            jmx = jmx.replace("service:jmx:rmi://localhost:", "service:jmx:rmi://" + getLocalHostAddress() + ":");
            jmx = jmx.replace("jndi/rmi://localhost","jndi/rmi://"  + getLocalHostAddress());
            return jmx;
        } else {
            return null;
        }
    }

    private String getSshUrl() throws IOException {
        Configuration config = configurationAdmin.getConfiguration("org.apache.karaf.shell");
        if (config != null && config.getProperties() != null) {
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
            return ip.getHostName() + ":" + port;
        }
        return null;
    }

    public void destroy() {
        try {
            unregisterDomains();
        } catch (ServiceException e) {
            logger.trace("ZooKeeper is no longer available", e);
        } catch (Exception e) {
            logger.warn("An error occured during disconnecting to zookeeper", e);
        }
    }

    public void onDisconnected() {
        connected = false;
    }

    public void registerMBeanServer(ServiceReference ref) {
        try {
            String name = System.getProperty("karaf.name");
            mbeanServer = (MBeanServer) bundleContext.getService(ref);
            mbeanServer.addNotificationListener(new ObjectName("JMImplementation:type=MBeanServerDelegate"), this, null, name);
            registerDomains();
        } catch (Exception e) {
            logger.warn("An error occured during mbean server registration", e);
        }
    }

    public void unregisterMBeanServer(ServiceReference ref) {
        try {
            mbeanServer.removeNotificationListener(new ObjectName("JMImplementation:type=MBeanServerDelegate"), this);
            unregisterDomains();
        } catch (Exception e) {
            logger.warn("An error occured during mbean server unregistration", e);
        }
        mbeanServer = null;
        bundleContext.ungetService(ref);
    }

    protected void registerDomains() throws InterruptedException, KeeperException {
        if (connected && mbeanServer != null) {
            String name = System.getProperty("karaf.name");
            domains.addAll(Arrays.asList(mbeanServer.getDomains()));
            for (String domain : mbeanServer.getDomains()) {
                zooKeeper.createOrSetWithParents(CONTAINER_DOMAIN.getPath(name, domain), null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        }
    }

    protected void unregisterDomains() throws InterruptedException, KeeperException {
        if (connected) {
            String name = System.getProperty("karaf.name");
            String domainsPath = CONTAINER_DOMAINS.getPath(name);
            if (zooKeeper.exists(domainsPath) != null) {
                for (String child : zooKeeper.getChildren(domainsPath)) {
                    zooKeeper.delete(domainsPath + "/" + child);
                }
            }
        }
    }

    @Override
    public void handleNotification(Notification notif, Object o) {
        // handle mbeans registration and de-registration events
        if (connected && mbeanServer != null && notif instanceof MBeanServerNotification) {
            MBeanServerNotification notification = (MBeanServerNotification) notif;
            String domain = notification.getMBeanName().getDomain();
            String path = CONTAINER_DOMAIN.getPath((String) o, domain);
            try {
                if (MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(notification.getType())) {
                    if (domains.add(domain) && zooKeeper.exists(path) == null) {
                        zooKeeper.createOrSetWithParents(path, "", ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                    }
                } else if (MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(notification.getType())) {
                    domains.clear();
                    domains.addAll(Arrays.asList(mbeanServer.getDomains()));
                    if (!domains.contains(domain)) {
                        // domain is no present any more
                        zooKeeper.delete(path);
                    }
                }
//            } catch (KeeperException.SessionExpiredException e) {
//                logger.debug("Session expiry detected. Handling notification once again", e);
//                handleNotification(notif, o);
            } catch (Exception e) {
                logger.warn("Exception while jmx domain synchronization", e);
            }
        }
    }
}
