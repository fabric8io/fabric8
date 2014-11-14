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
package io.fabric8.service;

import static io.fabric8.zookeeper.ZkPath.CONTAINER_DOMAIN;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.deleteSafe;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.exists;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getByteData;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getChildrenSafe;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getStringData;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getSubstitutedPath;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;
import io.fabric8.api.AutoScaleStatus;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.CreateContainerOptions;
import io.fabric8.api.DataStore;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricRequirements;
import io.fabric8.api.FabricService;
import io.fabric8.api.ProfileService;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.ZkDefs;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.common.util.Closeables;
import io.fabric8.common.util.ObjectUtils;
import io.fabric8.common.util.Strings;
import io.fabric8.internal.RequirementsJson;
import io.fabric8.utils.Base64Encoder;
import io.fabric8.utils.FabricVersionUtils;
import io.fabric8.zookeeper.ZkPath;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.zookeeper.KeeperException;
import io.fabric8.api.gravia.IllegalArgumentAssertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Zookeeper based implementation of {@link DataStore}.
 */
@ThreadSafe
@Component(label = "Fabric8 DataStore", policy = ConfigurationPolicy.IGNORE, immediate = true, metatype = true)
@Service({ DataStore.class })
public final class ZkDataStoreImpl extends AbstractComponent implements DataStore, PathChildrenCacheListener {
    
    private static final transient Logger LOGGER = LoggerFactory.getLogger(ZkDataStoreImpl.class);
    
    private static final String JVM_OPTIONS_PATH = "/fabric/configs/io.fabric8.containers.jvmOptions";
    private static final String REQUIREMENTS_JSON_PATH = "/fabric/configs/io.fabric8.requirements.json";
    
    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();
    @Reference(referenceInterface = RuntimeProperties.class)
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<RuntimeProperties>();
    
    private final CopyOnWriteArrayList<Runnable> callbacks = new CopyOnWriteArrayList<Runnable>();
    private final ExecutorService cacheExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService callbacksExecutor = Executors.newSingleThreadExecutor();
    private TreeCache configCache;
    private TreeCache containerCache;

    @Activate
    void activate() throws Exception {
        activateInternal();
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        deactivateInternal();
    }
    
    private void activateInternal() throws Exception {
        configCache = new TreeCache(curator.get(), ZkPath.CONFIGS.getPath(), true, false, true, cacheExecutor);
        configCache.start(TreeCache.StartMode.NORMAL);
        configCache.getListenable().addListener(this);

        containerCache = new TreeCache(curator.get(), ZkPath.CONTAINERS.getPath(), true, false, true, cacheExecutor);
        containerCache.start(TreeCache.StartMode.NORMAL);
        containerCache.getListenable().addListener(this);

    }

    private void deactivateInternal() {
        configCache.getListenable().removeListener(this);
        Closeables.closeQuietly(configCache);

        containerCache.getListenable().removeListener(this);
        Closeables.closeQuietly(containerCache);

        callbacksExecutor.shutdownNow();
        cacheExecutor.shutdownNow();
    }

    @Override
    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
        if (isValid()) {

            // guard against events with null data or path
            String path;
            ChildData childData = event.getData();
            if (childData != null) {
                path = childData.getPath();
            } else {
                path = null;
            }

            byte[] data = null;
            if (childData != null) {
                data = childData.getData();
            }

            PathChildrenCacheEvent.Type type = event.getType();
            switch (type) {
                case CHILD_ADDED:
                case CHILD_REMOVED:
                case CHILD_UPDATED:
                case INITIALIZED:
                    if (shouldRunCallbacks(type, path)) {
                        String s = data != null ? new String(data, "UTF-8") : "";
                        LOGGER.info("Event {} detected on {} with data {}. Sending notification.", type.name(), path, s);
                        fireChangeNotifications();
                    }
                    break;
            }
        }
    }

    /**
     * Checks if the container should react to a change in the specified path.
     */
    private boolean shouldRunCallbacks(PathChildrenCacheEvent.Type type, String path) {
        if (path == null) {
            return false;
        }

        String runtimeIdentity = runtimeProperties.get().getRuntimeIdentity();
        String currentVersion = getContainerVersion(runtimeIdentity);
        return (path.startsWith(ZkPath.CONTAINERS.getPath()) && type.equals(PathChildrenCacheEvent.Type.CHILD_UPDATED)) ||
                        path.equals(ZkPath.CONFIG_ENSEMBLES.getPath()) ||
                        path.equals(ZkPath.CONFIG_ENSEMBLE_URL.getPath()) ||
                        path.equals(ZkPath.CONFIG_ENSEMBLE_PASSWORD.getPath()) ||
                        path.equals(ZkPath.CONFIG_CONTAINER.getPath(runtimeIdentity)) ||
                        (currentVersion != null && path.equals(ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(currentVersion, runtimeIdentity)));
    }
    
    @Override
    public void fireChangeNotifications() {
        runCallbacks();
    }
    
    private void runCallbacks() {
        callbacksExecutor.submit(new Runnable() {
            @Override
            public void run() {
                doRunCallbacks();
            }
        });
    }
    
    private void doRunCallbacks() {
        assertValid();
        for (Runnable callback : callbacks) {
            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Running callback " + callback);
                }
                callback.run();
            } catch (Throwable e) {
                LOGGER.warn("Caught: " + e, e);
            }
        }
    }
    
    @Override
    public String getFabricReleaseVersion() {
        return FabricVersionUtils.getReleaseVersion();
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

    @Override
    public List<String> getContainers() {
        assertValid();
        try {
            return getChildrenSafe(curator.get(), ZkPath.CONFIGS_CONTAINERS.getPath());
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
            String parentName = getStringData(curator.get(), ZkPath.CONTAINER_PARENT.getPath(containerId));
            return parentName != null ? parentName.trim() : "";
        } catch (KeeperException.NoNodeException e) {
            // Ignore
            return "";
        } catch (Throwable e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void deleteContainer(FabricService fabricService, String containerId) {
        assertValid();
        try {
            if (curator.get() == null) {
                throw new IllegalStateException("Zookeeper service not available");
            }
            // Wipe all config entries that are related to the container for all versions.
            ProfileService profileService = fabricService.adapt(ProfileService.class);
            for (String version : profileService.getVersions()) {
                deleteSafe(curator.get(), ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, containerId));
            }
            deleteSafe(curator.get(), ZkPath.CONFIG_CONTAINER.getPath(containerId));
            deleteSafe(curator.get(), ZkPath.CONTAINER.getPath(containerId));
            deleteSafe(curator.get(), ZkPath.CONTAINER_ALIVE.getPath(containerId));
            deleteSafe(curator.get(), ZkPath.CONTAINER_DOMAINS.getPath(containerId));
            deleteSafe(curator.get(), ZkPath.CONTAINER_PROVISION.getPath(containerId));
            deleteSafe(curator.get(), ZkPath.CONTAINER_STATUS.getPath(containerId));
            deleteSafe(curator.get(), ZkPath.AUTHENTICATION_CONTAINER.getPath(containerId));
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

            setData(curator.get(), ZkPath.CONFIG_CONTAINER.getPath(containerId), versionId);
            setData(curator.get(), ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(versionId, containerId), sb.toString());
            setData(curator.get(), ZkPath.CONTAINER_PARENT.getPath(containerId), parent);
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
            //            setData(curator.get(), ZkPath.CONFIG_CONTAINER.getPath(containerId), versionId);
            //            setData(curator.get(), ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(versionId, containerId), sb.toString());
            //            setData(curator.get(), ZkPath.CONTAINER_PARENT.getPath(containerId), parent);

            setContainerMetadata(metadata);

            Map<String, String> configuration = metadata.getContainerConfiguration();
            for (Map.Entry<String, String> entry : configuration.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                setData(curator.get(), ZkPath.CONTAINER_ENTRY.getPath(metadata.getContainerName(), key), value);
            }

            // If no resolver specified but a resolver is already present in the registry, use the registry value
            String resolver = metadata.getOverridenResolver() != null ? metadata.getOverridenResolver() : options.getResolver();

            if (resolver == null && exists(curator.get(), ZkPath.CONTAINER_RESOLVER.getPath(containerId)) != null) {
                resolver = getStringData(curator.get(), ZkPath.CONTAINER_RESOLVER.getPath(containerId));
            } else if (options.getResolver() != null) {
                // Use the resolver specified in the options and do nothing.
            } else if (exists(curator.get(), ZkPath.POLICIES.getPath(ZkDefs.RESOLVER)) != null) {
                // If there is a globlal resolver specified use it.
                resolver = getStringData(curator.get(), ZkPath.POLICIES.getPath(ZkDefs.RESOLVER));
            } else {
                // Fallback to the default resolver
                resolver = ZkDefs.DEFAULT_RESOLVER;
            }
            // Set the resolver if not already set
            setData(curator.get(), ZkPath.CONTAINER_RESOLVER.getPath(containerId), resolver);
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public CreateContainerMetadata getContainerMetadata(String containerId, final ClassLoader classLoader) {
        assertValid();
        try {
            byte[] encoded = getByteData(configCache, ZkPath.CONTAINER_METADATA.getPath(containerId));
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
            setData(curator.get(), ZkPath.CONTAINER_METADATA.getPath(metadata.getContainerName()), Base64Encoder.encode(ObjectUtils.toBytes(metadata)));
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public String getContainerVersion(String containerId) {
        assertValid();
        try {
            return getStringData(configCache, ZkPath.CONFIG_CONTAINER.getPath(containerId));
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void setContainerVersion(String containerId, String versionId) {
        assertValid();
        try {
            String oldVersionId = getStringData(curator.get(), ZkPath.CONFIG_CONTAINER.getPath(containerId));
            String oldProfileIds = getStringData(curator.get(), ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(oldVersionId, containerId));

            setData(curator.get(), ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(versionId, containerId), oldProfileIds);
            setData(curator.get(), ZkPath.CONFIG_CONTAINER.getPath(containerId), versionId);
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
                String versionId = getStringData(configCache, ZkPath.CONFIG_CONTAINER.getPath(containerId));
                if (Strings.isNotBlank(versionId)) {
                    str = getStringData(configCache, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(versionId, containerId));
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
            String versionId = getStringData(curator.get(), ZkPath.CONFIG_CONTAINER.getPath(containerId));
            Set<String> idset = new HashSet<>();
            StringBuilder sb = new StringBuilder();
            for (String profileId : profileIds) {
                IllegalArgumentAssertion.assertFalse(idset.contains(profileId), "Duplicate profile id in: " + profileIds);
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(profileId);
                idset.add(profileId);
            }
            setData(curator.get(), ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(versionId, containerId), sb.toString());
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public boolean isContainerAlive(String id) {
        assertValid();
        try {
            return exists(curator.get(), ZkPath.CONTAINER_ALIVE.getPath(id)) != null;
        } catch (KeeperException.NoNodeException e) {
            return false;
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void setContainerAlive(String id, boolean flag) {
        assertValid();
        try {
            if (flag) {
                setData(curator.get(), ZkPath.CONTAINER_ALIVE.getPath(id), "alive");
            } else {
                deleteSafe(curator.get(), ZkPath.CONTAINER_ALIVE.getPath(id));
            }
        } catch (KeeperException.NoNodeException e) {
            // ignore
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public String getContainerAttribute(String containerId, ContainerAttribute attribute, String def, boolean mandatory, boolean substituted) {
        assertValid();
        if (attribute == ContainerAttribute.Domains) {
            try {
                List<String> list = curator.get().getChildren().forPath(ZkPath.CONTAINER_DOMAINS.getPath(containerId));
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
                    return getSubstitutedPath(curator.get(), getAttributePath(containerId, attribute));
                } else {
                    return getStringData(curator.get(), getAttributePath(containerId, attribute));
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
                setData(curator.get(), ZkPath.CONTAINER_IP.getPath(containerId), "${zk:" + containerId + "/" + value + "}");
                setData(curator.get(), ZkPath.CONTAINER_RESOLVER.getPath(containerId), value);
            } catch (Exception e) {
                throw FabricException.launderThrowable(e);
            }
        } else if (attribute == ContainerAttribute.Domains) {
            try {
                List<String> list = value != null ? Arrays.asList(value.split("\n")) : Collections.<String>emptyList();
                Set<String> zkSet = new HashSet<String>(getChildrenSafe(curator.get(), ZkPath.CONTAINER_DOMAINS.getPath(containerId)));
                for (String domain : list) {
                    String path = CONTAINER_DOMAIN.getPath(containerId, domain);
                    // add any missing domains
                    if (!zkSet.remove(domain)) {
                        setData(curator.get(), path, "");
                    }
                }

                // now lets delete the old ones
                for (String domain : zkSet) {
                    String path = CONTAINER_DOMAIN.getPath(containerId, domain);
                    deleteSafe(curator.get(), path);
                }
            } catch (Exception e) {
                throw FabricException.launderThrowable(e);
            }
        } else {
            try {
                //                if (value == null) {
                //                    deleteSafe(zk, getAttributePath(containerId, attribute));
                //                } else {
                setData(curator.get(), getAttributePath(containerId, attribute), value);
                //                }
            } catch (KeeperException.NoNodeException e) {
                // Ignore
            } catch (Exception e) {
                throw FabricException.launderThrowable(e);
            }
        }
    }

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
        case DebugPort:
            return ZkPath.CONTAINER_DEBUG_PORT.getPath(containerId);
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
    public String getDefaultVersion() {
        assertValid();
        try {
            String version = null;
            if (configCache.getCurrentData(ZkPath.CONFIG_DEFAULT_VERSION.getPath()) != null) {
                version = getStringData(configCache, ZkPath.CONFIG_DEFAULT_VERSION.getPath());
            }
            if (version == null || version.isEmpty()) {
                version = ZkDefs.DEFAULT_VERSION;
                setData(curator.get(), ZkPath.CONFIG_DEFAULT_VERSION.getPath(), version);
                setData(curator.get(), ZkPath.CONFIG_VERSION.getPath(version), (String) null);
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
            setData(curator.get(), ZkPath.CONFIG_DEFAULT_VERSION.getPath(), versionId);
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public String getDefaultJvmOptions() {
        assertValid();
        try {
            CuratorFramework curatorFramework = curator.get();
            if (curatorFramework.getZookeeperClient().isConnected() && exists(curatorFramework, JVM_OPTIONS_PATH) != null) {
                return getStringData(configCache, JVM_OPTIONS_PATH);
            } else {
                return "";
            }
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void setDefaultJvmOptions(String jvmOptions) {
        assertValid();
        try {
            String opts = jvmOptions != null ? jvmOptions : "";
            setData(curator.get(), JVM_OPTIONS_PATH, opts);
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public FabricRequirements getRequirements() {
        assertValid();
        try {
            FabricRequirements answer = null;
            if (configCache.getCurrentData(REQUIREMENTS_JSON_PATH) != null) {
                String json = getStringData(configCache, REQUIREMENTS_JSON_PATH);
                answer = RequirementsJson.fromJSON(json);
            }
            if (answer == null) {
                answer = new FabricRequirements();
            }
            return answer;
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public AutoScaleStatus getAutoScaleStatus() {
        assertValid();
        try {
            AutoScaleStatus answer = null;
            String zkPath = ZkPath.AUTO_SCALE_STATUS.getPath();
            if (configCache.getCurrentData(zkPath) != null) {
                String json = getStringData(configCache, zkPath);
                answer = RequirementsJson.autoScaleStatusFromJSON(json);
            }
            if (answer == null) {
                answer = new AutoScaleStatus();
            }
            return answer;
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void setRequirements(FabricRequirements requirements) throws IOException {
        assertValid();
        try {
            requirements.removeEmptyRequirements();
            String json = RequirementsJson.toJSON(requirements);
            setData(curator.get(), REQUIREMENTS_JSON_PATH, json);
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public String getClusterId() {
        assertValid();
        try {
            return getStringData(curator.get(), ZkPath.CONFIG_ENSEMBLES.getPath());
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public List<String> getEnsembleContainers() {
        assertValid();
        List<String> containers = new ArrayList<String>();
        try {
            String ensemble = getStringData(curator.get(), ZkPath.CONFIG_ENSEMBLE.getPath(getClusterId()));
            if (ensemble != null) {
                for (String name : ensemble.trim().split(",")) {
                    containers.add(name);
                }
            }
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
        return containers;
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
}
