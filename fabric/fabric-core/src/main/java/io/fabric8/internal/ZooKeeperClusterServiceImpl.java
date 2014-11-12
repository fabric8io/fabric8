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
package io.fabric8.internal;

import static io.fabric8.utils.Ports.mapPortToRange;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.copy;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.exists;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getStringData;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getSubstitutedData;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getSubstitutedPath;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;

import com.google.common.base.Charsets;
import io.fabric8.api.Constants;
import io.fabric8.api.Container;
import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.api.CreateEnsembleOptions.Builder;
import io.fabric8.api.DataStore;
import io.fabric8.api.DataStoreTemplate;
import io.fabric8.api.EnsembleModificationFailed;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricService;
import io.fabric8.api.LockHandle;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileRegistry;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.ZooKeeperClusterBootstrap;
import io.fabric8.api.ZooKeeperClusterService;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.service.ContainerTemplate;
import io.fabric8.service.JmxTemplateSupport;
import io.fabric8.utils.DataStoreUtils;
import io.fabric8.utils.PasswordEncoder;
import io.fabric8.utils.Ports;
import io.fabric8.zookeeper.ZkPath;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.retry.RetryOneTime;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import io.fabric8.api.gravia.IllegalStateAssertion;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
@Component(name = "io.fabric8.zookeeper.cluster.service", label = "Fabric8 ZooKeeper Cluster Service", metatype = false)
@Service(ZooKeeperClusterService.class)
public final class ZooKeeperClusterServiceImpl extends AbstractComponent implements ZooKeeperClusterService {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ZooKeeperClusterServiceImpl.class);

    @Reference(referenceInterface = ACLProvider.class)
    private final ValidatingReference<ACLProvider> aclProvider = new ValidatingReference<ACLProvider>();
    @Reference(referenceInterface = ConfigurationAdmin.class)
    private final ValidatingReference<ConfigurationAdmin> configAdmin = new ValidatingReference<>();
    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();
    @Reference(referenceInterface = DataStore.class)
    private final ValidatingReference<DataStore> dataStore = new ValidatingReference<DataStore>();
    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();
    @Reference(referenceInterface = ProfileRegistry.class)
    private final ValidatingReference<ProfileRegistry> profileRegistry = new ValidatingReference<>();
    @Reference(referenceInterface = RuntimeProperties.class)
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<RuntimeProperties>();
    @Reference(referenceInterface = ZooKeeperClusterBootstrap.class)
    private final ValidatingReference<ZooKeeperClusterBootstrap> bootstrap = new ValidatingReference<>();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    public List<String> getEnsembleContainers() {
        assertValid();
        try {
            Configuration[] configs = configAdmin.get().listConfigurations("(service.pid=" + Constants.ZOOKEEPER_CLIENT_PID + ")");
            if (configs == null || configs.length == 0) {
                return Collections.emptyList();
            }
            List<String> list = new ArrayList<String>();
            if (exists(curator.get(), ZkPath.CONFIG_ENSEMBLES.getPath()) != null) {
                String clusterId = getStringData(curator.get(), ZkPath.CONFIG_ENSEMBLES.getPath());
                String containers = getStringData(curator.get(), ZkPath.CONFIG_ENSEMBLE.getPath(clusterId));
                Collections.addAll(list, containers.split(","));
            }
            return list;
        } catch (Exception e) {
            throw new FabricException("Unable to load zookeeper quorum containers", e);
        }
    }

    public String getZooKeeperUrl() {
        assertValid();
        return fabricService.get().getZookeeperUrl();
    }

    public String getZookeeperPassword() {
        assertValid();
        return fabricService.get().getZookeeperPassword();
    }

    @Override
    public Map<String, String> getEnsembleConfiguration() throws Exception {
        String clusterId = getStringData(curator.get(), ZkPath.CONFIG_ENSEMBLES.getPath());
        String versionId = dataStore.get().getDefaultVersion();
        String profileId = "fabric-ensemble-" + clusterId;
        String ensembleConfigName = "io.fabric8.zookeeper.server-" + clusterId + ".properties";
        Profile ensembleProfile = profileRegistry.get().getRequiredProfile(versionId, profileId);
        Map<String, byte[]> fileconfigs = ensembleProfile.getFileConfigurations();
        return DataStoreUtils.toMap(fileconfigs.get(ensembleConfigName));
     }

    public void createCluster(List<String> containers) {
        assertValid();
        RuntimeProperties sysprops = runtimeProperties.get();
        createCluster(containers, CreateEnsembleOptions.builder().fromRuntimeProperties(sysprops).build());
    }

    public void createCluster(final List<String> containers, CreateEnsembleOptions options) {
        assertValid();
        try {
            List<String> oldContainers = getEnsembleContainers();
            if (containers == null || containers.size() == 2) {
                throw new EnsembleModificationFailed("One or at least 3 containers must be used to create a zookeeper ensemble", EnsembleModificationFailed.Reason.INVALID_ARGUMENTS);
            }
            Configuration config = configAdmin.get().getConfiguration(Constants.ZOOKEEPER_CLIENT_PID, null);
            String zooKeeperUrl = config != null && config.getProperties() != null ? (String) config.getProperties().get("zookeeper.url") : null;
            String karafName = runtimeProperties.get().getRuntimeIdentity();
            if (zooKeeperUrl == null) {
                if (containers.size() != 1 || !containers.get(0).equals(karafName)) {
                    throw new EnsembleModificationFailed("The first zookeeper cluster must be configured on this container only.", EnsembleModificationFailed.Reason.INVALID_ARGUMENTS);
                }
                bootstrap.get().create(options);
                return;
            }

            Container[] allContainers = fabricService.get().getContainers();
            Set<Container> notAliveOrOk = new HashSet<Container>();
            for (Container container : allContainers) {
                if (!container.isAliveAndOK()) {
                    notAliveOrOk.add(container);
                }
            }

            if (!notAliveOrOk.isEmpty()) {
                throw new EnsembleModificationFailed("Can not modify the zookeeper ensemble if all containers are not running. Containers not ready:" + notAliveOrOk, EnsembleModificationFailed.Reason.CONTAINERS_NOT_ALIVE);
            }

            String versionId = dataStore.get().getDefaultVersion();

            for (String container : containers) {
                fabricService.get().getContainer(container);
                if (exists(curator.get(), ZkPath.CONTAINER_ALIVE.getPath(container)) == null) {
                    throw new EnsembleModificationFailed("The container " + container + " is not alive", EnsembleModificationFailed.Reason.CONTAINERS_NOT_ALIVE);
                }
            }

            // Find used zookeeper ports
            Map<String, List<Integer>> usedPorts = new HashMap<String, List<Integer>>();
            final String oldClusterId = getStringData(curator.get(), ZkPath.CONFIG_ENSEMBLES.getPath());
            if (oldClusterId != null) {
                String profileId = "fabric-ensemble-" + oldClusterId;
                String pid = "io.fabric8.zookeeper.server-" + oldClusterId;

                Profile ensProfile = profileRegistry.get().getRequiredProfile(versionId, profileId);
                Map<String, String> p = ensProfile.getConfiguration(pid);

                if (p == null) {
                    throw new EnsembleModificationFailed("Failed to find old cluster configuration for ID " + oldClusterId, EnsembleModificationFailed.Reason.ILLEGAL_STATE);
                }

                for (Object n : p.keySet()) {
                    String node = (String) n;
                    if (node.startsWith("server.")) {
                        Map<String, String> zkconfig = ensProfile.getConfiguration("io.fabric8.zookeeper.server-" + oldClusterId);
                        String data = getSubstitutedData(curator.get(), zkconfig.get(node));
                        addUsedPorts(usedPorts, data);
                    }
                }

                Profile defaultProfile = profileRegistry.get().getRequiredProfile(versionId, "default");
                Map<String, String> zkConfig = defaultProfile.getConfiguration(Constants.ZOOKEEPER_CLIENT_PID);
                if (zkConfig == null) {
                    throw new FabricException("Failed to find old zookeeper configuration in default profile");
                }
                String zkUrl = getSubstitutedData(curator.get(), zkConfig.get("zookeeper.url"));
                for (String data : zkUrl.split(",")) {
                    addUsedPorts(usedPorts, data);
                }
            }

            String newClusterId;
            if (oldClusterId == null) {
                newClusterId = "0000";
            } else {
                newClusterId = new DecimalFormat("0000").format(Integer.parseInt(oldClusterId) + 1);
            }

            // Ensemble properties
            Properties ensembleProperties = new Properties();
            String ensemblePropertiesName = "io.fabric8.zookeeper.server-" + newClusterId + ".properties";
            ensembleProperties.put("tickTime", String.valueOf(options.getZooKeeperServerTickTime()));
            ensembleProperties.put("initLimit", String.valueOf(options.getZooKeeperServerInitLimit()));
            ensembleProperties.put("syncLimit", String.valueOf(options.getZooKeeperServerSyncLimit()));
            ensembleProperties.put("dataDir", options.getZooKeeperServerDataDir() + File.separator + newClusterId);
            
            // create new ensemble
            String ensembleProfileId = "fabric-ensemble-" + newClusterId;
            IllegalStateAssertion.assertFalse(profileRegistry.get().hasProfile(versionId, ensembleProfileId), "Profile already exists: " + versionId + "/" + ensembleProfileId);
            ProfileBuilder ensembleProfileBuilder = ProfileBuilder.Factory.create(versionId, ensembleProfileId);
            ensembleProfileBuilder.addAttribute(Profile.ABSTRACT, "true").addAttribute(Profile.HIDDEN, "true");

            int index = 1;
            String connectionUrl = "";
            String realConnectionUrl = "";
            String containerList = "";
            List<Profile> memberProfiles = new ArrayList<>();
            for (String container : containers) {
                String ip = getSubstitutedPath(curator.get(), ZkPath.CONTAINER_IP.getPath(container));

                String minimumPort = String.valueOf(Ports.MIN_PORT_NUMBER);
                String maximumPort = String.valueOf(Ports.MAX_PORT_NUMBER);
                String bindAddress = "0.0.0.0";

                if (exists(curator.get(), ZkPath.CONTAINER_PORT_MIN.getPath(container)) != null) {
                    minimumPort = getSubstitutedPath(curator.get(), ZkPath.CONTAINER_PORT_MIN.getPath(container));
                }

                if (exists(curator.get(), ZkPath.CONTAINER_PORT_MAX.getPath(container)) != null) {
                    maximumPort = getSubstitutedPath(curator.get(), ZkPath.CONTAINER_PORT_MAX.getPath(container));
                }

                if (exists(curator.get(), ZkPath.CONTAINER_BINDADDRESS.getPath(container)) != null) {
                    bindAddress = getSubstitutedPath(curator.get(), ZkPath.CONTAINER_BINDADDRESS.getPath(container));
                }

                // Ensemble member properties
                Properties memberProperties = new Properties();
                String memberPropertiesName = "io.fabric8.zookeeper.server-" + newClusterId + ".properties";
                String port1 = publicPort(container, Integer.toString(findPort(usedPorts, ip, mapPortToRange(Ports.DEFAULT_ZOOKEEPER_SERVER_PORT, minimumPort, maximumPort))));
                if (containers.size() > 1) {
                    String port2 = publicPort(container, Integer.toString(findPort(usedPorts, ip, mapPortToRange(Ports.DEFAULT_ZOOKEEPER_PEER_PORT, minimumPort, maximumPort))));
                    String port3 = publicPort(container, Integer.toString(findPort(usedPorts, ip, mapPortToRange(Ports.DEFAULT_ZOOKEEPER_ELECTION_PORT, minimumPort, maximumPort))));
                    ensembleProperties.put("server." + Integer.toString(index), "${zk:" + container + "/ip}:" + port2 + ":" + port3);
                    memberProperties.put("server.id", Integer.toString(index));
                }
                memberProperties.put("clientPort", port1);
                memberProperties.put("clientPortAddress", bindAddress);

                // Create ensemble member profile
                String memberProfileId = "fabric-ensemble-" + newClusterId + "-" + index;
                IllegalStateAssertion.assertFalse(profileRegistry.get().hasProfile(versionId, memberProfileId), "Profile already exists: " + versionId + "/" + memberProfileId);
                ProfileBuilder memberProfileBuilder = ProfileBuilder.Factory.create(versionId, memberProfileId);
                memberProfileBuilder.addAttribute(Profile.HIDDEN, "true").addAttribute(Profile.PARENTS, ensembleProfileId);
                memberProfileBuilder.addFileConfiguration(memberPropertiesName, DataStoreUtils.toBytes(memberProperties));
                memberProfiles.add(memberProfileBuilder.getProfile());

                if (connectionUrl.length() > 0) {
                    connectionUrl += ",";
                    realConnectionUrl += ",";
                }
                connectionUrl += "${zk:" + container + "/ip}:" + port1;
                realConnectionUrl += ip + ":" + port1;
                if (containerList.length() > 0) {
                    containerList += ",";
                }
                containerList += container;
                index++;
            }

            LockHandle writeLock = profileRegistry.get().aquireWriteLock();
            try {
                // Create the ensemble profile
                ensembleProfileBuilder.addFileConfiguration(ensemblePropertiesName, DataStoreUtils.toBytes(ensembleProperties));
                Profile ensembleProfile = ensembleProfileBuilder.getProfile();
                LOGGER.info("Creating parent ensemble profile: {}", ensembleProfile);
                profileRegistry.get().createProfile(ensembleProfile);
                
                // Create the member profiles
                for (Profile memberProfile : memberProfiles) {
                    LOGGER.info("Creating member ensemble profile: {}", memberProfile);
                    profileRegistry.get().createProfile(memberProfile);
                }
            } finally {
                writeLock.unlock();
            }
            
            index = 1;
            for (String container : containers) {
                // add this container to the ensemble
                List<String> profiles = new LinkedList<String>(dataStore.get().getContainerProfiles(container));
                profiles.add("fabric-ensemble-" + newClusterId + "-" + Integer.toString(index));
                LOGGER.info("Assigning member ensemble profile with id: {} to {}.", ensembleProfileId + "-" + index, container);
                dataStore.get().setContainerProfiles(container, profiles);
                index++;
            }

            Profile defaultProfile = profileRegistry.get().getRequiredProfile(versionId, "default");
            Map<String, String> zkConfig = defaultProfile.getConfiguration(Constants.ZOOKEEPER_CLIENT_PID);
            if (oldClusterId != null) {
                Properties properties = DataStoreUtils.toProperties(zkConfig);
                properties.put("zookeeper.url", getSubstitutedData(curator.get(), realConnectionUrl));
                properties.put("zookeeper.password", options.getZookeeperPassword());
                CuratorFramework dst = CuratorFrameworkFactory.builder().connectString(realConnectionUrl).retryPolicy(new RetryOneTime(500))
                        .aclProvider(aclProvider.get()).authorization("digest", ("fabric:" + options.getZookeeperPassword()).getBytes()).sessionTimeoutMs(30000)
                        .connectionTimeoutMs((int) options.getMigrationTimeout()).build();
                dst.start();
                try {
                    long t0 = System.currentTimeMillis();
                    LOGGER.info("Waiting for ensemble {} to become ready.", newClusterId);
                    if (!dst.getZookeeperClient().blockUntilConnectedOrTimedOut()) {
                        throw new EnsembleModificationFailed("Timed out connecting to new ensemble.", EnsembleModificationFailed.Reason.TIMEOUT);
                    }
                    LOGGER.info("Copying data from the old ensemble to the new one");
                    copy(curator.get(), dst, "/fabric");
                    setData(dst, ZkPath.CONFIG_ENSEMBLES.getPath(), newClusterId);
                    setData(dst, ZkPath.CONFIG_ENSEMBLE.getPath(newClusterId), containerList);

                    // Perform cleanup when the new datastore has been registered.
                    final AtomicReference<DataStore> result = new AtomicReference<DataStore>();
                    runtimeProperties.get().putRuntimeAttribute(DataStoreTemplate.class, new DataStoreTemplate() {
                        @Override
                        public void doWith(ProfileRegistry profileRegistry, DataStore dataStore) {
                            synchronized (result) {
                                result.set(dataStore);
                                result.notifyAll();
                            }
                        }
                    });

                    LOGGER.info("Migrating containers to the new ensemble using url {}.", connectionUrl);
                    setData(dst, ZkPath.CONFIG_ENSEMBLE_PASSWORD.getPath(), PasswordEncoder.encode(options.getZookeeperPassword()));
                    setData(dst, ZkPath.CONFIG_ENSEMBLE_URL.getPath(), connectionUrl);
                    curator.get().inTransaction()
                            .setData().forPath(ZkPath.CONFIG_ENSEMBLE_PASSWORD.getPath(),  PasswordEncoder.encode(options.getZookeeperPassword()).getBytes(Charsets.UTF_8))
                            .and()
                            .setData().forPath(ZkPath.CONFIG_ENSEMBLE_URL.getPath(), connectionUrl.getBytes(Charsets.UTF_8))
                            .and().commit();

                    // Wait until all containers switched
                    boolean allStarted = false;
                    while (!allStarted && System.currentTimeMillis() - t0 < options.getMigrationTimeout()) {
                        allStarted = true;
                        for (Container container : allContainers) {
                            allStarted &= exists(dst, ZkPath.CONTAINER_ALIVE.getPath(container.getId())) != null;
                        }
                        if (!allStarted) {
                            Thread.sleep(1000);
                        }
                    }
                    if (!allStarted) {
                        throw new EnsembleModificationFailed("Timeout waiting for containers to join the new ensemble", EnsembleModificationFailed.Reason.TIMEOUT);
                    }
                    LOGGER.info("Migration successful. Cleaning up");
                    // Wait until the new datastore has been registered
                    synchronized (result) {
                        if (result.get() == null) {
                            result.wait();
                        }
                    }
                    // Remove old profiles
                    for (String container : oldContainers) {
                        cleanUpEnsembleProfiles(result.get(), container, oldClusterId);
                    }

                } finally {
                    dst.close();
                }
            } else {
                ProfileBuilder builder = ProfileBuilder.Factory.createFrom(defaultProfile);
                zkConfig = new HashMap<>(zkConfig);
                zkConfig.put("zookeeper.password", "${zk:" + ZkPath.CONFIG_ENSEMBLE_PASSWORD.getPath() + "}");
                zkConfig.put("zookeeper.url", "${zk:" + ZkPath.CONFIG_ENSEMBLE_URL.getPath() + "}");
                builder.addConfiguration(Constants.ZOOKEEPER_CLIENT_PID, zkConfig);
                profileRegistry.get().updateProfile(builder.getProfile());
            }
        } catch (Exception e) {
            throw EnsembleModificationFailed.launderThrowable(e);
        }
    }

    private String publicPort(String containerName, final String port) {
        FabricService fabric = fabricService.get();
        Container container = fabric.getContainer(containerName);

        ContainerTemplate containerTemplate = new ContainerTemplate(container, fabric.getZooKeeperUser(), fabric.getZookeeperPassword(), false);
        return containerTemplate.execute(new JmxTemplateSupport.JmxConnectorCallback<String>() {
            @Override
            public String doWithJmxConnector(JMXConnector connector) throws Exception {
                return connector.getMBeanServerConnection().invoke(new ObjectName("io.fabric8:type=Fabric"), "getPublicPortOnCurrentContainer", new Object[]{new Integer(port)}, new String[]{"int"}).toString();
            }
        });
    }

    public void addToCluster(List<String> containers) {
        assertValid();
        CreateEnsembleOptions options = CreateEnsembleOptions.builder().zookeeperPassword(fabricService.get().getZookeeperPassword()).build();
        addToCluster(containers, options);
    }

    /**
     * Adds the containers to the cluster.
     */
    @Override
    public void addToCluster(List<String> containers, CreateEnsembleOptions options) {
        assertValid();
        try {
            List<String> current = getEnsembleContainers();
            for (String c : containers) {
                if (current.contains(c)) {
                    throw new EnsembleModificationFailed("Container " + c + " is already part of the ensemble." , EnsembleModificationFailed.Reason.CONTAINERS_ALREADY_IN_ENSEMBLE);
                } else {
                    current.add(c);
                }
            }

            createCluster(current, options);
        } catch (Exception e) {
            throw EnsembleModificationFailed.launderThrowable(e);
        }
    }

    public void removeFromCluster(List<String> containers) {
        assertValid();
        Builder<?> builder = CreateEnsembleOptions.builder();
        String password = fabricService.get().getZookeeperPassword();
        CreateEnsembleOptions options = builder.zookeeperPassword(password).build();
        removeFromCluster(containers, options);
    }

    /**
     * Removes the containers from the cluster.
     */
    @Override
    public void removeFromCluster(List<String> containers, CreateEnsembleOptions options) {
        assertValid();
        try {
            List<String> current = getEnsembleContainers();
            for (String c : containers) {
                if (! current.contains(c)) {
                    throw new EnsembleModificationFailed("Container " + c + " is not part of the ensemble." , EnsembleModificationFailed.Reason.CONTAINERS_NOT_IN_ENSEMBLE);
                } else {
                    current.remove(c);
                }
            }

            createCluster(current, options);
        } catch (Exception e) {
            throw EnsembleModificationFailed.launderThrowable(e);
        }
    }

    /**
     * Removes all ensemble profiles matching the clusterId from the container.
     */
    private void cleanUpEnsembleProfiles(DataStore dataStore, String container, String clusterId) {
        List<String> profiles = new LinkedList<String>(dataStore.getContainerProfiles(container));
        List<String> toRemove = new LinkedList<String>();
        for (String p : profiles) {
            if (p.startsWith("fabric-ensemble-" + clusterId)) {
                toRemove.add(p);
            }
        }
        profiles.removeAll(toRemove);
        dataStore.setContainerProfiles(container, profiles);
    }

    private int findPort(Map<String, List<Integer>> usedPorts, String ip, int port) {
        List<Integer> ports = usedPorts.get(ip);
        if (ports == null) {
            ports = new ArrayList<Integer>();
            usedPorts.put(ip, ports);
        }
        for (;;) {
            if (!ports.contains(port)) {
                ports.add(port);
                return port;
            }
            port++;
        }
    }

    private void addUsedPorts(Map<String, List<Integer>> usedPorts, String data) {
        String[] parts = data.split(":");
        List<Integer> ports = usedPorts.get(parts[0]);
        if (ports == null) {
            ports = new ArrayList<Integer>();
            usedPorts.put(parts[0], ports);
        }
        for (int i = 1; i < parts.length; i++) {
            ports.add(Integer.parseInt(parts[i]));
        }
    }

    void bindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.bind(service);
    }

    void unbindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.unbind(service);
    }

    void bindBootstrap(ZooKeeperClusterBootstrap bootstrap) {
        this.bootstrap.bind(bootstrap);
    }

    void unbindBootstrap(ZooKeeperClusterBootstrap bootstrap) {
        this.bootstrap.unbind(bootstrap);
    }

    void bindAclProvider(ACLProvider aclProvider) {
        this.aclProvider.bind(aclProvider);
    }

    void unbindAclProvider(ACLProvider aclProvider) {
        this.aclProvider.unbind(aclProvider);
    }

    void bindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.bind(service);
    }

    void unbindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.unbind(service);
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }

    void bindDataStore(DataStore dataStore) {
        this.dataStore.bind(dataStore);
    }

    void unbindDataStore(DataStore dataStore) {
        this.dataStore.unbind(dataStore);
    }
    
    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }

    void bindProfileRegistry(ProfileRegistry service) {
        this.profileRegistry.bind(service);
    }
    void unbindProfileRegistry(ProfileRegistry service) {
        this.profileRegistry.unbind(service);
    }
}
