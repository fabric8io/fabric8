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
package io.fabric8.runtime.embedded.registration;

import io.fabric8.api.ContainerRegistration;
import io.fabric8.api.GeoLocationService;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.utils.HostUtils;
import io.fabric8.utils.SystemProperties;
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
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static io.fabric8.zookeeper.ZkPath.CONFIG_CONTAINER;
import static io.fabric8.zookeeper.ZkPath.CONFIG_VERSIONS_CONTAINER;
import static io.fabric8.zookeeper.ZkPath.CONTAINER_ADDRESS;
import static io.fabric8.zookeeper.ZkPath.CONTAINER_ALIVE;
import static io.fabric8.zookeeper.ZkPath.CONTAINER_BINDADDRESS;
import static io.fabric8.zookeeper.ZkPath.CONTAINER_DOMAINS;
import static io.fabric8.zookeeper.ZkPath.CONTAINER_GEOLOCATION;
import static io.fabric8.zookeeper.ZkPath.CONTAINER_IP;
import static io.fabric8.zookeeper.ZkPath.CONTAINER_LOCAL_HOSTNAME;
import static io.fabric8.zookeeper.ZkPath.CONTAINER_LOCAL_IP;
import static io.fabric8.zookeeper.ZkPath.CONTAINER_PORT_MAX;
import static io.fabric8.zookeeper.ZkPath.CONTAINER_PORT_MIN;
import static io.fabric8.zookeeper.ZkPath.CONTAINER_RESOLVER;

@ThreadSafe
@Component(name = "io.fabric8.container.registration.embedded", label = "Fabric8 Embedded Registration", immediate = true, metatype = false)
@Service({ContainerRegistration.class, ConnectionStateListener.class})
public class EmbeddedContainerRegistration extends AbstractComponent implements ContainerRegistration, ConnectionStateListener {

    private transient Logger LOGGER = LoggerFactory.getLogger(EmbeddedContainerRegistration.class);

    @Reference(referenceInterface = RuntimeProperties.class)
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<RuntimeProperties>();
    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();
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
        String karafName = sysprops.getRuntimeIdentity();
        String version = sysprops.getProperty("fabric.version", ZkDefs.DEFAULT_VERSION);
        String profiles = sysprops.getProperty("fabric.profiles");
        try {
            if (profiles != null) {
                String versionNode = CONFIG_CONTAINER.getPath(karafName);
                String profileNode = CONFIG_VERSIONS_CONTAINER.getPath(version, karafName);
                ZooKeeperUtils.createDefault(curator.get(), versionNode, version);
                ZooKeeperUtils.createDefault(curator.get(), profileNode, profiles);
            }

            checkAlive();

            String domainsNode = CONTAINER_DOMAINS.getPath(karafName);
            Stat stat = ZooKeeperUtils.exists(curator.get(), domainsNode);
            if (stat != null) {
                ZooKeeperUtils.deleteSafe(curator.get(), domainsNode);
            }

            ZooKeeperUtils.createDefault(curator.get(), CONTAINER_BINDADDRESS.getPath(karafName), sysprops.getProperty(ZkDefs.BIND_ADDRESS, "0.0.0.0"));
            ZooKeeperUtils.createDefault(curator.get(), CONTAINER_RESOLVER.getPath(karafName), getContainerResolutionPolicy(sysprops, curator.get(), karafName));
            ZooKeeperUtils.setData(curator.get(), CONTAINER_LOCAL_HOSTNAME.getPath(karafName), HostUtils.getLocalHostName());
            ZooKeeperUtils.setData(curator.get(), CONTAINER_LOCAL_IP.getPath(karafName), HostUtils.getLocalIp());
            ZooKeeperUtils.setData(curator.get(), CONTAINER_IP.getPath(karafName), getContainerPointer(curator.get(), karafName));
            ZooKeeperUtils.createDefault(curator.get(), CONTAINER_GEOLOCATION.getPath(karafName), geoLocationService.get().getGeoLocation());
            //Check if there are addresses specified as system properties and use them if there is not an existing value in the registry.
            //Mostly usable for adding values when creating containers without an existing ensemble.
            for (String resolver : ZkDefs.VALID_RESOLVERS) {
                String address = sysprops.getProperty(resolver);
                if (address != null && !address.isEmpty() && ZooKeeperUtils.exists(curator.get(), CONTAINER_ADDRESS.getPath(karafName, resolver)) == null) {
                    ZooKeeperUtils.setData(curator.get(), CONTAINER_ADDRESS.getPath(karafName, resolver), address);
                }
            }

            //Set the port range values
            String minimumPort = sysprops.getProperty(ZkDefs.MINIMUM_PORT);
            String maximumPort = sysprops.getProperty(ZkDefs.MAXIMUM_PORT);
            ZooKeeperUtils.createDefault(curator.get(), CONTAINER_PORT_MIN.getPath(karafName), minimumPort);
            ZooKeeperUtils.createDefault(curator.get(), CONTAINER_PORT_MAX.getPath(karafName), maximumPort);
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

    void bindGeoLocationService(GeoLocationService service) {
        this.geoLocationService.bind(service);
    }

    void ubindGeoLocationService(GeoLocationService service) {
        this.geoLocationService.unbind(service);
    }
}
