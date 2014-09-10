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
package io.fabric8.internal.cluster;

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
import io.fabric8.common.util.Closeables;
import io.fabric8.internal.ImmutableContainerBuilder;
import io.fabric8.utils.DataStoreUtils;
import io.fabric8.zookeeper.ZkPath;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import static io.fabric8.internal.cluster.Constants.CLUSTER_PROFILE_FORMAT;
import static io.fabric8.internal.cluster.Constants.MEMBER_PROFILE_FORMAT;
import static io.fabric8.internal.cluster.Constants.SERVER_ID_PREFIX;
import static io.fabric8.internal.cluster.Constants.SERVER_PORTS_PATTERN;
import static io.fabric8.internal.cluster.Constants.ZOOKEEPER_CLIENT_PORT_KEY;
import static io.fabric8.internal.cluster.Constants.ZOOKEEPER_SERVER_PID;
import static io.fabric8.internal.cluster.ZooKeeperClusterUtils.cleanUp;
import static io.fabric8.internal.cluster.ZooKeeperClusterUtils.waitForContainersToSwitch;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.exists;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getStringData;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getSubstitutedData;

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
        String ensembleConfigName = "io.fabric8.zookeeper.server.properties";
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

        //We want to pull all the required information as early as possible, before the component deactivates.
        final ACLProvider aclProvider = this.aclProvider.get();
        final String zooKeeperUser = fabricService.get().getZooKeeperUser();
        final String zooKeeperPassword = fabricService.get().getZookeeperPassword();
        final Map<String, Container> allContainers = getAllContainers();
        final List<Container> clusterContainers = filterContainers(allContainers, containers);
        final List<Container> oldContainers = filterContainers(allContainers, getEnsembleContainers());
        final List<Container> containersToAdd = copyAndRemove(clusterContainers, oldContainers);
        final List<Container> containersToRemove = copyAndRemove(oldContainers, clusterContainers);

        validate(containers, allContainers, options);

        ZooKeeperClusterState oldState = null;
        ZooKeeperClusterState newState = null;

        CountDownLatch reactivationLatch = prepareReactivationMonitor();
        try {
            long startMillis = System.currentTimeMillis();

            final int oldClusterId = Integer.parseInt(getStringData(curator.get(), ZkPath.CONFIG_ENSEMBLES.getPath()));
            oldState = getClusterState(oldClusterId, allContainers, aclProvider, options);
            newState = oldState.newCluster();

            newState = newState.updateConfiguration(populateEnsembleProperties(options));
            for (Container container : containersToRemove) {
                newState = newState.removeMember(container);
            }

            for (Container container : containersToAdd) {
                newState = newState.addMember(container);
            }

            Profile clusterProfile = generateClusterProfile(newState);
            List<Profile> memberProfiles = generateMemberProfiles(newState);
            createEnsembleProfiles(clusterProfile, memberProfiles);

            ZooKeeperClusterOperationContext context = ZooKeeperClusterOperationContext.builder()
                    .allContainers(allContainers)
                    .containersToAdd(containersToAdd)
                    .containersToRemove(containersToRemove)
                    .currentState(oldState)
                    .targetState(newState)
                    .usersname(zooKeeperUser)
                    .password(zooKeeperPassword)
                    .aclProvider(aclProvider)
                    .createEnsembleOptions(options)
                    .countDownLatch(reactivationLatch)
                    .build();

            selectOperation(oldContainers, clusterContainers).execute(context);

            reactivationLatch.await(options.getMigrationTimeout() - (System.currentTimeMillis() - startMillis), TimeUnit.MILLISECONDS);
            waitForContainersToSwitch(allContainers.values(), newState);
            cleanUp(newState, oldContainers);
        } catch (Exception e) {
            throw EnsembleModificationFailed.launderThrowable(e);
        } finally {
            Closeables.closeQuietly(oldState);
            Closeables.closeQuietly(newState);
        }
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
                    throw new EnsembleModificationFailed("Container " + c + " is already part of the ensemble.", EnsembleModificationFailed.Reason.CONTAINERS_ALREADY_IN_ENSEMBLE);
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
                if (!current.contains(c)) {
                    throw new EnsembleModificationFailed("Container " + c + " is not part of the ensemble.", EnsembleModificationFailed.Reason.CONTAINERS_NOT_IN_ENSEMBLE);
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
     * Validates that ensemble modification is possible.
     *
     * @param clusterContainers
     * @param allContainers
     * @param options
     * @throws Exception
     */
    private void validate(List<String> clusterContainers, Map<String, Container> allContainers, CreateEnsembleOptions options) {
        try {
            if (clusterContainers == null || clusterContainers.size() == 2) {
                throw new EnsembleModificationFailed("One or at least 3 containers must be used to create a zookeeper ensemble", EnsembleModificationFailed.Reason.INVALID_ARGUMENTS);
            }
            Configuration config = configAdmin.get().getConfiguration(Constants.ZOOKEEPER_CLIENT_PID, null);
            String zooKeeperUrl = config != null && config.getProperties() != null ? (String) config.getProperties().get("zookeeper.url") : null;
            String runtimeIdentity = runtimeProperties.get().getRuntimeIdentity();
            if (zooKeeperUrl == null) {
                if (clusterContainers.size() != 1 || !clusterContainers.get(0).equals(runtimeIdentity)) {
                    throw new EnsembleModificationFailed("The first zookeeper cluster must be configured on this container only.", EnsembleModificationFailed.Reason.INVALID_ARGUMENTS);
                }
                bootstrap.get().create(options);
                return;
            }

            Set<Container> notAliveOrOk = new HashSet<>();
            for (Container container : allContainers.values()) {
                if (!container.isAliveAndOK()) {
                    notAliveOrOk.add(container);
                }
            }

            if (!notAliveOrOk.isEmpty()) {
                throw new EnsembleModificationFailed("Can not modify the zookeeper ensemble if all containers are not running. Containers not ready:" + notAliveOrOk, EnsembleModificationFailed.Reason.CONTAINERS_NOT_ALIVE);
            }

            for (String container : clusterContainers) {
                fabricService.get().getContainer(container);
                if (exists(curator.get(), ZkPath.CONTAINER_ALIVE.getPath(container)) == null) {
                    throw new EnsembleModificationFailed("The container " + container + " is not alive", EnsembleModificationFailed.Reason.CONTAINERS_NOT_ALIVE);
                }
            }
        } catch (Exception e) {
            throw EnsembleModificationFailed.launderThrowable(e);
        }
    }

    private ZooKeeperClusterOperation selectOperation(List<Container> current, List<Container> target) {
        List<Container> containersToAdd = copyAndRemove(target, current);
        List<Container> containersToRemove = copyAndRemove(current, target);

        if (containersToAdd.size() > 0 && current.size() == 1) {
            return new UpgradeFromSingleServerOperation();
        } else if (containersToAdd.size() > 0) {
            return new RollingAddOperation();
        } else if (containersToRemove.size() > 0 && target.size() == 1) {
            return new DowngradeToSingleServerOperation();
        } else {
            return new RollingRemoveOperation();
        }
    }

    /**
     * Populates the global ensemble properties from the specified options.
     *
     * @param options The specified options.
     * @return
     */
    private Map<String, String> populateEnsembleProperties(CreateEnsembleOptions options) {
        Map<String, String> ensembleProperties = new HashMap<>();
        ensembleProperties.put("tickTime", String.valueOf(options.getZooKeeperServerTickTime()));
        ensembleProperties.put("initLimit", String.valueOf(options.getZooKeeperServerInitLimit()));
        ensembleProperties.put("syncLimit", String.valueOf(options.getZooKeeperServerSyncLimit()));
        ensembleProperties.put("dataBaseDir", options.getZooKeeperServerDataDir());
        ensembleProperties.put("dataLogBaseDir", options.getZooKeeperServerDataLogDir());
        return ensembleProperties;
    }

    /**
     * Returns a Map of containers by Id.
     * The container objects are an immutable variation of the original object, with the minimum required fields set.
     * The immutable version doesn't depend on volatile services (or any other service that is meant to go during the process).
     *
     * @return
     */
    private Map<String, Container> getAllContainers() {
        Map<String, Container> result = new HashMap<>();
        for (Container cnt : fabricService.get().getContainers()) {
            Container container = new ImmutableContainerBuilder()
                    .id(cnt.getId())
                    .ip(cnt.getIp())
                    .jmxUrl(cnt.getJmxUrl())
                    .profiles(cnt.getProfiles())
                    .alive(cnt.isAlive())
                    .aliveAndOK(cnt.isAliveAndOK())
                    .build();
            result.put(container.getId(), container);
        }
        return result;
    }

    private static List<Container> filterContainers(Map<String, Container> all, List<String> ids) {
        List<Container> result = new LinkedList<>();

        for (String containerId : ids) {
            result.add(all.get(containerId));
        }
        return result;
    }

    /**
     * Gets the state describing the cluster with the specified id.
     *
     * @param clusterId The id of the cluster.
     * @return
     */
    private ZooKeeperClusterState getClusterState(int clusterId, Map<String, Container> allContainers, ACLProvider aclProvider, CreateEnsembleOptions options) {
        ZooKeeperClusterState state = new ZooKeeperClusterState(clusterId, new HashMap<Integer, ZooKeeperClusterMember>(), Collections.<String, String>emptyMap(), aclProvider, options);
        String versionId = fabricService.get().getDefaultVersionId();
        String profileId = String.format(CLUSTER_PROFILE_FORMAT, clusterId);
        Profile ensProfile = profileRegistry.get().getRequiredProfile(versionId, profileId);
        Map<String, String> parentConfig = ensProfile.getConfiguration(ZOOKEEPER_SERVER_PID);

        if (parentConfig == null) {
            throw new EnsembleModificationFailed("Failed to find old cluster configuration for ID " + clusterId, EnsembleModificationFailed.Reason.ILLEGAL_STATE);
        }

        try {
            //Find members and ports defined in the cluster.
            for (Object n : parentConfig.keySet()) {
                String node = (String) n;
                if (node.startsWith(SERVER_ID_PREFIX)) {
                    Map<String, String> zkconfig = ensProfile.getConfiguration(ZOOKEEPER_SERVER_PID);
                    String data = getSubstitutedData(curator.get(), zkconfig.get(node));
                    int membershipId = Integer.parseInt(node.substring(SERVER_ID_PREFIX.length()));
                    String memberProfileId = String.format(MEMBER_PROFILE_FORMAT, clusterId, membershipId);
                    Container container = findContainerByClusterAndId(memberProfileId, allContainers);

                    Profile memberProfile = profileRegistry.get().getRequiredProfile(versionId, memberProfileId);
                    Map<String, String> memberConfig = memberProfile.getConfiguration(ZOOKEEPER_SERVER_PID);

                    int peerPort = readServerPort(data, ZooKeeperPortType.PEER.name());
                    int electionPort = readServerPort(data, ZooKeeperPortType.ELECTION.name());
                    int clientPort = Integer.parseInt(memberConfig.get(ZOOKEEPER_CLIENT_PORT_KEY));
                    ZooKeeperClusterMember member = ZooKeeperClusterMember.create(container, clientPort, peerPort, electionPort);
                    state = state.addMember(member, membershipId);
                }
            }

            //Check if we have a standalone server.
            if (state.getMembers().isEmpty()) {
                String memberProfileId = String.format(MEMBER_PROFILE_FORMAT, clusterId, 1);
                Profile memberProfile = profileRegistry.get().getRequiredProfile(versionId, memberProfileId);
                Container container = findContainerByClusterAndId(memberProfileId, allContainers);
                Map<String, String> memberConfig = memberProfile.getConfiguration(ZOOKEEPER_SERVER_PID);

                int clientPort = Integer.parseInt(memberConfig.get(ZOOKEEPER_CLIENT_PORT_KEY));
                ZooKeeperClusterMember member = ZooKeeperClusterMember.create(container, clientPort, ZooKeeperPortType.PEER.getValue(), ZooKeeperPortType.ELECTION.getValue());
                state = state.addMember(member, 1);
            }

        } catch (Exception e) {
            throw new EnsembleModificationFailed("Failed to get the state of cluster with id:" + clusterId, e, EnsembleModificationFailed.Reason.ILLEGAL_STATE);
        }
        return state;
    }

    /**
     * Stores the profiles to the datastore.
     *
     * @param clusterProfile The cluster profile.
     * @param memberProfiles A list with the ensemble member profiles.
     */
    private void createEnsembleProfiles(Profile clusterProfile, List<Profile> memberProfiles) {
        LockHandle writeLock = profileRegistry.get().aquireWriteLock();
        try {
            profileRegistry.get().createProfile(clusterProfile);

            // Create the member profiles
            for (Profile memberProfile : memberProfiles) {
                LOGGER.info("Creating member ensemble profile: {}", memberProfile);
                profileRegistry.get().createProfile(memberProfile);
            }
        } finally {
            writeLock.unlock();
        }
    }


    private CountDownLatch prepareReactivationMonitor() {
        final CountDownLatch latch = new CountDownLatch(1);
        // Perform cleanup when the new datastore has been registered.
        runtimeProperties.get().putRuntimeAttribute(DataStoreTemplate.class, new DataStoreTemplate() {
            @Override
            public void doWith(ProfileRegistry profileRegistry, DataStore dataStore) {
                latch.countDown();
            }
        });
        return latch;
    }

    /**
     * Finds the container id associated with the specified memberProfileId.
     *
     * @param memberProfileId The member profile id id.
     * @return The container id or null if no container is associated.
     */
    private Container findContainerByClusterAndId(String memberProfileId, Map<String, Container> allContainers) {

        for (Container container : allContainers.values()) {
            for (Profile profile : Arrays.asList(container.getProfiles())) {
                if (profile.getId().equals(memberProfileId)) {
                    return container;
                }
            }
        }
        return null;
    }


    private Profile generateClusterProfile(ZooKeeperClusterState state) {
        String versionId = fabricService.get().getDefaultVersion().getId();
        String ensembleProfileId = String.format(CLUSTER_PROFILE_FORMAT, state.getClusterId());
        IllegalStateAssertion.assertFalse(profileRegistry.get().hasProfile(versionId, ensembleProfileId), "Profile already exists: " + versionId + "/" + ensembleProfileId);
        return ProfileBuilder.Factory.create(versionId, ensembleProfileId)
                .addAttribute(Profile.ABSTRACT, "true")
                .addAttribute(Profile.HIDDEN, "true")
                .addConfiguration(ZOOKEEPER_SERVER_PID, state.getClusterConfiguration()).getProfile();
    }

    private List<Profile> generateMemberProfiles(ZooKeeperClusterState state) {
        List<Profile> profiles = new LinkedList<>();

        String versionId = fabricService.get().getDefaultVersion().getId();
        for (Map.Entry<Integer, ZooKeeperClusterMember> entry : state.getMembers().entrySet()) {
            int membershipId = entry.getKey();
            ZooKeeperClusterMember member = entry.getValue();
            String clusterProfileId = String.format(CLUSTER_PROFILE_FORMAT, state.getClusterId());
            String memberProfileId = String.format(MEMBER_PROFILE_FORMAT, state.getClusterId(), membershipId);
            IllegalStateAssertion.assertFalse(profileRegistry.get().hasProfile(versionId, memberProfileId), "Profile already exists: " + versionId + "/" + memberProfileId);
            profiles.add(ProfileBuilder.Factory.create(versionId, memberProfileId)
                    .addAttribute(Profile.ABSTRACT, "true")
                    .addAttribute(Profile.HIDDEN, "true")
                    .addAttribute(Profile.PARENTS, clusterProfileId)
                    .addConfiguration(ZOOKEEPER_SERVER_PID, state.getMemberConfiguration(member.getId())).getProfile());
        }

        return profiles;
    }


    private static int readServerPort(String line, String type) {
        Matcher m = SERVER_PORTS_PATTERN.matcher(line);
        IllegalStateAssertion.assertTrue(m.matches(), "Illegal server definition:" + line);
        return Integer.parseInt(m.group(type));
    }

    private static <T> List<T> copyAndRemove(List<T> toCopy, List<T> toRemove) {
        List<T> result = new LinkedList<>(toCopy);
        result.removeAll(toRemove);
        return result;
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
