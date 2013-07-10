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
package org.fusesource.fabric.service;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.internal.ContainerImpl;
import org.fusesource.fabric.internal.GeoUtils;
import org.fusesource.fabric.utils.HostUtils;
import org.fusesource.fabric.utils.Ports;
import org.fusesource.fabric.utils.SystemProperties;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.fusesource.fabric.zookeeper.ZkPath.CONFIG_CONTAINER;
import static org.fusesource.fabric.zookeeper.ZkPath.CONFIG_VERSIONS_CONTAINER;
import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_ADDRESS;
import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_ALIVE;
import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_BINDADDRESS;
import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_DOMAIN;
import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_DOMAINS;
import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_GEOLOCATION;
import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_HTTP;
import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_IP;
import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_JMX;
import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_LOCAL_HOSTNAME;
import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_LOCAL_IP;
import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_PORT_MAX;
import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_PORT_MIN;
import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_RESOLVER;
import static org.fusesource.fabric.zookeeper.ZkPath.CONTAINER_SSH;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.create;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.createDefault;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.delete;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.deleteSafe;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.exists;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getStringData;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getSubstitutedPath;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setData;

public class
        KarafContainerRegistration implements ConnectionStateListener, NotificationListener, ConfigurationListener {

    private transient Logger LOGGER = LoggerFactory.getLogger(KarafContainerRegistration.class);

    private static final String MANAGEMENT_PID = "org.apache.karaf.management";
    private static final String SSH_PID = "org.apache.karaf.shell";
    private static final String HTTP_PID = "org.ops4j.pax.web";

    private static final String RMI_REGISTRY_KEY = "rmiRegistryPort";
    private static final String RMI_SERVER_KEY = "rmiServerPort";
    private static final String SSH_KEY = "sshPort";
    private static final String HTTP_KEY = "org.osgi.service.http.port";


    private ConfigurationAdmin configurationAdmin;
    private CuratorFramework curator;
    private FabricService fabricService;
    private BundleContext bundleContext;
    private final Set<String> domains = new CopyOnWriteArraySet<String>();
    private volatile MBeanServer mbeanServer;
    private volatile boolean connected;


    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    @Override
    public void stateChanged(CuratorFramework client, ConnectionState newState) {
        switch (newState) {
            case CONNECTED:
            case RECONNECTED:
                this.curator = client;
                onConnected();
                break;
            default:
                onDisconnected();
        }
    }

    public synchronized void onConnected() {
        connected = true;

        final String name = System.getProperty(SystemProperties.KARAF_NAME);
        String version = System.getProperty("fabric.version", ZkDefs.DEFAULT_VERSION);
        String profiles = System.getProperty("fabric.profiles");
        LOGGER.trace("onConnected");

        String nodeAlive = CONTAINER_ALIVE.getPath(name);
        try {

            if (profiles != null) {
                String versionNode = CONFIG_CONTAINER.getPath(name);
                String profileNode = CONFIG_VERSIONS_CONTAINER.getPath(version, name);
                createDefault(curator, versionNode, version);
                createDefault(curator, profileNode, profiles);
            }

            Stat stat = exists(curator, nodeAlive);
            if (stat != null) {
                if (stat.getEphemeralOwner() != curator.getZookeeperClient().getZooKeeper().getSessionId()) {
                    delete(curator, nodeAlive);
                    create(curator, nodeAlive, CreateMode.EPHEMERAL);
                }
            } else {
                create(curator, nodeAlive, CreateMode.EPHEMERAL);
            }

            String domainsNode = CONTAINER_DOMAINS.getPath(name);
            stat = exists(curator, domainsNode);
            if (stat != null) {
                deleteSafe(curator, domainsNode);
            }

            createDefault(curator, CONTAINER_BINDADDRESS.getPath(name), System.getProperty(ZkDefs.BIND_ADDRESS, "0.0.0.0"));
            createDefault(curator, CONTAINER_RESOLVER.getPath(name), getContainerResolutionPolicy(curator, name));
            setData(curator, CONTAINER_LOCAL_HOSTNAME.getPath(name), HostUtils.getLocalHostName());
            setData(curator, CONTAINER_LOCAL_IP.getPath(name), HostUtils.getLocalIp());
            setData(curator, CONTAINER_IP.getPath(name), getContainerPointer(curator, name));
            createDefault(curator, CONTAINER_GEOLOCATION.getPath(name), GeoUtils
                    .getGeoLocation());
            //Check if there are addresses specified as system properties and use them if there is not an existing value in the registry.
            //Mostly usable for adding values when creating containers without an existing ensemble.
            for (String resolver : ZkDefs.VALID_RESOLVERS) {
                String address = System.getProperty(resolver);
                if (address != null && !address.isEmpty() && exists(curator, CONTAINER_ADDRESS.getPath(name, resolver)) == null) {
                    setData(curator, CONTAINER_ADDRESS.getPath(name, resolver), address);
                }
            }

            //We are creating a dummy container object, since this might be called before the actual container is ready.
            Container current = getContainer();

            registerJmx(current);
            registerSsh(current);
            registerHttp(current);

            //Set the port range values
            String minimumPort = System.getProperty(ZkDefs.MINIMUM_PORT);
            String maximumPort = System.getProperty(ZkDefs.MAXIMUM_PORT);
            createDefault(curator, CONTAINER_PORT_MIN.getPath(name), minimumPort);
            createDefault(curator, CONTAINER_PORT_MAX.getPath(name), maximumPort);

            registerDomains();
        } catch (Exception e) {
            LOGGER.warn("Error updating Fabric Container information. This exception will be ignored.", e);
        }
    }

    private void registerJmx(Container container) throws Exception {
        int rmiRegistryPort = getRmiRegistryPort(container);
        int rmiServerPort = getRmiServerPort(container);
        String jmxUrl = getJmxUrl(container.getId(), rmiServerPort, rmiRegistryPort);
        setData(curator, CONTAINER_JMX.getPath(container.getId()), jmxUrl);
        fabricService.getPortService().registerPort(container, MANAGEMENT_PID, RMI_REGISTRY_KEY, rmiRegistryPort);
        fabricService.getPortService().registerPort(container, MANAGEMENT_PID, RMI_SERVER_KEY, rmiServerPort);
        Configuration configuration = configurationAdmin.getConfiguration(MANAGEMENT_PID);
        updateIfNeeded(configuration, RMI_REGISTRY_KEY, rmiRegistryPort);
        updateIfNeeded(configuration, RMI_SERVER_KEY, rmiServerPort);
    }

    private int getRmiRegistryPort(Container container) throws IOException, KeeperException, InterruptedException {
        return getPortForKey(container, MANAGEMENT_PID, RMI_REGISTRY_KEY, Ports.DEFAULT_RMI_REGISTRY_PORT);
    }

    private int getRmiServerPort(Container container) throws IOException, KeeperException, InterruptedException {
        return getPortForKey(container, MANAGEMENT_PID, RMI_SERVER_KEY, Ports.DEFAULT_RMI_SERVER_PORT);
    }

    private String getJmxUrl(String name, int serverPort, int registryPort) throws IOException, KeeperException, InterruptedException {
        return "service:jmx:rmi://${zk:" + name + "/ip}:" + serverPort + "/jndi/rmi://${zk:" + name + "/ip}:" + registryPort + "/karaf-" + name;
    }

    private void registerSsh(Container container) throws Exception {
        int sshPort = getSshPort(container);
        String sshUrl = getSshUrl(container.getId(), sshPort);
        setData(curator, CONTAINER_SSH.getPath(container.getId()), sshUrl);
        fabricService.getPortService().registerPort(container, SSH_PID, SSH_KEY, sshPort);
        Configuration configuration = configurationAdmin.getConfiguration(SSH_PID);
        updateIfNeeded(configuration, SSH_KEY, sshPort);
    }

    private int getSshPort(Container container) throws IOException, KeeperException, InterruptedException {
        return getPortForKey(container, SSH_PID, SSH_KEY, Ports.DEFAULT_KARAF_SSH_PORT);
    }

    private String getSshUrl(String name, int sshPort) throws IOException, KeeperException, InterruptedException {
        return "${zk:" + name + "/ip}:" + sshPort;
    }


    private void registerHttp(Container container) throws Exception {
        int httpPort = getHttpPort(container);
        String httpUrl = getHttpUrl(container.getId(), httpPort);
        setData(curator, CONTAINER_HTTP.getPath(container.getId()), httpUrl);
        fabricService.getPortService().registerPort(container, HTTP_PID, HTTP_KEY, httpPort);
        Configuration configuration = configurationAdmin.getConfiguration(HTTP_PID);
        updateIfNeeded(configuration, HTTP_KEY, httpPort);
    }

    private int getHttpPort(Container container) throws KeeperException, InterruptedException, IOException {
        return getPortForKey(container, HTTP_PID, HTTP_KEY, Ports.DEFAULT_HTTP_PORT);
    }

    private String getHttpUrl(String name, int httpPort) throws IOException, KeeperException, InterruptedException {
        return "http://${zk:" + name + "/ip}:" + httpPort;
    }


    /**
     * Returns a port number for the use in the specified pid and key.
     * If the port is already registered it is directly returned. Else the {@link ConfigurationAdmin} or a default value is used.
     * In the later case, the port will be checked against the already registered ports and will be increased, till it doesn't match the used ports.
     *
     * @param container
     * @param pid
     * @param key
     * @param defaultValue
     * @return
     * @throws IOException
     * @throws KeeperException
     * @throws InterruptedException
     */
    private int getPortForKey(Container container, String pid, String key, int defaultValue) throws IOException, KeeperException, InterruptedException {
        Configuration config = configurationAdmin.getConfiguration(pid);
        Set<Integer> unavailable = fabricService.getPortService().findUsedPortByHost(container);
        int port = fabricService.getPortService().lookupPort(container, pid, key);
        if (port > 0) {
            return port;
        } else if (config.getProperties() != null && config.getProperties().get(key) != null) {
            try {
                port = Integer.parseInt((String) config.getProperties().get(key));
            } catch (NumberFormatException ex) {
                port = defaultValue;
            }
        } else {
            port = defaultValue;
        }

        while (unavailable.contains(port)) {
            port++;
        }
        return port;
    }

    private void updateIfNeeded(Configuration configuration, String key, int port) throws IOException {
        if (configuration != null) {
            Dictionary dictionary = configuration.getProperties();
            if (dictionary != null) {
                if (!dictionary.get(key).equals(String.valueOf(port))) {
                    dictionary.put(key, String.valueOf(port));
                    configuration.update(dictionary);
                }
            }
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
    private static String getGlobalResolutionPolicy(CuratorFramework zooKeeper) throws Exception {
        String policy = ZkDefs.LOCAL_HOSTNAME;
        List<String> validResolverList = Arrays.asList(ZkDefs.VALID_RESOLVERS);
        if (exists(zooKeeper, ZkPath.POLICIES.getPath(ZkDefs.RESOLVER)) != null) {
            policy = getStringData(zooKeeper, ZkPath.POLICIES.getPath(ZkDefs.RESOLVER));
        } else if (System.getProperty(ZkDefs.GLOBAL_RESOLVER_PROPERTY) != null && validResolverList.contains(System.getProperty(ZkDefs.GLOBAL_RESOLVER_PROPERTY))) {
            policy = System.getProperty(ZkDefs.GLOBAL_RESOLVER_PROPERTY);
            setData(zooKeeper, ZkPath.POLICIES.getPath("resolver"), policy);
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
    private static String getContainerResolutionPolicy(CuratorFramework zooKeeper, String container) throws Exception {
        String policy = null;
        List<String> validResolverList = Arrays.asList(ZkDefs.VALID_RESOLVERS);
        if (exists(zooKeeper, ZkPath.CONTAINER_RESOLVER.getPath(container)) != null) {
            policy = getStringData(zooKeeper, ZkPath.CONTAINER_RESOLVER.getPath(container));
        } else if (System.getProperty(ZkDefs.LOCAL_RESOLVER_PROPERTY) != null && validResolverList.contains(System.getProperty(ZkDefs.LOCAL_RESOLVER_PROPERTY))) {
            policy = System.getProperty(ZkDefs.LOCAL_RESOLVER_PROPERTY);
        }

        if (policy == null) {
            policy = getGlobalResolutionPolicy(zooKeeper);
        }

        if (policy != null && exists(zooKeeper, ZkPath.CONTAINER_RESOLVER.getPath(container)) == null) {
            setData(zooKeeper, ZkPath.CONTAINER_RESOLVER.getPath(container), policy);
        }
        return policy;
    }

    /**
     * Returns a pointer to the container IP based on the global IP policy.
     *
     * @param curator The curator client to use to read global policy.
     * @param container The name of the container.
     * @return
     * @throws InterruptedException
     * @throws KeeperException
     */
    private static String getContainerPointer(CuratorFramework curator, String container) throws Exception {
        String pointer = "${zk:%s/%s}";
        String policy = getContainerResolutionPolicy(curator, container);
        return String.format(pointer, container, policy);
    }

    public void destroy() {
        LOGGER.trace("destroy");
        try {
            unregisterDomains();
        } catch (ServiceException e) {
            LOGGER.trace("ZooKeeper is no longer available", e);
        } catch (Exception e) {
            LOGGER.warn("An error occurred during disconnecting to curator. This exception will be ignored.", e);
        }
    }

    public void onDisconnected() {
        LOGGER.trace("onDisconnected");
        connected = false;
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
            LOGGER.warn("An error occurred during mbean server registration. This exception will be ignored.", e);
        }
    }

    public synchronized void unregisterMBeanServer(ServiceReference ref) {
        if (mbeanServer != null) {
            try {
                mbeanServer.removeNotificationListener(new ObjectName("JMImplementation:type=MBeanServerDelegate"), this);
                unregisterDomains();
            } catch (Exception e) {
                LOGGER.warn("An error occurred during mbean server unregistration. This exception will be ignored.", e);
            }
        }
        mbeanServer = null;
        bundleContext.ungetService(ref);
    }

    protected void registerDomains() throws Exception {
        if (isConnected() && mbeanServer != null) {
            String name = System.getProperty(SystemProperties.KARAF_NAME);
            domains.addAll(Arrays.asList(mbeanServer.getDomains()));
            for (String domain : mbeanServer.getDomains()) {
                setData(curator, CONTAINER_DOMAIN.getPath(name, domain), (byte[]) null);
            }
        }
    }

    protected void unregisterDomains() throws Exception {
        if (isConnected()) {
            String name = System.getProperty(SystemProperties.KARAF_NAME);
            String domainsPath = CONTAINER_DOMAINS.getPath(name);
            deleteSafe(curator, domainsPath);
        }
    }

    @Override
    public synchronized void handleNotification(Notification notif, Object o) {
        LOGGER.trace("handleNotification[{}]", notif);

        // we may get notifications when curator client is not really connected
        // handle mbeans registration and de-registration events
        if (isConnected() && mbeanServer != null && notif instanceof MBeanServerNotification) {
            MBeanServerNotification notification = (MBeanServerNotification) notif;
            String domain = notification.getMBeanName().getDomain();
            String path = CONTAINER_DOMAIN.getPath((String) o, domain);
            try {
                if (MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(notification.getType())) {
                    if (domains.add(domain) && exists(curator, path) == null) {
                        setData(curator, path, "");
                    }
                } else if (MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(notification.getType())) {
                    domains.clear();
                    domains.addAll(Arrays.asList(mbeanServer.getDomains()));
                    if (!domains.contains(domain)) {
                        // domain is no present any more
                        deleteSafe(curator, path);
                    }
                }
//            } catch (KeeperException.SessionExpiredException e) {
//                LOGGER.debug("Session expiry detected. Handling notification once again", e);
//                handleNotification(notif, o);
            } catch (Exception e) {
                LOGGER.warn("Exception while jmx domain synchronization from event: " + notif + ". This exception will be ignored.", e);
            }
        }
    }


    private boolean isConnected() {
        // we are only considered connected if we have a client and its connected
        return curator != null && connected;
    }

    /**
     * Receives notification of a Configuration that has changed.
     *
     * @param event The <code>ConfigurationEvent</code>.
     */
    @Override
    public void configurationEvent(ConfigurationEvent event) {
        try {
            if (isConnected()) {
                Container current = getContainer();

                String name = System.getProperty(SystemProperties.KARAF_NAME);
                if (event.getPid().equals(SSH_PID) && event.getType() == ConfigurationEvent.CM_UPDATED) {
                    Configuration config = configurationAdmin.getConfiguration(SSH_PID);
                    int sshPort = Integer.parseInt((String) config.getProperties().get(SSH_KEY));
                    String sshUrl = getSshUrl(name, sshPort);
                    setData(curator, CONTAINER_SSH.getPath(name), sshUrl);
                    if (fabricService.getPortService().lookupPort(current, SSH_PID, SSH_KEY) != sshPort) {
                        fabricService.getPortService().unRegisterPort(current, SSH_PID);
                        fabricService.getPortService().registerPort(current, SSH_PID, SSH_KEY, sshPort);
                    }
                }
                if (event.getPid().equals(HTTP_PID) && event.getType() == ConfigurationEvent.CM_UPDATED) {
                    Configuration config = configurationAdmin.getConfiguration(HTTP_PID);
                    int httpPort = Integer.parseInt((String) config.getProperties().get(HTTP_KEY));
                    String httpUrl = getHttpUrl(name, httpPort);
                    setData(curator, CONTAINER_HTTP.getPath(name), httpUrl);
                    if (fabricService.getPortService().lookupPort(current, HTTP_PID, HTTP_KEY) != httpPort) {
                        fabricService.getPortService().unRegisterPort(current, HTTP_PID);
                        fabricService.getPortService().registerPort(current, HTTP_PID, HTTP_KEY, httpPort);
                    }
                }
                if (event.getPid().equals(MANAGEMENT_PID) && event.getType() == ConfigurationEvent.CM_UPDATED) {
                    Configuration config = configurationAdmin.getConfiguration(MANAGEMENT_PID);
                    int rmiServerPort = Integer.parseInt((String) config.getProperties().get(RMI_SERVER_KEY));
                    int rmiRegistryPort = Integer.parseInt((String) config.getProperties().get(RMI_REGISTRY_KEY));
                    String jmxUrl = getJmxUrl(name, rmiServerPort, rmiRegistryPort);
                    setData(curator, CONTAINER_JMX.getPath(name), jmxUrl);
                    if (fabricService.getPortService().lookupPort(current, MANAGEMENT_PID, RMI_REGISTRY_KEY) != rmiRegistryPort
                            || fabricService.getPortService().lookupPort(current, MANAGEMENT_PID, RMI_SERVER_KEY) != rmiServerPort) {
                        fabricService.getPortService().unRegisterPort(current, MANAGEMENT_PID);
                        fabricService.getPortService().registerPort(current, MANAGEMENT_PID, RMI_SERVER_KEY, rmiServerPort);
                        fabricService.getPortService().registerPort(current, MANAGEMENT_PID, RMI_REGISTRY_KEY, rmiRegistryPort);
                    }

                }
            }
        } catch (Exception e) {

        }
    }

    /**
     * Gets the current {@link Container}.
     * @return  The current container if registered or a dummy wrapper of the name and ip.
     */
    private Container getContainer() {
        try {
            return fabricService.getCurrentContainer();
        } catch (Exception e) {
            final String name = System.getProperty(SystemProperties.KARAF_NAME);
            return new ContainerImpl(null, name, null) {
                @Override
                public String getIp() {
                    try {
                        return getSubstitutedPath(curator, CONTAINER_IP.getPath(name));
                    } catch (Exception e) {
                        throw new FabricException(e);
                    }
                }
            };
        }
    }
}
