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
package io.fabric8.service;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerRegistration;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricService;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.internal.ContainerImpl;
import io.fabric8.internal.GeoUtils;
import io.fabric8.utils.HostUtils;
import io.fabric8.utils.Ports;
import io.fabric8.utils.SystemProperties;
import io.fabric8.zookeeper.ZkDefs;
import io.fabric8.zookeeper.ZkPath;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.Set;

import static io.fabric8.zookeeper.ZkPath.CONFIG_CONTAINER;
import static io.fabric8.zookeeper.ZkPath.CONFIG_VERSIONS_CONTAINER;
import static io.fabric8.zookeeper.ZkPath.CONTAINER_ADDRESS;
import static io.fabric8.zookeeper.ZkPath.CONTAINER_ALIVE;
import static io.fabric8.zookeeper.ZkPath.CONTAINER_BINDADDRESS;
import static io.fabric8.zookeeper.ZkPath.CONTAINER_DOMAINS;
import static io.fabric8.zookeeper.ZkPath.CONTAINER_GEOLOCATION;
import static io.fabric8.zookeeper.ZkPath.CONTAINER_HTTP;
import static io.fabric8.zookeeper.ZkPath.CONTAINER_IP;
import static io.fabric8.zookeeper.ZkPath.CONTAINER_JMX;
import static io.fabric8.zookeeper.ZkPath.CONTAINER_LOCAL_HOSTNAME;
import static io.fabric8.zookeeper.ZkPath.CONTAINER_LOCAL_IP;
import static io.fabric8.zookeeper.ZkPath.CONTAINER_PORT_MAX;
import static io.fabric8.zookeeper.ZkPath.CONTAINER_PORT_MIN;
import static io.fabric8.zookeeper.ZkPath.CONTAINER_RESOLVER;
import static io.fabric8.zookeeper.ZkPath.CONTAINER_SSH;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.create;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.createDefault;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.delete;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.deleteSafe;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.exists;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getStringData;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getSubstitutedPath;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;

@ThreadSafe
@Component(name = "io.fabric8.container.registration.karaf", label = "Fabric8 Karaf Container Registration", metatype = false)
@Service({ ContainerRegistration.class, ConfigurationListener.class, ConnectionStateListener.class })
public final class KarafContainerRegistration extends AbstractComponent implements ContainerRegistration, ConfigurationListener, ConnectionStateListener {

    private transient Logger LOGGER = LoggerFactory.getLogger(KarafContainerRegistration.class);

    private static final String MANAGEMENT_PID = "org.apache.karaf.management";
    private static final String SSH_PID = "org.apache.karaf.shell";
    private static final String HTTP_PID = "org.ops4j.pax.web";

    private static final String JMX_SERVICE_URL = "serviceUrl";
    private static final String RMI_REGISTRY_BINDING_PORT_KEY = "rmiRegistryPort";
    private static final String RMI_SERVER_BINDING_PORT_KEY = "rmiServerPort";
    private static final String SSH_BINDING_PORT_KEY = "sshPort";
    private static final String HTTP_BINDING_PORT_KEY = "org.osgi.service.http.port";
    private static final String HTTPS_BINDING_PORT_KEY = "org.osgi.service.http.port.secure";

    private static final String RMI_REGISTRY_CONNECTION_PORT_KEY = "rmiRegistryConnectionPort";
    private static final String RMI_SERVER_CONNECTION_PORT_KEY = "rmiServerConnectionPort";
    private static final String SSH_CONNECTION_PORT_KEY = "sshConnectionPort";
    private static final String HTTP_CONNECTION_PORT_KEY = "org.osgi.service.http.connection.port";
    private static final String HTTPS_CONNECTION_PORT_KEY = "org.osgi.service.http.connection.port.secure";

    private static final String HTTP_ENABLED = "org.osgi.service.http.enabled";
    private static final String HTTPS_ENABLED = "org.osgi.service.http.secure.enabled";

    @Reference(referenceInterface = ConfigurationAdmin.class)
    private final ValidatingReference<ConfigurationAdmin> configAdmin = new ValidatingReference<ConfigurationAdmin>();
    @Reference(referenceInterface = RuntimeProperties.class)
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<RuntimeProperties>();
    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();
    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    @Activate
    void activate() {
        activateInternal();
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    private void activateInternal() {
        RuntimeProperties sysprops = runtimeProperties.get();
        String karafName = sysprops.getProperty(SystemProperties.KARAF_NAME);
        String version = sysprops.getProperty("fabric.version", ZkDefs.DEFAULT_VERSION);
        String profiles = sysprops.getProperty("fabric.profiles");
        try {
            if (profiles != null) {
                String versionNode = CONFIG_CONTAINER.getPath(karafName);
                String profileNode = CONFIG_VERSIONS_CONTAINER.getPath(version, karafName);
                createDefault(curator.get(), versionNode, version);
                createDefault(curator.get(), profileNode, profiles);
            }

            checkAlive();

            String domainsNode = CONTAINER_DOMAINS.getPath(karafName);
            Stat stat = exists(curator.get(), domainsNode);
            if (stat != null) {
                deleteSafe(curator.get(), domainsNode);
            }

            createDefault(curator.get(), CONTAINER_BINDADDRESS.getPath(karafName), sysprops.getProperty(ZkDefs.BIND_ADDRESS, "0.0.0.0"));
            createDefault(curator.get(), CONTAINER_RESOLVER.getPath(karafName), getContainerResolutionPolicy(sysprops, curator.get(), karafName));
            setData(curator.get(), CONTAINER_LOCAL_HOSTNAME.getPath(karafName), HostUtils.getLocalHostName());
            setData(curator.get(), CONTAINER_LOCAL_IP.getPath(karafName), HostUtils.getLocalIp());
            setData(curator.get(), CONTAINER_IP.getPath(karafName), getContainerPointer(curator.get(), karafName));
            createDefault(curator.get(), CONTAINER_GEOLOCATION.getPath(karafName), GeoUtils.getGeoLocation());
            //Check if there are addresses specified as system properties and use them if there is not an existing value in the registry.
            //Mostly usable for adding values when creating containers without an existing ensemble.
            for (String resolver : ZkDefs.VALID_RESOLVERS) {
                String address = sysprops.getProperty(resolver);
                if (address != null && !address.isEmpty() && exists(curator.get(), CONTAINER_ADDRESS.getPath(karafName, resolver)) == null) {
                    setData(curator.get(), CONTAINER_ADDRESS.getPath(karafName, resolver), address);
                }
            }

            //We are creating a dummy container object, since this might be called before the actual container is ready.
            Container current = getContainer();

            sysprops.setProperty(SystemProperties.JAVA_RMI_SERVER_HOSTNAME, current.getIp());
            registerJmx(current);
            registerSsh(current);
            registerHttp(current);

            //Set the port range values
            String minimumPort = sysprops.getProperty(ZkDefs.MINIMUM_PORT);
            String maximumPort = sysprops.getProperty(ZkDefs.MAXIMUM_PORT);
            createDefault(curator.get(), CONTAINER_PORT_MIN.getPath(karafName), minimumPort);
            createDefault(curator.get(), CONTAINER_PORT_MAX.getPath(karafName), maximumPort);
        } catch (Exception e) {
            LOGGER.warn("Error updating Fabric Container information. This exception will be ignored.", e);
        }
    }

    @Override
    public void stateChanged(CuratorFramework client, ConnectionState newState) {
        if (isValid()) {
            switch (newState) {
            case CONNECTED:
            case RECONNECTED:
                try {
                    checkAlive();
                } catch (Exception ex) {
                    LOGGER.error("Error while checking/setting container status.");
                }
                break;
            }
        }
    }

    private void checkAlive() throws Exception {
        RuntimeProperties sysprops = runtimeProperties.get();
        String karafName = sysprops.getProperty(SystemProperties.KARAF_NAME);
        String nodeAlive = CONTAINER_ALIVE.getPath(karafName);
        Stat stat = exists(curator.get(), nodeAlive);
        if (stat != null) {
            if (stat.getEphemeralOwner() != curator.get().getZookeeperClient().getZooKeeper().getSessionId()) {
                delete(curator.get(), nodeAlive);
                create(curator.get(), nodeAlive, CreateMode.EPHEMERAL);
            }
        } else {
            create(curator.get(), nodeAlive, CreateMode.EPHEMERAL);
        }
    }

    private void registerJmx(Container container) throws Exception {
        int rmiRegistryPort = getRmiRegistryPort(container);
        int rmiRegistryConnectionPort = getRmiRegistryConnectionPort(container);
        int rmiServerPort = getRmiServerPort(container);
        int rmiServerConenctionPort = getRmiServerConnectionPort(container);
        String jmxUrl = getJmxUrl(container.getId(), rmiServerConenctionPort, rmiRegistryConnectionPort);
        setData(curator.get(), CONTAINER_JMX.getPath(container.getId()), jmxUrl);
        fabricService.get().getPortService().registerPort(container, MANAGEMENT_PID, RMI_REGISTRY_BINDING_PORT_KEY, rmiRegistryPort);
        fabricService.get().getPortService().registerPort(container, MANAGEMENT_PID, RMI_SERVER_BINDING_PORT_KEY, rmiServerPort);
        Configuration configuration = configAdmin.get().getConfiguration(MANAGEMENT_PID, null);
        updateIfNeeded(configuration, RMI_REGISTRY_BINDING_PORT_KEY, rmiRegistryPort);
        updateIfNeeded(configuration, RMI_SERVER_BINDING_PORT_KEY, rmiServerPort);
    }

    private int getRmiRegistryPort(Container container) throws IOException, KeeperException, InterruptedException {
        return getOrAllocatePortForKey(container, MANAGEMENT_PID, RMI_REGISTRY_BINDING_PORT_KEY, Ports.DEFAULT_RMI_REGISTRY_PORT);
    }

    private int getRmiRegistryConnectionPort(Container container, int defaultValue) throws IOException, KeeperException, InterruptedException {
        return getPortForKey(container, MANAGEMENT_PID, RMI_REGISTRY_CONNECTION_PORT_KEY, defaultValue);
    }

    private int getRmiRegistryConnectionPort(Container container) throws IOException, KeeperException, InterruptedException {
        return getRmiRegistryConnectionPort(container, getRmiRegistryPort(container));
    }

    private int getRmiServerPort(Container container) throws IOException, KeeperException, InterruptedException {
        return getOrAllocatePortForKey(container, MANAGEMENT_PID, RMI_SERVER_BINDING_PORT_KEY, Ports.DEFAULT_RMI_SERVER_PORT);
    }

    private int getRmiServerConnectionPort(Container container, int defaultValue) throws IOException, KeeperException, InterruptedException {
        return getPortForKey(container, MANAGEMENT_PID, RMI_SERVER_CONNECTION_PORT_KEY, defaultValue);
    }

    private int getRmiServerConnectionPort(Container container) throws IOException, KeeperException, InterruptedException {
        return getRmiServerConnectionPort(container, getRmiServerPort(container));
    }

    private String getJmxUrl(String name, int serverConnectionPort, int registryConnectionPort) throws IOException, KeeperException, InterruptedException {
        return "service:jmx:rmi://${zk:" + name + "/ip}:" + serverConnectionPort + "/jndi/rmi://${zk:" + name + "/ip}:" + registryConnectionPort + "/karaf-" + name;
    }

    private void registerSsh(Container container) throws Exception {
        int sshPort = getSshPort(container);
        int sshConnectionPort = getSshConnectionPort(container);
        String sshUrl = getSshUrl(container.getId(), sshConnectionPort);
        setData(curator.get(), CONTAINER_SSH.getPath(container.getId()), sshUrl);
        fabricService.get().getPortService().registerPort(container, SSH_PID, SSH_BINDING_PORT_KEY, sshPort);
        Configuration configuration = configAdmin.get().getConfiguration(SSH_PID, null);
        updateIfNeeded(configuration, SSH_BINDING_PORT_KEY, sshPort);
    }

    private int getSshPort(Container container) throws IOException, KeeperException, InterruptedException {
        return getOrAllocatePortForKey(container, SSH_PID, SSH_BINDING_PORT_KEY, Ports.DEFAULT_KARAF_SSH_PORT);
    }

    private int getSshConnectionPort(Container container) throws IOException, KeeperException, InterruptedException {
        return getSshConnectionPort(container, getSshPort(container));
    }

    private int getSshConnectionPort(Container container, int defaultValue) throws IOException, KeeperException, InterruptedException {
        return getPortForKey(container, SSH_PID, SSH_CONNECTION_PORT_KEY, defaultValue);
    }

    private String getSshUrl(String name, int sshPort) throws IOException, KeeperException, InterruptedException {
        return "${zk:" + name + "/ip}:" + sshPort;
    }

    private void registerHttp(Container container) throws Exception {
        boolean httpEnabled = isHttpEnabled();
        boolean httpsEnabled = isHttpsEnabled();
        String protocol = httpsEnabled && !httpEnabled ? "https" : "http";
        int httpPort = httpsEnabled && !httpEnabled ? getHttpsPort(container) : getHttpPort(container);
        int httpConnectionPort = httpsEnabled && !httpEnabled ? getHttpsConnectionPort(container) : getHttpConnectionPort(container);
        String httpUrl = getHttpUrl(protocol, container.getId(), httpConnectionPort);
        setData(curator.get(), CONTAINER_HTTP.getPath(container.getId()), httpUrl);
        fabricService.get().getPortService().registerPort(container, HTTP_PID, HTTP_BINDING_PORT_KEY, httpPort);
        Configuration configuration = configAdmin.get().getConfiguration(HTTP_PID, null);
        updateIfNeeded(configuration, HTTP_BINDING_PORT_KEY, httpPort);
    }

    private boolean isHttpEnabled() throws IOException {
        Configuration configuration = configAdmin.get().getConfiguration(HTTP_PID, null);
        Dictionary properties = configuration.getProperties();
        if (properties != null && properties.get(HTTP_ENABLED) != null) {
            return Boolean.parseBoolean(String.valueOf(properties.get(HTTP_ENABLED)));
        } else {
            return true;
        }
    }

    private boolean isHttpsEnabled() throws IOException {
        Configuration configuration = configAdmin.get().getConfiguration(HTTP_PID, null);
        Dictionary properties = configuration.getProperties();
        if (properties != null && properties.get(HTTPS_ENABLED) != null) {
            return Boolean.parseBoolean(String.valueOf(properties.get(HTTPS_ENABLED)));
        } else {
            return false;
        }
    }

    private int getHttpPort(Container container) throws KeeperException, InterruptedException, IOException {
        String portProperty = runtimeProperties.get().getProperty(HTTP_BINDING_PORT_KEY);
        int defaultPort = portProperty != null ? Integer.parseInt(portProperty) : Ports.DEFAULT_HTTP_PORT;
        return getOrAllocatePortForKey(container, HTTP_PID, HTTP_BINDING_PORT_KEY, defaultPort);
    }

    private int getHttpConnectionPort(Container container, int defaultValue) throws KeeperException, InterruptedException, IOException {
        return getPortForKey(container, HTTP_PID, HTTP_CONNECTION_PORT_KEY, defaultValue);
    }

    private int getHttpConnectionPort(Container container) throws KeeperException, InterruptedException, IOException {
        return getHttpConnectionPort(container, getHttpPort(container));
    }

    private String getHttpUrl(String protocol, String name, int httpConnectionPort) throws IOException, KeeperException, InterruptedException {
        return protocol + "://${zk:" + name + "/ip}:" + httpConnectionPort;
    }

    private int getHttpsPort(Container container) throws KeeperException, InterruptedException, IOException {
        String portProperty = runtimeProperties.get().getProperty(HTTPS_BINDING_PORT_KEY);
        int defaultPort = portProperty != null ? Integer.parseInt(portProperty) : Ports.DEFAULT_HTTPS_PORT;
        return getOrAllocatePortForKey(container, HTTP_PID, HTTPS_BINDING_PORT_KEY, defaultPort);
    }

    private int getHttpsConnectionPort(Container container) throws KeeperException, InterruptedException, IOException {
        return getPortForKey(container, HTTP_PID, HTTPS_CONNECTION_PORT_KEY, getHttpsPort(container));
    }

    /**
     * Returns a port number for the use in the specified pid and key.
     * If the port is already registered it is directly returned. Else the {@link ConfigurationAdmin} or a default value is used.
     * In the later case, the port will be checked against the already registered ports and will be increased, till it doesn't match the used ports.
     */
    private int getOrAllocatePortForKey(Container container, String pid, String key, int defaultValue) throws IOException, KeeperException, InterruptedException {
        Configuration config = configAdmin.get().getConfiguration(pid, null);
        Set<Integer> unavailable = fabricService.get().getPortService().findUsedPortByHost(container);
        int port = fabricService.get().getPortService().lookupPort(container, pid, key);
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

    /**
     * Returns a port number for the use in the specified pid and key.
     * Note: The method doesn't allocate ports, only gets port if configured.
     */
    private int getPortForKey(Container container, String pid, String key, int defaultValue) throws IOException {
        int port = defaultValue;
        Configuration config = configAdmin.get().getConfiguration(pid, null);
        if (config.getProperties() != null && config.getProperties().get(key) != null) {
            try {
                port = Integer.parseInt((String) config.getProperties().get(key));
            } catch (NumberFormatException ex) {
                port = defaultValue;
            }
        } else {
            port = defaultValue;
        }
        return port;
    }

    private void updateIfNeeded(Configuration configuration, String key, Object value) throws IOException {
        if (configuration != null) {
            Dictionary dictionary = configuration.getProperties();
            if (dictionary != null) {
                if (!String.valueOf(value).equals(dictionary.get(key))) {
                    dictionary.put(key, String.valueOf(value));
                    configuration.setBundleLocation(null);
                    configuration.update(dictionary);
                }
            }
        }
    }

    /**
     * Returns the global resolution policy.
     */
    private String getGlobalResolutionPolicy(RuntimeProperties sysprops, CuratorFramework zooKeeper) throws Exception {
        String policy = ZkDefs.LOCAL_HOSTNAME;
        List<String> validResolverList = Arrays.asList(ZkDefs.VALID_RESOLVERS);
        if (exists(zooKeeper, ZkPath.POLICIES.getPath(ZkDefs.RESOLVER)) != null) {
            policy = getStringData(zooKeeper, ZkPath.POLICIES.getPath(ZkDefs.RESOLVER));
        } else if (sysprops.getProperty(ZkDefs.GLOBAL_RESOLVER_PROPERTY) != null && validResolverList.contains(sysprops.getProperty(ZkDefs.GLOBAL_RESOLVER_PROPERTY))) {
            policy = sysprops.getProperty(ZkDefs.GLOBAL_RESOLVER_PROPERTY);
            setData(zooKeeper, ZkPath.POLICIES.getPath("resolver"), policy);
        }
        return policy;
    }

    /**
     * Returns the container specific resolution policy.
     */
    private String getContainerResolutionPolicy(RuntimeProperties sysprops, CuratorFramework zooKeeper, String container) throws Exception {
        String policy = null;
        List<String> validResolverList = Arrays.asList(ZkDefs.VALID_RESOLVERS);
        if (exists(zooKeeper, ZkPath.CONTAINER_RESOLVER.getPath(container)) != null) {
            policy = getStringData(zooKeeper, ZkPath.CONTAINER_RESOLVER.getPath(container));
        } else if (sysprops.getProperty(ZkDefs.LOCAL_RESOLVER_PROPERTY) != null && validResolverList.contains(sysprops.getProperty(ZkDefs.LOCAL_RESOLVER_PROPERTY))) {
            policy = sysprops.getProperty(ZkDefs.LOCAL_RESOLVER_PROPERTY);
        }

        if (policy == null) {
            policy = getGlobalResolutionPolicy(sysprops, zooKeeper);
        }

        if (policy != null && exists(zooKeeper, ZkPath.CONTAINER_RESOLVER.getPath(container)) == null) {
            setData(zooKeeper, ZkPath.CONTAINER_RESOLVER.getPath(container), policy);
        }
        return policy;
    }

    /**
     * Returns a pointer to the container IP based on the global IP policy.
     *
     * @param curator   The curator client to use to read global policy.
     * @param container The name of the container.
     */
    private static String getContainerPointer(CuratorFramework curator, String container) throws Exception {
        String pointer = "${zk:%s/%s}";
        String resolver = "${zk:%s/resolver}";
        return String.format(pointer, container, String.format(resolver, container));
    }

    /**
     * Receives notification of a Configuration that has changed.
     *
     * @param event The <code>ConfigurationEvent</code>.
     */
    @Override
    public void configurationEvent(ConfigurationEvent event) {
        if (isValid()) {
            try {
                Container current = getContainer();
                RuntimeProperties sysprops = runtimeProperties.get();
                String karafName = sysprops.getProperty(SystemProperties.KARAF_NAME);
                if (event.getPid().equals(SSH_PID) && event.getType() == ConfigurationEvent.CM_UPDATED) {
                    Configuration config = configAdmin.get().getConfiguration(SSH_PID, null);
                    int sshPort = Integer.parseInt((String) config.getProperties().get(SSH_BINDING_PORT_KEY));
                    int sshConnectionPort = getSshConnectionPort(current, sshPort);
                    String sshUrl = getSshUrl(karafName, sshConnectionPort);
                    setData(curator.get(), CONTAINER_SSH.getPath(karafName), sshUrl);
                    if (fabricService.get().getPortService().lookupPort(current, SSH_PID, SSH_BINDING_PORT_KEY) != sshPort) {
                        fabricService.get().getPortService().unregisterPort(current, SSH_PID);
                        fabricService.get().getPortService().registerPort(current, SSH_PID, SSH_BINDING_PORT_KEY, sshPort);
                    }
                }
                if (event.getPid().equals(HTTP_PID) && event.getType() == ConfigurationEvent.CM_UPDATED) {
                    Configuration config = configAdmin.get().getConfiguration(HTTP_PID, null);
                    boolean httpEnabled = isHttpEnabled();
                    boolean httpsEnabled = isHttpsEnabled();
                    String protocol = httpsEnabled && !httpEnabled ? "https" : "http";
                    int httpPort = httpsEnabled && !httpEnabled ? Integer.parseInt((String) config.getProperties().get(HTTPS_BINDING_PORT_KEY)) : Integer
                            .parseInt((String) config.getProperties().get(HTTP_BINDING_PORT_KEY));
                    int httpConnectionPort = httpsEnabled && !httpEnabled ? getHttpsConnectionPort(current) : getHttpConnectionPort(current, httpPort);
                    String httpUrl = getHttpUrl(protocol, karafName, httpConnectionPort);
                    setData(curator.get(), CONTAINER_HTTP.getPath(karafName), httpUrl);
                    if (fabricService.get().getPortService().lookupPort(current, HTTP_PID, HTTP_BINDING_PORT_KEY) != httpPort) {
                        fabricService.get().getPortService().unregisterPort(current, HTTP_PID);
                        fabricService.get().getPortService().registerPort(current, HTTP_PID, HTTP_BINDING_PORT_KEY, httpPort);
                    }
                }
                if (event.getPid().equals(MANAGEMENT_PID) && event.getType() == ConfigurationEvent.CM_UPDATED) {
                    Configuration config = configAdmin.get().getConfiguration(MANAGEMENT_PID, null);
                    int rmiServerPort = Integer.parseInt((String) config.getProperties().get(RMI_SERVER_BINDING_PORT_KEY));
                    int rmiServerConnectionPort = getRmiServerConnectionPort(current, rmiServerPort);
                    int rmiRegistryPort = Integer.parseInt((String) config.getProperties().get(RMI_REGISTRY_BINDING_PORT_KEY));
                    int rmiRegistryConnectionPort = getRmiRegistryConnectionPort(current, rmiRegistryPort);
                    String jmxUrl = getJmxUrl(karafName, rmiServerConnectionPort, rmiRegistryConnectionPort);
                    setData(curator.get(), CONTAINER_JMX.getPath(karafName), jmxUrl);
                    //Whenever the JMX URL changes we need to make sure that the java.rmi.server.hostname points to a valid address.
                    sysprops.setProperty(SystemProperties.JAVA_RMI_SERVER_HOSTNAME, current.getIp());
                    if (fabricService.get().getPortService().lookupPort(current, MANAGEMENT_PID, RMI_REGISTRY_BINDING_PORT_KEY) != rmiRegistryPort
                            || fabricService.get().getPortService().lookupPort(current, MANAGEMENT_PID, RMI_SERVER_BINDING_PORT_KEY) != rmiServerPort) {
                        fabricService.get().getPortService().unregisterPort(current, MANAGEMENT_PID);
                        fabricService.get().getPortService().registerPort(current, MANAGEMENT_PID, RMI_SERVER_BINDING_PORT_KEY, rmiServerPort);
                        fabricService.get().getPortService().registerPort(current, MANAGEMENT_PID, RMI_REGISTRY_BINDING_PORT_KEY, rmiRegistryPort);
                    }

                }
            } catch (Exception e) {
                // ignored
            }
        }
    }

    /**
     * Gets the current {@link Container}.
     *
     * @return The current container if registered or a dummy wrapper of the name and ip.
     */
    private Container getContainer() {
        try {
            return fabricService.get().getCurrentContainer();
        } catch (Exception e) {
            final RuntimeProperties sysprops = runtimeProperties.get();
            final String karafName = sysprops.getProperty(SystemProperties.KARAF_NAME);
            return new ContainerImpl(null, karafName, null) {
                @Override
                public String getIp() {
                    try {
                        return getSubstitutedPath(curator.get(), CONTAINER_IP.getPath(karafName));
                    } catch (Exception e) {
                        throw FabricException.launderThrowable(e);
                    }
                }
            };
        }
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }

    void bindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.bind(service);
    }

    void unbindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.unbind(service);
    }

    void bindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.bind(service);
    }

    void unbindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.unbind(service);
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }
}
