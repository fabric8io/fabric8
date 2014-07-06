/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.runtime.container.wildfly;

import io.fabric8.api.Container;
import io.fabric8.api.ContainerRegistration;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricService;
import io.fabric8.api.GeoLocationService;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.internal.ContainerImpl;
import io.fabric8.utils.HostUtils;
import io.fabric8.zookeeper.ZkDefs;
import io.fabric8.zookeeper.ZkPath;
import io.fabric8.zookeeper.utils.ZooKeeperUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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

@ThreadSafe
@Component(name = "io.fabric8.container.registration.widlfly", label = "Fabric8 Wildfly Container Registration", immediate = true, metatype = false)
@Service({ContainerRegistration.class, ConnectionStateListener.class})
public final class WildflyContainerRegistration extends AbstractComponent implements ContainerRegistration, ConnectionStateListener {

    private transient Logger LOGGER = LoggerFactory.getLogger(WildflyContainerRegistration.class);

    private static final String JMX_REMOTE_PORT = "jboss.management.http.port";
    private static final int DEFAULT_JMX_REMOTE_PORT = 9990;
    private static final String JMX_URL_FORMAT = "service:jmx:http-remoting-jmx://%s:%d";

    private static final String HTTP_PORT = "jboss.management.http.port";
    private static final String HTTPS_PORT = "jboss.management.https.port";
    private static final int DEFAULT_HTTP_PORT = 8080;
    private static final int DEFAULT_HTTPS_PORT = 8443;

    @Reference(referenceInterface = RuntimeProperties.class)
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<RuntimeProperties>();
    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();
    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();
    @Reference(referenceInterface = GeoLocationService.class)
    private final ValidatingReference<GeoLocationService> geoLocationService = new ValidatingReference<GeoLocationService>();


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
        String runtimeIdentity = sysprops.getRuntimeIdentity();
        String version = sysprops.getProperty("fabric.version", ZkDefs.DEFAULT_VERSION);
        String profiles = sysprops.getProperty("fabric.profiles");
        try {
            if (profiles != null) {
                String versionNode = CONFIG_CONTAINER.getPath(runtimeIdentity);
                String profileNode = CONFIG_VERSIONS_CONTAINER.getPath(version, runtimeIdentity);
                ZooKeeperUtils.createDefault(curator.get(), versionNode, version);
                ZooKeeperUtils.createDefault(curator.get(), profileNode, profiles);
            }

            checkAlive();

            String domainsNode = CONTAINER_DOMAINS.getPath(runtimeIdentity);
            Stat stat = ZooKeeperUtils.exists(curator.get(), domainsNode);
            if (stat != null) {
                ZooKeeperUtils.deleteSafe(curator.get(), domainsNode);
            }

            ZooKeeperUtils.createDefault(curator.get(), CONTAINER_BINDADDRESS.getPath(runtimeIdentity), sysprops.getProperty(ZkDefs.BIND_ADDRESS, "0.0.0.0"));
            ZooKeeperUtils.createDefault(curator.get(), CONTAINER_RESOLVER.getPath(runtimeIdentity), getContainerResolutionPolicy(sysprops, curator.get(), runtimeIdentity));
            ZooKeeperUtils.setData(curator.get(), CONTAINER_LOCAL_HOSTNAME.getPath(runtimeIdentity), HostUtils.getLocalHostName());
            ZooKeeperUtils.setData(curator.get(), CONTAINER_LOCAL_IP.getPath(runtimeIdentity), HostUtils.getLocalIp());
            ZooKeeperUtils.setData(curator.get(), CONTAINER_IP.getPath(runtimeIdentity), getContainerPointer(curator.get(), runtimeIdentity));
            ZooKeeperUtils.createDefault(curator.get(), CONTAINER_GEOLOCATION.getPath(runtimeIdentity), geoLocationService.get().getGeoLocation());
            //Check if there are addresses specified as system properties and use them if there is not an existing value in the registry.
            //Mostly usable for adding values when creating containers without an existing ensemble.
            for (String resolver : ZkDefs.VALID_RESOLVERS) {
                String address = sysprops.getProperty(resolver);
                if (address != null && !address.isEmpty() && ZooKeeperUtils.exists(curator.get(), CONTAINER_ADDRESS.getPath(runtimeIdentity, resolver)) == null) {
                    ZooKeeperUtils.setData(curator.get(), CONTAINER_ADDRESS.getPath(runtimeIdentity, resolver), address);
                }
            }

            //We are creating a dummy container object, since this might be called before the actual container is ready.
            Container current = getContainer();
            registerHttp(current);
            registerJmx(current);

            //Set the port range values
            String minimumPort = sysprops.getProperty(ZkDefs.MINIMUM_PORT);
            String maximumPort = sysprops.getProperty(ZkDefs.MAXIMUM_PORT);
            ZooKeeperUtils.createDefault(curator.get(), CONTAINER_PORT_MIN.getPath(runtimeIdentity), minimumPort);
            ZooKeeperUtils.createDefault(curator.get(), CONTAINER_PORT_MAX.getPath(runtimeIdentity), maximumPort);
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
        String runtimeIdentity = sysprops.getRuntimeIdentity();
        String nodeAlive = CONTAINER_ALIVE.getPath(runtimeIdentity);
        Stat stat = ZooKeeperUtils.exists(curator.get(), nodeAlive);
        if (stat != null) {
            if (stat.getEphemeralOwner() != curator.get().getZookeeperClient().getZooKeeper().getSessionId()) {
                ZooKeeperUtils.delete(curator.get(), nodeAlive);
                ZooKeeperUtils.create(curator.get(), nodeAlive, CreateMode.EPHEMERAL);
            }
        } else {
            ZooKeeperUtils.create(curator.get(), nodeAlive, CreateMode.EPHEMERAL);
        }
    }



    private void registerHttp(Container container) throws Exception {
        boolean httpEnabled = isHttpEnabled();
        boolean httpsEnabled = isHttpsEnabled();
        String protocol = httpsEnabled && !httpEnabled ? "https" : "http";
        int httpPort = httpsEnabled && !httpEnabled ? getHttpsPort() : getHttpPort();
        String httpUrl = getHttpUrl(protocol, container.getId(), httpPort);
        ZooKeeperUtils.setData(curator.get(), CONTAINER_HTTP.getPath(container.getId()), httpUrl);
    }

    private boolean isHttpEnabled() throws IOException {
        return true;
    }

    private boolean isHttpsEnabled() throws IOException {
        return true;
    }

    private int getHttpPort() {
        return Integer.parseInt(runtimeProperties.get().getProperty(HTTP_PORT, "" + DEFAULT_HTTP_PORT));
    }

    private int getHttpsPort() throws KeeperException, InterruptedException, IOException {
        return Integer.parseInt(runtimeProperties.get().getProperty(HTTPS_PORT, "" + DEFAULT_HTTPS_PORT));
    }

    private String getHttpUrl(String protocol, String name, int httpConnectionPort) throws IOException, KeeperException, InterruptedException {
        return protocol + "://${zk:" + name + "/ip}:" + httpConnectionPort;
    }

    private void registerJmx(Container container) throws Exception {
        String jmxUrl = getJmxUrl(container.getId(), getJmxPort());
        ZooKeeperUtils.setData(curator.get(), CONTAINER_JMX.getPath(container.getId()), jmxUrl);
    }

    private int getJmxPort() throws KeeperException, InterruptedException, IOException {
        return Integer.parseInt(runtimeProperties.get().getProperty(JMX_REMOTE_PORT, "" + DEFAULT_JMX_REMOTE_PORT));
    }

    private String getJmxUrl(String name, int port) throws IOException, KeeperException, InterruptedException {
        return String.format(JMX_URL_FORMAT, name, port);
    }

    /**
     * Returns the global resolution policy.
     */
    private String getGlobalResolutionPolicy(RuntimeProperties sysprops, CuratorFramework zooKeeper) throws Exception {
        String policy = ZkDefs.LOCAL_HOSTNAME;
        List<String> validResolverList = Arrays.asList(ZkDefs.VALID_RESOLVERS);
        if (ZooKeeperUtils.exists(zooKeeper, ZkPath.POLICIES.getPath(ZkDefs.RESOLVER)) != null) {
            policy = ZooKeeperUtils.getStringData(zooKeeper, ZkPath.POLICIES.getPath(ZkDefs.RESOLVER));
        } else if (sysprops.getProperty(ZkDefs.GLOBAL_RESOLVER_PROPERTY) != null && validResolverList.contains(sysprops.getProperty(ZkDefs.GLOBAL_RESOLVER_PROPERTY))) {
            policy = sysprops.getProperty(ZkDefs.GLOBAL_RESOLVER_PROPERTY);
            ZooKeeperUtils.setData(zooKeeper, ZkPath.POLICIES.getPath("resolver"), policy);
        }
        return policy;
    }

    /**
     * Returns the container specific resolution policy.
     */
    private String getContainerResolutionPolicy(RuntimeProperties sysprops, CuratorFramework zooKeeper, String container) throws Exception {
        String policy = null;
        List<String> validResolverList = Arrays.asList(ZkDefs.VALID_RESOLVERS);
        if (ZooKeeperUtils.exists(zooKeeper, CONTAINER_RESOLVER.getPath(container)) != null) {
            policy = ZooKeeperUtils.getStringData(zooKeeper, CONTAINER_RESOLVER.getPath(container));
        } else if (sysprops.getProperty(ZkDefs.LOCAL_RESOLVER_PROPERTY) != null && validResolverList.contains(sysprops.getProperty(ZkDefs.LOCAL_RESOLVER_PROPERTY))) {
            policy = sysprops.getProperty(ZkDefs.LOCAL_RESOLVER_PROPERTY);
        }

        if (policy == null) {
            policy = getGlobalResolutionPolicy(sysprops, zooKeeper);
        }

        if (policy != null && ZooKeeperUtils.exists(zooKeeper, CONTAINER_RESOLVER.getPath(container)) == null) {
            ZooKeeperUtils.setData(zooKeeper, CONTAINER_RESOLVER.getPath(container), policy);
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
     * Gets the current {@link io.fabric8.api.Container}.
     *
     * @return The current container if registered or a dummy wrapper of the name and ip.
     */
    private Container getContainer() {
        try {
            return fabricService.get().getCurrentContainer();
        } catch (Exception e) {
            final RuntimeProperties sysprops = runtimeProperties.get();
            final String runtimeIdentity = sysprops.getRuntimeIdentity();
            return new ContainerImpl(null, runtimeIdentity, null) {
                @Override
                public String getIp() {
                    try {
                        return ZooKeeperUtils.getSubstitutedPath(curator.get(), CONTAINER_IP.getPath(runtimeIdentity));
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

    void bindGeoLocationService(GeoLocationService service) {
        this.geoLocationService.bind(service);
    }

    void unbindGeoLocationService(GeoLocationService service) {
        this.geoLocationService.unbind(service);
    }
}
