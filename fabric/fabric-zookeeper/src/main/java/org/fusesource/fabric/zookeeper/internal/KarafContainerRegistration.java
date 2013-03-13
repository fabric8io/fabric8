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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.management.MBeanServer;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.fusesource.fabric.utils.HostUtils;
import org.fusesource.fabric.utils.SystemProperties;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fusesource.fabric.zookeeper.ZkPath.*;

public class KarafContainerRegistration implements LifecycleListener, NotificationListener, ConfigurationListener {

    private transient Logger logger = LoggerFactory.getLogger(KarafContainerRegistration.class);

    public static final String IP_REGEX = "([1-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])){3}";
    public static final String HOST_REGEX = "[a-zA-Z][a-zA-Z0-9\\-\\._]*[a-zA-Z]";
    public static final String IP_OR_HOST_REGEX = "((" + IP_REGEX + ")|(" + HOST_REGEX + ")|0.0.0.0)";
    public static final String RMI_HOST_REGEX = "://" + IP_OR_HOST_REGEX;

    private static final String MANAGEMENT_PID = "org.apache.karaf.management";
    private static final String SHELL_PID = "org.apache.karaf.shell";


    private ConfigurationAdmin configurationAdmin;
    private IZKClient zooKeeper;
    private BundleContext bundleContext;
    private final Set<String> domains = new CopyOnWriteArraySet<String>();
    private volatile MBeanServer mbeanServer;


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

    public synchronized void onConnected() {
        String name = System.getProperty(SystemProperties.KARAF_NAME);
        logger.trace("onConnected");
        String nodeAlive = CONTAINER_ALIVE.getPath(name);
        try {
            Stat stat = ZooKeeperUtils.exists(zooKeeper,nodeAlive);
            if (stat != null) {
                if (stat.getEphemeralOwner() != zooKeeper.getSessionId()) {
                    zooKeeper.delete(nodeAlive);
                    zooKeeper.createWithParents(nodeAlive, CreateMode.EPHEMERAL);
                }
            } else {
                zooKeeper.createWithParents(nodeAlive, CreateMode.EPHEMERAL);
            }

            String domainsNode = CONTAINER_DOMAINS.getPath(name);
            stat = ZooKeeperUtils.exists(zooKeeper,domainsNode);
            if (stat != null) {
                zooKeeper.deleteWithChildren(domainsNode);
            }

            String jmxUrl = getJmxUrl();
            if (jmxUrl != null) {
                ZooKeeperUtils.set(zooKeeper, CONTAINER_JMX.getPath(name), getJmxUrl());
            }
            String sshUrl = getSshUrl();
            if (sshUrl != null) {
                ZooKeeperUtils.set(zooKeeper,CONTAINER_SSH.getPath(name), getSshUrl());
            }

            if (ZooKeeperUtils.exists(zooKeeper,CONTAINER_RESOLVER.getPath(name)) == null) {
                ZooKeeperUtils.set(zooKeeper,CONTAINER_RESOLVER.getPath(name), getContainerResolutionPolicy(zooKeeper, name));
            }
            ZooKeeperUtils.set(zooKeeper,CONTAINER_LOCAL_HOSTNAME.getPath(name), HostUtils.getLocalHostName());
            ZooKeeperUtils.set(zooKeeper,CONTAINER_LOCAL_IP.getPath(name), HostUtils.getLocalIp());
            ZooKeeperUtils.set(zooKeeper,CONTAINER_IP.getPath(name), getContainerPointer(zooKeeper, name));
            //only set the geolocation if it doesn't exist

            if (ZooKeeperUtils.exists(zooKeeper,CONTAINER_GEOLOCATION.getPath(name))==null) {
                ZooKeeperUtils.set(zooKeeper,CONTAINER_GEOLOCATION.getPath(name), HostUtils.getGeoLocation());
            }
            //Check if there are addresses specified as system properties and use them if there is not an existing value in the registry.
            //Mostly usable for adding values when creating containers without an existing ensemble.
            for (String resolver : ZkDefs.VALID_RESOLVERS) {
                String address = System.getProperty(resolver);
                if (address != null && !address.isEmpty() && ZooKeeperUtils.exists(zooKeeper,CONTAINER_ADDRESS.getPath(name, resolver)) == null) {
                    ZooKeeperUtils.set(zooKeeper,CONTAINER_ADDRESS.getPath(name, resolver), address);
                }
            }

            //Set the port range values
            String minimumPort = System.getProperty(ZkDefs.MINIMUM_PORT);
            String maximumPort = System.getProperty(ZkDefs.MAXIMUM_PORT);
            if (ZooKeeperUtils.exists(zooKeeper,CONTAINER_PORT_MIN.getPath(name)) == null) {
                ZooKeeperUtils.set(zooKeeper,CONTAINER_PORT_MIN.getPath(name), minimumPort);
            }

            if (ZooKeeperUtils.exists(zooKeeper,CONTAINER_PORT_MAX.getPath(name)) == null) {
                ZooKeeperUtils.set(zooKeeper,CONTAINER_PORT_MAX.getPath(name), maximumPort);
            }

            String version = System.getProperty("fabric.version", ZkDefs.DEFAULT_VERSION);
            String profiles = System.getProperty("fabric.profiles");

            if (profiles != null) {
                String versionNode = CONFIG_CONTAINER.getPath(name);
                String profileNode = CONFIG_VERSIONS_CONTAINER.getPath(version, name);

                if (ZooKeeperUtils.exists(zooKeeper,versionNode) == null) {
                    ZooKeeperUtils.set(zooKeeper,versionNode, version);
                }
                if (ZooKeeperUtils.exists(zooKeeper,profileNode) == null) {
                    ZooKeeperUtils.set(zooKeeper,profileNode, profiles);
                }
            }
            registerDomains();
        } catch (Exception e) {
            logger.warn("Error updating Fabric Container information. This exception will be ignored.", e);
        }
    }

    private String getJmxUrl() throws IOException {
        String name = System.getProperty(SystemProperties.KARAF_NAME);
        Configuration config = configurationAdmin.getConfiguration(MANAGEMENT_PID);
        if (config.getProperties() != null) {
            String jmx = (String) config.getProperties().get("serviceUrl");
            jmx = replaceJmxHost(jmx, "\\${zk:" + name + "/ip}");
            return jmx;
        } else {
            return null;
        }
    }

    private String getSshUrl() throws IOException {
        String name = System.getProperty(SystemProperties.KARAF_NAME);
        Configuration config = configurationAdmin.getConfiguration(SHELL_PID);
        if (config != null && config.getProperties() != null) {
            String port = (String) config.getProperties().get("sshPort");
            return "${zk:" + name + "/ip}:" + port;
        } else {
            return null;
        }
    }

    /**
     * Returns the global resolution policy.
     *
     * @param zooKeeper
     * @return
     * @throws InterruptedException
     * @throws KeeperException
     */
    private static String getGlobalResolutionPolicy(IZKClient zooKeeper) throws InterruptedException, KeeperException {
        String policy = ZkDefs.LOCAL_HOSTNAME;
        List<String> validResoverList = Arrays.asList(ZkDefs.VALID_RESOLVERS);
        if (ZooKeeperUtils.exists(zooKeeper,ZkPath.POLICIES.getPath(ZkDefs.RESOLVER)) != null) {
            policy = ZooKeeperUtils.get(zooKeeper, ZkPath.POLICIES.getPath(ZkDefs.RESOLVER));
        } else if (System.getProperty(ZkDefs.GLOBAL_RESOLVER_PROPERTY) != null && validResoverList.contains(System.getProperty(ZkDefs.GLOBAL_RESOLVER_PROPERTY))) {
            policy = System.getProperty(ZkDefs.GLOBAL_RESOLVER_PROPERTY);
            ZooKeeperUtils.set(zooKeeper,ZkPath.POLICIES.getPath("resolver"), policy);
        }
        return policy;
    }

    /**
     * Returns the container specific resolution policy.
     *
     * @param zooKeeper
     * @return
     * @throws InterruptedException
     * @throws KeeperException
     */
    private static String getContainerResolutionPolicy(IZKClient zooKeeper, String container) throws InterruptedException, KeeperException {
        String policy = null;
        List<String> validResoverList = Arrays.asList(ZkDefs.VALID_RESOLVERS);
        if (ZooKeeperUtils.exists(zooKeeper,ZkPath.CONTAINER_RESOLVER.getPath(container)) != null) {
            policy = ZooKeeperUtils.get(zooKeeper, ZkPath.CONTAINER_RESOLVER.getPath(container));
        } else if (System.getProperty(ZkDefs.LOCAL_RESOLVER_PROPERTY) != null && validResoverList.contains(System.getProperty(ZkDefs.LOCAL_RESOLVER_PROPERTY))) {
            policy = System.getProperty(ZkDefs.LOCAL_RESOLVER_PROPERTY);
        }

        if (policy == null) {
            policy = getGlobalResolutionPolicy(zooKeeper);
        }

        if (policy != null && ZooKeeperUtils.exists(zooKeeper,ZkPath.CONTAINER_RESOLVER.getPath(container)) == null) {
            ZooKeeperUtils.set(zooKeeper,ZkPath.CONTAINER_RESOLVER.getPath(container), policy);
        }
        return policy;
    }

    /**
     * Returns a pointer to the container IP based on the global IP policy.
     *
     * @param zookeeper The zookeeper client to use to read global policy.
     * @param container The name of the container.
     * @return
     * @throws InterruptedException
     * @throws KeeperException
     */
    private static String getContainerPointer(IZKClient zookeeper, String container) throws InterruptedException, KeeperException {
        String pointer = "${zk:%s/%s}";
        String policy = getContainerResolutionPolicy(zookeeper, container);
        return String.format(pointer, container, policy);
    }

    public void destroy() {
        logger.trace("destroy");
        try {
            unregisterDomains();
        } catch (ServiceException e) {
            logger.trace("ZooKeeper is no longer available", e);
        } catch (Exception e) {
            logger.warn("An error occurred during disconnecting to zookeeper. This exception will be ignored.", e);
        }
    }

    public void onDisconnected() {
        logger.trace("onDisconnected");
        // noop
    }

    public synchronized void registerMBeanServer(ServiceReference ref) {
        try {
            String name = System.getProperty(SystemProperties.KARAF_NAME);
            mbeanServer = (MBeanServer) bundleContext.getService(ref);
            if (mbeanServer != null) {
                mbeanServer.addNotificationListener(new ObjectName("JMImplementation:type=MBeanServerDelegate"), this, null, name);
                registerDomains();
            }
        } catch (Exception e) {
            logger.warn("An error occurred during mbean server registration. This exception will be ignored.", e);
        }
    }

    public synchronized void unregisterMBeanServer(ServiceReference ref) {
        if (mbeanServer != null) {
            try {
                mbeanServer.removeNotificationListener(new ObjectName("JMImplementation:type=MBeanServerDelegate"), this);
                unregisterDomains();
            } catch (Exception e) {
                logger.warn("An error occurred during mbean server unregistration. This exception will be ignored.", e);
            }
        }
        mbeanServer = null;
        bundleContext.ungetService(ref);
    }

    protected void registerDomains() throws InterruptedException, KeeperException {
        if (isConnected() && mbeanServer != null) {
            String name = System.getProperty(SystemProperties.KARAF_NAME);
            domains.addAll(Arrays.asList(mbeanServer.getDomains()));
            for (String domain : mbeanServer.getDomains()) {
                ZooKeeperUtils.set(zooKeeper,CONTAINER_DOMAIN.getPath(name, domain), (byte[]) null);
            }
        }
    }

    protected void unregisterDomains() throws InterruptedException, KeeperException {
        if (isConnected()) {
            String name = System.getProperty(SystemProperties.KARAF_NAME);
            String domainsPath = CONTAINER_DOMAINS.getPath(name);
            if (ZooKeeperUtils.exists(zooKeeper,domainsPath) != null) {
                for (String child : zooKeeper.getChildren(domainsPath)) {
                    zooKeeper.delete(domainsPath + "/" + child);
                }
            }
        }
    }

    @Override
    public synchronized void handleNotification(Notification notif, Object o) {
        logger.trace("handleNotification[{}]", notif);

        // we may get notifications when zookeeper client is not really connected
        // handle mbeans registration and de-registration events
        if (isConnected() && mbeanServer != null && notif instanceof MBeanServerNotification) {
            MBeanServerNotification notification = (MBeanServerNotification) notif;
            String domain = notification.getMBeanName().getDomain();
            String path = CONTAINER_DOMAIN.getPath((String) o, domain);
            try {
                if (MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(notification.getType())) {
                    if (domains.add(domain) && ZooKeeperUtils.exists(zooKeeper,path) == null) {
                        ZooKeeperUtils.set(zooKeeper,path, "");
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
                logger.warn("Exception while jmx domain synchronization from event: " + notif + ". This exception will be ignored.", e);
            }
        }
    }

    /**
     * Replaces hostname/ip occurances inside the jmx url, with the specified hostname
     *
     * @param jmxUrl
     * @param hostName
     * @return
     */
    public static String replaceJmxHost(String jmxUrl, String hostName) {
        if (jmxUrl == null) {
            return null;
        }
        return jmxUrl.replaceAll(RMI_HOST_REGEX, "://" + hostName);
    }


    private boolean isConnected() {
        // we are only considered connected if we have a client and its connected
        return zooKeeper != null && zooKeeper.isConnected();
    }

    /**
     * Receives notification of a Configuration that has changed.
     *
     * @param event The <code>ConfigurationEvent</code>.
     */
    @Override
    public void configurationEvent(ConfigurationEvent event) {
        try {
            if (zooKeeper.isConnected()) {
                String name = System.getProperty(SystemProperties.KARAF_NAME);
                if (event.getPid().equals(SHELL_PID) && event.getType() == ConfigurationEvent.CM_UPDATED) {
                    String sshUrl = getSshUrl();
                    if (sshUrl != null) {
                        ZooKeeperUtils.set(zooKeeper,CONTAINER_SSH.getPath(name), getSshUrl());
                    }
                }
                if (event.getPid().equals(MANAGEMENT_PID) && event.getType() == ConfigurationEvent.CM_UPDATED) {
                    String jmxUrl = getJmxUrl();
                    if (jmxUrl != null) {
                        ZooKeeperUtils.set(zooKeeper,CONTAINER_JMX.getPath(name), getJmxUrl());
                    }
                }
            }
        } catch (Exception e) {

        }
    }
}
