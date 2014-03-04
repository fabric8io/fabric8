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

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.deleteSafe;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.exists;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getByteData;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getChildren;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getStringData;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getSubstitutedPath;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;
import io.fabric8.api.Constants;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.CreateContainerOptions;
import io.fabric8.api.DataStore;
import io.fabric8.api.DataStoreRegistrationHandler;
import io.fabric8.api.DataStoreTemplate;
import io.fabric8.api.FabricException;
import io.fabric8.api.PlaceholderResolver;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.api.visibility.VisibleForTesting;
import io.fabric8.utils.Base64Encoder;
import io.fabric8.utils.Closeables;
import io.fabric8.utils.DataStoreUtils;
import io.fabric8.utils.ObjectUtils;
import io.fabric8.utils.Strings;
import io.fabric8.utils.SystemProperties;
import io.fabric8.zookeeper.ZkDefs;
import io.fabric8.zookeeper.ZkPath;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
public abstract class AbstractDataStore<T extends DataStore> extends AbstractComponent implements DataStore, PathChildrenCacheListener {

    private static final transient Logger LOG = LoggerFactory.getLogger(AbstractDataStore.class);

    public static final String REQUIREMENTS_JSON_PATH = "/fabric/configs/io.fabric8.requirements.json";
    public static final String JVM_OPTIONS_PATH = "/fabric/configs/io.fabric8.containers.jvmOptions";

    private final ValidatingReference<DataStoreRegistrationHandler> registrationHandler = new ValidatingReference<DataStoreRegistrationHandler>();
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<RuntimeProperties>();
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

    private final ExecutorService callbacksExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService cacheExecutor = Executors.newSingleThreadExecutor();

    private final CopyOnWriteArrayList<Runnable> callbacks = new CopyOnWriteArrayList<Runnable>();
    private Map<String, String> dataStoreProperties;
    private TreeCache treeCache;

    protected RuntimeProperties getRuntimeProperties() {
        return runtimeProperties.get();
    }

    @Override
    public abstract void importFromFileSystem(String from);

    protected void protectedActivate(Map<String, ?> configuration) throws Exception {

        // Remove non-String values from the configuration
        HashMap<String, String> dataStoreProperties = new HashMap<String, String>();
        for (Map.Entry<String, ?> entry : configuration.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String) {
                dataStoreProperties.put(key, (String) value);
            }
        }
        this.dataStoreProperties = Collections.unmodifiableMap(dataStoreProperties);

        // DataStore activation accesses public API that is protected by {@link AbstractComponent#assertValid()).
        // We activate the component first and rollback on error
        try {
            activateComponent();
            activateInternal();
        } catch (Exception ex) {
            deactivateComponent();
            throw ex;
        }
    }

    protected void protectedDeactivate() {
        deactivateComponent();
        deactivateInternal();
    }

    protected void activateInternal() throws Exception {
        LOG.info("Starting up DataStore " + this);
        treeCache = new TreeCache(getCurator(), ZkPath.CONFIGS.getPath(), true, false, true, cacheExecutor);
        treeCache.start(TreeCache.StartMode.NORMAL);
        treeCache.getListenable().addListener(this);

        // Call the bootstrap {@link DataStoreTemplate}
        DataStoreRegistrationHandler templateRegistry = registrationHandler.get();
        DataStoreTemplate template = templateRegistry.removeRegistrationCallback();
        if (template != null) {
            LOG.info("Using template: " + template);
            template.doWith(this);
        }
    }

    protected void deactivateInternal() {
        treeCache.getListenable().removeListener(this);
        Closeables.closeQuitely(treeCache);
        callbacksExecutor.shutdownNow();
        cacheExecutor.shutdownNow();
    }

    protected TreeCache getTreeCache() {
        assertValid();
        return treeCache;
    }

    @Override
    public Map<String, String> getDataStoreProperties() {
        assertValid();
        return dataStoreProperties;
    }

    @Override
    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
        if (isValid()) {
            switch (event.getType()) {
                case CHILD_ADDED:
                case CHILD_REMOVED:
                case CHILD_UPDATED:
                case INITIALIZED:
                    if (shouldRunCallbacks(event.getData().getPath())) {
                        fireChangeNotifications();
                    }
                    break;
            }
        }
    }

    /**
     * Allow derived classes to cache stuff
     */
    protected void clearCaches() {
    }

    protected void fireChangeNotifications() {
        assertValid();
        LOG.debug("Firing change notifications!");
        clearCaches();
        runCallbacks();
    }


    /**
     * Checks if the container should react to a change in the specified path.
     * @param path  The path that has been updated.
     * @return
     */
    private boolean shouldRunCallbacks(String path) {
        String karafName = runtimeProperties.get().getProperty(SystemProperties.KARAF_NAME);
        String currentVersion = getContainerVersion(karafName);
        return path.equals(ZkPath.CONFIG_ENSEMBLES.getPath()) ||
            path.equals(ZkPath.CONFIG_ENSEMBLE_URL.getPath()) ||
            path.equals(ZkPath.CONFIG_ENSEMBLE_PASSWORD.getPath()) ||
            path.equals(ZkPath.CONFIG_CONTAINER.getPath(karafName)) ||
                (currentVersion != null && path.equals(ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(currentVersion, karafName)));

    }

    protected void runCallbacks() {
        callbacksExecutor.submit(new Runnable() {
            @Override
            public void run() {
                doRunCallbacks();
            }
        });
    }

    protected void doRunCallbacks() {
        assertValid();
        for (Runnable callback : callbacks) {
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Running callback " + callback);
                }
                callback.run();
            } catch (Throwable e) {
                LOG.warn("Caught: " + e, e);
            }
        }
    }

    @Override
    public void trackConfiguration(Runnable callback) {
        if (isValid()) {
            callbacks.addIfAbsent(callback);
        }
    }

    @Override
    public void untrackConfiguration(Runnable callback) {
        callbacks.remove(callback);
    }

    // Container stuff
    //-------------------------------------------------------------------------

    @Override
    public List<String> getContainers() {
        assertValid();
        try {
            return getChildren(getCurator(), ZkPath.CONFIGS_CONTAINERS.getPath());
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public boolean hasContainer(String containerId) {
        assertValid();
        return getContainers().contains(containerId);
    }

    @Override
    public String getContainerParent(String containerId) {
        assertValid();
        try {
            String parentName = getStringData(getCurator(), ZkPath.CONTAINER_PARENT.getPath(containerId));
            return parentName != null ? parentName.trim() : "";
        } catch (KeeperException.NoNodeException e) {
            // Ignore
            return "";
        } catch (Throwable e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void deleteContainer(String containerId) {
        assertValid();
        try {
            if (getCurator() == null) {
                throw new IllegalStateException("Zookeeper service not available");
            }
            //Wipe all config entries that are related to the container for all versions.
            for (String version : getVersions()) {
                deleteSafe(getCurator(), ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, containerId));
            }
            deleteSafe(getCurator(), ZkPath.CONFIG_CONTAINER.getPath(containerId));
            deleteSafe(getCurator(), ZkPath.CONTAINER.getPath(containerId));
            deleteSafe(getCurator(), ZkPath.CONTAINER_DOMAINS.getPath(containerId));
            deleteSafe(getCurator(), ZkPath.CONTAINER_PROVISION.getPath(containerId));
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void createContainerConfig(CreateContainerOptions options) {
        assertValid();
        try {
            String parent = options.getParent();
            String containerId = options.getName();
            String versionId = options.getVersion();
            Set<String> profileIds = options.getProfiles();
            StringBuilder sb = new StringBuilder();
            for (String profileId : profileIds) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(profileId);
            }

            setData(getCurator(), ZkPath.CONFIG_CONTAINER.getPath(containerId), versionId);
            setData(getCurator(), ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(versionId, containerId), sb.toString());
            setData(getCurator(), ZkPath.CONTAINER_PARENT.getPath(containerId), parent);
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void createContainerConfig(CreateContainerMetadata metadata) {
        assertValid();
        try {
            CreateContainerOptions options = metadata.getCreateOptions();
            String containerId = metadata.getContainerName();
            //            String parent = options.getParent();
            //            String versionId = options.getVersion() != null ? options.getVersion() : getDefaultVersion();
            //            Set<String> profileIds = options.getProfiles();
            //            if (profileIds == null || profileIds.isEmpty()) {
            //                profileIds = new LinkedHashSet<String>();
            //                profileIds.add("default");
            //            }
            //            StringBuilder sb = new StringBuilder();
            //            for (String profileId : profileIds) {
            //                if (sb.length() > 0) {
            //                    sb.append(" ");
            //                }
            //                sb.append(profileId);
            //            }
            //
            //            setData(getCurator(), ZkPath.CONFIG_CONTAINER.getPath(containerId), versionId);
            //            setData(getCurator(), ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(versionId, containerId), sb.toString());
            //            setData(getCurator(), ZkPath.CONTAINER_PARENT.getPath(containerId), parent);

            setContainerMetadata(metadata);

            Map<String, String> configuration = metadata.getContainerConfiguration();
            for (Map.Entry<String, String> entry : configuration.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                setData(getCurator(), ZkPath.CONTAINER_ENTRY.getPath(metadata.getContainerName(), key), value);
            }

            // If no resolver specified but a resolver is already present in the registry, use the registry value
            String resolver = metadata.getOverridenResolver() != null ? metadata.getOverridenResolver() : options.getResolver();

            if (resolver == null && exists(getCurator(), ZkPath.CONTAINER_RESOLVER.getPath(containerId)) != null) {
                resolver = getStringData(getCurator(), ZkPath.CONTAINER_RESOLVER.getPath(containerId));
            } else if (options.getResolver() != null) {
                // Use the resolver specified in the options and do nothing.
            } else if (exists(getCurator(), ZkPath.POLICIES.getPath(ZkDefs.RESOLVER)) != null) {
                // If there is a globlal resolver specified use it.
                resolver = getStringData(getCurator(), ZkPath.POLICIES.getPath(ZkDefs.RESOLVER));
            } else {
                // Fallback to the default resolver
                resolver = ZkDefs.DEFAULT_RESOLVER;
            }
            // Set the resolver if not already set
            setData(getCurator(), ZkPath.CONTAINER_RESOLVER.getPath(containerId), resolver);
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public CreateContainerMetadata getContainerMetadata(String containerId, final ClassLoader classLoader) {
        assertValid();
        try {
            byte[] encoded = getByteData(getTreeCache(), ZkPath.CONTAINER_METADATA.getPath(containerId));
            if (encoded == null) {
                return null;
            }
            byte[] decoded = Base64Encoder.decode(encoded);
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(decoded)) {
                @Override
                protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                    return classLoader.loadClass(desc.getName());
                }
            };
            return (CreateContainerMetadata) ois.readObject();
        } catch (ClassNotFoundException e) {
            return null;
        } catch (InvalidClassException e) {
            return null;
        } catch (KeeperException.NoNodeException e) {
            return null;
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void setContainerMetadata(CreateContainerMetadata metadata) {
        assertValid();
        //We encode the metadata so that they are more friendly to import/export.
        try {
            setData(getCurator(), ZkPath.CONTAINER_METADATA.getPath(metadata.getContainerName()), Base64Encoder.encode(ObjectUtils.toBytes(metadata)));
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public String getContainerVersion(String containerId) {
        assertValid();
        try {
            return getStringData(getTreeCache(), ZkPath.CONFIG_CONTAINER.getPath(containerId));
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void setContainerVersion(String containerId, String versionId) {
        assertValid();
        try {
            String oldVersionId = getStringData(getCurator(), ZkPath.CONFIG_CONTAINER.getPath(containerId));
            String oldProfileIds = getStringData(getCurator(), ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(oldVersionId, containerId));

            setData(getCurator(), ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(versionId, containerId), oldProfileIds);
            setData(getCurator(), ZkPath.CONFIG_CONTAINER.getPath(containerId), versionId);
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public List<String> getContainerProfiles(String containerId) {
        assertValid();
        try {
            String str = null;
            if (Strings.isNotBlank(containerId)) {
                String versionId = getStringData(getTreeCache(), ZkPath.CONFIG_CONTAINER.getPath(containerId));
                if (Strings.isNotBlank(versionId)) {
                    str = getStringData(getTreeCache(), ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(versionId, containerId));
                }
            }
            return str == null || str.isEmpty() ? Collections.<String> emptyList() : Arrays.asList(str.trim().split(" +"));
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void setContainerProfiles(String containerId, List<String> profileIds) {
        assertValid();
        try {
            String versionId = getStringData(getCurator(), ZkPath.CONFIG_CONTAINER.getPath(containerId));
            StringBuilder sb = new StringBuilder();
            for (String profileId : profileIds) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(profileId);
            }
            setData(getCurator(), ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(versionId, containerId), sb.toString());
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public boolean isContainerAlive(String id) {
        assertValid();
        try {
            return exists(getCurator(), ZkPath.CONTAINER_ALIVE.getPath(id)) != null;
        } catch (KeeperException.NoNodeException e) {
            return false;
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public String getContainerAttribute(String containerId, ContainerAttribute attribute, String def, boolean mandatory, boolean substituted) {
        assertValid();
        if (attribute == ContainerAttribute.Domains) {
            try {
                List<String> list = getCurator().getChildren().forPath(ZkPath.CONTAINER_DOMAINS.getPath(containerId));
                Collections.sort(list);
                StringBuilder sb = new StringBuilder();
                for (String l : list) {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(l);
                }
                return sb.toString();
            } catch (Exception e) {
                return "";
            }
        } else {
            try {
                if (substituted) {
                    return getSubstitutedPath(getCurator(), getAttributePath(containerId, attribute));
                } else {
                    return getStringData(getCurator(), getAttributePath(containerId, attribute));
                }
            } catch (KeeperException.NoNodeException e) {
                if (mandatory) {
                    throw FabricException.launderThrowable(e);
                }
                return def;
            } catch (Exception e) {
                throw FabricException.launderThrowable(e);
            }
        }
    }

    @Override
    public void setContainerAttribute(String containerId, ContainerAttribute attribute, String value) {
        assertValid();
        // Special case for resolver
        // TODO: we could use a double indirection on the ip so that it does not need to change
        // TODO: something like ${zk:container/${zk:container/resolver}}
        if (attribute == ContainerAttribute.Resolver) {
            try {
                setData(getCurator(), ZkPath.CONTAINER_IP.getPath(containerId), "${zk:" + containerId + "/" + value + "}");
                setData(getCurator(), ZkPath.CONTAINER_RESOLVER.getPath(containerId), value);
            } catch (Exception e) {
                throw FabricException.launderThrowable(e);
            }
        } else {
            try {
                //                if (value == null) {
                //                    deleteSafe(zk, getAttributePath(containerId, attribute));
                //                } else {
                setData(getCurator(), getAttributePath(containerId, attribute), value);
                //                }
            } catch (KeeperException.NoNodeException e) {
                // Ignore
            } catch (Exception e) {
                throw FabricException.launderThrowable(e);
            }
        }
    }

    @Override
    public String getDefaultVersion() {
        assertValid();
        try {
            String version = null;
            if (getTreeCache().getCurrentData(ZkPath.CONFIG_DEFAULT_VERSION.getPath()) != null) {
                version = getStringData(getTreeCache(), ZkPath.CONFIG_DEFAULT_VERSION.getPath());
            }
            if (version == null || version.isEmpty()) {
                version = ZkDefs.DEFAULT_VERSION;
                setData(getCurator(), ZkPath.CONFIG_DEFAULT_VERSION.getPath(), version);
                setData(getCurator(), ZkPath.CONFIG_VERSION.getPath(version), (String) null);
            }
            return version;
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void setDefaultVersion(String versionId) {
        assertValid();
        try {
            setData(getCurator(), ZkPath.CONFIG_DEFAULT_VERSION.getPath(), versionId);
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    // Profile methods
    //-------------------------------------------------------------------------

    @Override
    public boolean hasProfile(String version, String profile) {
        assertValid();
        return getProfile(version, profile, false) != null;
    }

    // Implementation
    //-------------------------------------------------------------------------

    private String getAttributePath(String containerId, ContainerAttribute attribute) {
        switch (attribute) {
        case BlueprintStatus:
            return ZkPath.CONTAINER_EXTENDER_STATUS.getPath(containerId, "blueprint");
        case SpringStatus:
            return ZkPath.CONTAINER_EXTENDER_STATUS.getPath(containerId, "spring");
        case ProvisionStatus:
            return ZkPath.CONTAINER_PROVISION_RESULT.getPath(containerId);
        case ProvisionException:
            return ZkPath.CONTAINER_PROVISION_EXCEPTION.getPath(containerId);
        case ProvisionList:
            return ZkPath.CONTAINER_PROVISION_LIST.getPath(containerId);
        case ProvisionChecksums:
            return ZkPath.CONTAINER_PROVISION_CHECKSUMS.getPath(containerId);
        case Location:
            return ZkPath.CONTAINER_LOCATION.getPath(containerId);
        case GeoLocation:
            return ZkPath.CONTAINER_GEOLOCATION.getPath(containerId);
        case Resolver:
            return ZkPath.CONTAINER_RESOLVER.getPath(containerId);
        case Ip:
            return ZkPath.CONTAINER_IP.getPath(containerId);
        case LocalIp:
            return ZkPath.CONTAINER_LOCAL_IP.getPath(containerId);
        case LocalHostName:
            return ZkPath.CONTAINER_LOCAL_HOSTNAME.getPath(containerId);
        case PublicIp:
            return ZkPath.CONTAINER_PUBLIC_IP.getPath(containerId);
        case PublicHostName:
            return ZkPath.CONTAINER_PUBLIC_HOSTNAME.getPath(containerId);
        case ManualIp:
            return ZkPath.CONTAINER_MANUAL_IP.getPath(containerId);
        case BindAddress:
            return ZkPath.CONTAINER_BINDADDRESS.getPath(containerId);
        case JmxUrl:
            return ZkPath.CONTAINER_JMX.getPath(containerId);
        case JolokiaUrl:
            return ZkPath.CONTAINER_JOLOKIA.getPath(containerId);
        case HttpUrl:
            return ZkPath.CONTAINER_HTTP.getPath(containerId);
        case SshUrl:
            return ZkPath.CONTAINER_SSH.getPath(containerId);
        case PortMin:
            return ZkPath.CONTAINER_PORT_MIN.getPath(containerId);
        case PortMax:
            return ZkPath.CONTAINER_PORT_MAX.getPath(containerId);
        case ProcessId:
            return ZkPath.CONTAINER_PROCESS_ID.getPath(containerId);
        case OpenShift:
            return ZkPath.CONTAINER_OPENSHIFT.getPath(containerId);
        default:
            throw new IllegalArgumentException("Unsupported container attribute " + attribute);
        }
    }

    @Override
    public Map<String, String> getProfileAttributes(String version, String profile) {
        assertValid();
        Map<String, String> attributes = new HashMap<String, String>();
        Map<String, String> config = getConfiguration(version, profile, Constants.AGENT_PID);
        for (Map.Entry<String, String> entry : config.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(ATTRIBUTE_PREFIX)) {
                String attribute = key.substring(ATTRIBUTE_PREFIX.length());
                String value = entry.getValue();
                attributes.put(attribute, value);
            }
        }
        return attributes;
    }

    @Override
    public void setProfileAttribute(final String version, final String profile, final String key, final String value) {
        assertValid();
        Map<String, String> config = getConfiguration(version, profile, Constants.AGENT_PID);
        if (value != null) {
            config.put(ATTRIBUTE_PREFIX + key, value);
        } else {
            config.remove(key);
        }
        setConfiguration(version, profile, Constants.AGENT_PID, config);
    }

    @Override
    public List<String> getConfigurationFileNames(String version, String profile) {
        assertValid();
        // TODO this is an inefficient implementation; we could optimise away loading all the config files in an implementation
        try {
            Map<String, byte[]> configs = getFileConfigurations(version, profile);
            return new ArrayList<String>(configs.keySet());
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public Map<String, Map<String, String>> getConfigurations(String version, String profile) {
        assertValid();
        try {
            Map<String, Map<String, String>> configurations = new HashMap<String, Map<String, String>>();
            Map<String, byte[]> configs = getFileConfigurations(version, profile);
            for (Map.Entry<String, byte[]> entry : configs.entrySet()) {
                if (entry.getKey().endsWith(".properties")) {
                    String pid = DataStoreUtils.stripSuffix(entry.getKey(), ".properties");
                    configurations.put(pid, DataStoreUtils.toMap(DataStoreUtils.toProperties(entry.getValue())));
                }
            }
            return configurations;
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    protected CuratorFramework getCurator() {
        return curator.get();
    }

    @VisibleForTesting
    public void bindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.bind(service);
    }

    protected void unbindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.unbind(service);
    }

    @VisibleForTesting
    public void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    protected void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }

    @VisibleForTesting
    public void bindRegistrationHandler(DataStoreRegistrationHandler service) {
        this.registrationHandler.bind(service);
    }

    protected void unbindRegistrationHandler(DataStoreRegistrationHandler service) {
        this.registrationHandler.unbind(service);
    }
}
