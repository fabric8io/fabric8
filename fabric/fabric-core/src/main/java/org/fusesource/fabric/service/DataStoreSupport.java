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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.zookeeper.KeeperException;
import org.fusesource.fabric.api.CreateContainerMetadata;
import org.fusesource.fabric.api.CreateContainerOptions;
import org.fusesource.fabric.api.DataStore;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.PlaceholderResolver;
import org.fusesource.fabric.internal.DataStoreHelpers;
import org.fusesource.fabric.internal.Objects;
import org.fusesource.fabric.utils.Base64Encoder;
import org.fusesource.fabric.utils.Closeables;
import org.fusesource.fabric.utils.ObjectUtils;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.utils.InterpolationHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fusesource.fabric.internal.DataStoreHelpers.substituteBundleProperty;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.deleteSafe;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.exists;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getByteData;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getChildren;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getStringData;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getSubstitutedPath;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setData;

/**
 */
public abstract class DataStoreSupport implements DataStore, PathChildrenCacheListener {
    private static final transient Logger LOG = LoggerFactory.getLogger(DataStoreSupport.class);

    public static final String REQUIREMENTS_JSON_PATH
            = "/fabric/configs/org.fusesource.fabric.requirements.json";
    public static final String JVM_OPTIONS_PATH
            = "/fabric/configs/org.fusesource.fabric.containers.jvmOptions";

    private Map<String, PlaceholderResolver> placeholderResolvers = new HashMap<String, PlaceholderResolver>();

    private final List<Runnable> callbacks = new CopyOnWriteArrayList<Runnable>();

    //We are using an external ExecutorService to prevent IllegalThreadStateExceptions when the cache is starting.
    private ExecutorService cacheExecutor;

    protected TreeCache treeCache;
    private AtomicBoolean started = new AtomicBoolean(false);

    private CuratorFramework curator;
    private Map<String, String> dataStoreProperties;

    @Override
    public abstract void importFromFileSystem(String from);

    public synchronized void start() {
        try {
        if (started.compareAndSet(false, true)) {
            LOG.info("Starting up DataStore " + this);
            Objects.notNull(getCurator(), "curator");
            createCache(getCurator());
        }
        }catch (Exception ex) {
            throw new FabricException("Failed to start data store.", ex);
        }
    }

    public synchronized void stop() {
        destroyCache();
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }

    public Map<String, String> getDataStoreProperties() {
        return dataStoreProperties;
    }

    /**
     * Sets the configuration properties for this DataStore implementation
     */
    public void setDataStoreProperties(Map<String, String> dataStoreProperties) {
        this.dataStoreProperties = dataStoreProperties;
    }

    public Map<String, PlaceholderResolver> getPlaceholderResolvers() {
        return placeholderResolvers;
    }

    public void setPlaceholderResolvers(Map<String, PlaceholderResolver> placeholderResolvers) {
        this.placeholderResolvers = placeholderResolvers;
    }


    public void bindCurator(CuratorFramework curator) {
        this.curator = curator;
    }

    public void unbindCurator(CuratorFramework curator) {
        this.curator = null;
    }

    public synchronized void bindPlaceholderResolver(PlaceholderResolver resolver) {
        if (resolver != null) {
            getPlaceholderResolvers().put(resolver.getScheme(), resolver);
        }
    }

    public synchronized void unbindPlaceholderResolver(PlaceholderResolver resolver) {
        if (resolver != null) {
            getPlaceholderResolvers().remove(resolver.getScheme());
        }
    }

    private synchronized void createCache(CuratorFramework curator) throws Exception {
        cacheExecutor = Executors.newSingleThreadExecutor();
        treeCache = new TreeCache(curator, ZkPath.CONFIGS.getPath(), true, false, true, cacheExecutor);
        treeCache.start(TreeCache.StartMode.NORMAL);
        treeCache.getListenable().addListener(this);
    }

    private synchronized void destroyCache() {
        if (treeCache != null) {
            treeCache.getListenable().removeListener(this);
            Closeables.closeQuitely(treeCache);
            treeCache = null;
            cacheExecutor.shutdownNow();
        }
    }

    @Override
    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
        switch (event.getType()) {
        case CHILD_ADDED:
        case CHILD_REMOVED:
        case CHILD_UPDATED:
        case INITIALIZED:
            runCallbacks();
            break;
        }
    }

    public void trackConfiguration(Runnable callback) {
        callbacks.add(callback);
    }

    @Override
    public void unTrackConfiguration(Runnable callback) {
        callbacks.remove(callback);
    }


    protected void runCallbacks() {
        for (Runnable callback : callbacks) {
            try {
                callback.run();
            } catch (Throwable t) {
                //ignore
            }
        }
    }

    // PlaceholderResolver stuff
    //-------------------------------------------------------------------------

    public BundleContext getBundleContext() {
        try {
            return FrameworkUtil.getBundle(DataStoreSupport.class).getBundleContext();
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Performs substitution to configuration based on the registered {@link org.fusesource.fabric.api.PlaceholderResolver} instances.
     *
     * @param configs
     */
    public synchronized void substituteConfigurations(final Map<String, Map<String, String>> configs) {
        final Map<String, PlaceholderResolver> placeholderResolvers
                = getPlaceholderResolvers();

        for (Map.Entry<String, Map<String, String>> entry : configs.entrySet()) {
            final String pid = entry.getKey();
            Map<String, String> props = entry.getValue();

            for (Map.Entry<String, String> e : props.entrySet()) {
                final String key = e.getKey();
                final String value = e.getValue();
                props.put(key, InterpolationHelper
                        .substVars(value, key, null, props, new InterpolationHelper.SubstitutionCallback() {
                            public String getValue(String toSubstitute) {
                                if (toSubstitute != null && toSubstitute.contains(":")) {
                                    String scheme = toSubstitute.substring(0, toSubstitute.indexOf(":"));
                                    if (placeholderResolvers.containsKey(scheme)) {
                                        return placeholderResolvers.get(scheme)
                                                .resolve(pid, key, toSubstitute);
                                    }
                                }
                                return substituteBundleProperty(toSubstitute, getBundleContext());
                            }
                        }));
            }
        }
    }

    // Container stuff
    //-------------------------------------------------------------------------

    @Override
    public List<String> getContainers() {
        try {
            return getChildren(getCurator(), ZkPath.CONFIGS_CONTAINERS.getPath());
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public boolean hasContainer(String containerId) {
        return getContainers().contains(containerId);
    }

    @Override
    public String getContainerParent(String containerId) {
        try {
            String parentName = getStringData(getCurator(), ZkPath.CONTAINER_PARENT.getPath(containerId));
            return parentName != null ? parentName.trim() : "";
        } catch (KeeperException.NoNodeException e) {
            // Ignore
            return "";
        } catch (Throwable e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void deleteContainer(String containerId) {
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
            throw new FabricException(e);
        }
    }

    @Override
    public void createContainerConfig(CreateContainerMetadata metadata) {
        try {
            CreateContainerOptions options = metadata.getCreateOptions();
            String containerId = metadata.getContainerName();
            String parent = options.getParent();
            String versionId = options.getVersion() != null ? options.getVersion() : getDefaultVersion();
            Set<String> profileIds = options.getProfiles();
            if (profileIds == null || profileIds.isEmpty()) {
                profileIds = new LinkedHashSet<String>();
                profileIds.add("default");
            }
            StringBuilder sb = new StringBuilder();
            for (String profileId : profileIds) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(profileId);
            }

            setData(getCurator(), ZkPath.CONFIG_CONTAINER.getPath(containerId), versionId);
            setData(getCurator(), ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(versionId, containerId),
                    sb.toString());
            setData(getCurator(), ZkPath.CONTAINER_PARENT.getPath(containerId), parent);

            setContainerMetadata(metadata);

            Map<String, String> configuration = metadata.getContainerConfiguration();
            for (Map.Entry<String, String> entry : configuration.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                setData(getCurator(), ZkPath.CONTAINER_ENTRY.getPath(metadata.getContainerName(), key),
                        value);
            }

            // If no resolver specified but a resolver is already present in the registry, use the registry value
            String resolver = metadata.getOverridenResolver() != null ? metadata.getOverridenResolver()
                    : options.getResolver();

            if (resolver == null
                    && exists(getCurator(), ZkPath.CONTAINER_RESOLVER.getPath(containerId)) != null) {
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
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public CreateContainerMetadata getContainerMetadata(String containerId, final ClassLoader classLoader) {
        try {
            byte[] encoded = getByteData(treeCache, ZkPath.CONTAINER_METADATA.getPath(containerId));
            if (encoded == null) {
                return null;
            }
            byte[] decoded = Base64Encoder.decode(encoded);
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(decoded)) {
                @Override
                protected Class<?> resolveClass(ObjectStreamClass desc)
                        throws IOException, ClassNotFoundException {
                    return classLoader.loadClass(desc.getName());
                }
            };
            return (CreateContainerMetadata)ois.readObject();
        } catch (ClassNotFoundException e) {
            return null;
        } catch (InvalidClassException e) {
            return null;
        } catch (KeeperException.NoNodeException e) {
            return null;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setContainerMetadata(CreateContainerMetadata metadata) {
        //We encode the metadata so that they are more friendly to import/export.
        try {
            setData(getCurator(), ZkPath.CONTAINER_METADATA.getPath(metadata.getContainerName()),
                    Base64Encoder.encode(
                            ObjectUtils.toBytes(metadata)));
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public String getContainerVersion(String containerId) {
        try {
            return getStringData(treeCache, ZkPath.CONFIG_CONTAINER.getPath(containerId));
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setContainerVersion(String containerId, String versionId) {
        try {
            String oldVersionId = getStringData(getCurator(), ZkPath.CONFIG_CONTAINER.getPath(containerId));
            String oldProfileIds = getStringData(getCurator(),
                    ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(oldVersionId, containerId));

            setData(getCurator(), ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(versionId, containerId),
                    oldProfileIds);
            setData(getCurator(), ZkPath.CONFIG_CONTAINER.getPath(containerId), versionId);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public List<String> getContainerProfiles(String containerId) {
        try {
            String versionId = getStringData(treeCache, ZkPath.CONFIG_CONTAINER.getPath(containerId));
            String str = getStringData(treeCache,
                    ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(versionId, containerId));
            return str == null || str.isEmpty() ? Collections.<String>emptyList() : Arrays
                    .asList(str.trim().split(" +"));
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setContainerProfiles(String containerId, List<String> profileIds) {
        try {
            String versionId = getStringData(getCurator(), ZkPath.CONFIG_CONTAINER.getPath(containerId));
            StringBuilder sb = new StringBuilder();
            for (String profileId : profileIds) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(profileId);
            }
            setData(getCurator(), ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(versionId, containerId),
                    sb.toString());
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public boolean isContainerAlive(String id) {
        try {
            return exists(getCurator(), ZkPath.CONTAINER_ALIVE.getPath(id)) != null;
        } catch (KeeperException.NoNodeException e) {
            return false;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public String getContainerAttribute(String containerId, ContainerAttribute attribute, String def,
                                        boolean mandatory, boolean substituted) {
        if (attribute == ContainerAttribute.Domains) {
            try {
                List<String> list = getCurator().getChildren()
                        .forPath(ZkPath.CONTAINER_DOMAINS.getPath(containerId));
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
                    throw new FabricException(e);
                }
                return def;
            } catch (Exception e) {
                throw new FabricException(e);
            }
        }
    }

    @Override
    public void setContainerAttribute(String containerId, ContainerAttribute attribute, String value) {
        // Special case for resolver
        // TODO: we could use a double indirection on the ip so that it does not need to change
        // TODO: something like ${zk:container/${zk:container/resolver}}
        if (attribute == ContainerAttribute.Resolver) {
            try {
                setData(getCurator(), ZkPath.CONTAINER_IP.getPath(containerId),
                        "${zk:" + containerId + "/" + value + "}");
                setData(getCurator(), ZkPath.CONTAINER_RESOLVER.getPath(containerId), value);
            } catch (Exception e) {
                throw new FabricException(e);
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
                throw new FabricException(e);
            }
        }
    }

    @Override
    public String getDefaultVersion() {
        try {
            String version = null;
            if (treeCache.getCurrentData(ZkPath.CONFIG_DEFAULT_VERSION.getPath()) != null) {
                version = getStringData(treeCache, ZkPath.CONFIG_DEFAULT_VERSION.getPath());
            }
            if (version == null || version.isEmpty()) {
                version = ZkDefs.DEFAULT_VERSION;
                setData(getCurator(), ZkPath.CONFIG_DEFAULT_VERSION.getPath(), version);
                setData(getCurator(), ZkPath.CONFIG_VERSION.getPath(version), (String)null);
            }
            return version;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setDefaultVersion(String versionId) {
        try {
            setData(getCurator(), ZkPath.CONFIG_DEFAULT_VERSION.getPath(), versionId);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }


    // Profile methods
    //-------------------------------------------------------------------------

    @Override
    public boolean hasProfile(String version, String profile) {
        return getProfile(version, profile, false) != null;
    }


    // Implementation
    //-------------------------------------------------------------------------


    private String getAttributePath(String containerId, ContainerAttribute attribute) {
        switch (attribute) {
        case ProvisionStatus:
            return ZkPath.CONTAINER_PROVISION_RESULT.getPath(containerId);
        case ProvisionException:
            return ZkPath.CONTAINER_PROVISION_EXCEPTION.getPath(containerId);
        case ProvisionList:
            return ZkPath.CONTAINER_PROVISION_LIST.getPath(containerId);
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
        default:
            throw new IllegalArgumentException("Unsupported container attribute " + attribute);
        }
    }

    @Override
    public Map<String, Map<String, String>> getConfigurations(String version, String profile) {
        try {
            Map<String, Map<String, String>> configurations = new HashMap<String, Map<String, String>>();
            Map<String, byte[]> configs = getFileConfigurations(version, profile);
            for (Map.Entry<String, byte[]> entry : configs.entrySet()) {
                if (entry.getKey().endsWith(".properties")) {
                    String pid = DataStoreHelpers.stripSuffix(entry.getKey(), ".properties");
                    configurations.put(pid,
                            DataStoreHelpers.toMap(DataStoreHelpers.toProperties(entry.getValue())));
                }
            }
            return configurations;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }
}
