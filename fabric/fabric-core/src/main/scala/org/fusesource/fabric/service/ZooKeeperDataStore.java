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
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeData;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.KeeperException;
import org.fusesource.fabric.api.CreateContainerMetadata;
import org.fusesource.fabric.api.CreateContainerOptions;
import org.fusesource.fabric.api.DataStore;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.FabricRequirements;
import org.fusesource.fabric.internal.DataStoreHelpers;
import org.fusesource.fabric.internal.RequirementsJson;
import org.fusesource.fabric.utils.Base64Encoder;
import org.fusesource.fabric.utils.Closeables;
import org.fusesource.fabric.utils.ObjectUtils;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.ZkProfiles;
import org.fusesource.fabric.zookeeper.utils.ZookeeperImportUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.copy;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.create;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.createDefault;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.deleteSafe;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.exists;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getAllChildren;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getByteData;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getChildren;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getProperties;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getPropertiesAsMap;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getStringData;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getSubstitutedPath;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.lastModified;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setData;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setProperties;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setPropertiesAsMap;

/**
 * @author Stan Lewis
 */
public class ZooKeeperDataStore extends SubstitutionSupport implements DataStore, PathChildrenCacheListener {

    public static final String REQUIREMENTS_JSON_PATH = "/fabric/configs/org.fusesource.fabric.requirements.json";
    public static final String JVM_OPTIONS_PATH = "/fabric/configs/org.fusesource.fabric.containers.jvmOptions";

    private CuratorFramework curator;
    private final List<Runnable> callbacks = new CopyOnWriteArrayList<Runnable>();
    private TreeCache treeCache;


    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    public void destroy() {
        destroyCache();
    }

    @Override
    public void importFromFileSystem(String from) {
        try {
            ZookeeperImportUtils.importFromFileSystem(curator, from, "/", null, null, false, false, false);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    private void createCache(CuratorFramework curator) throws Exception {
        destroyCache();
        treeCache = new TreeCache(curator, ZkPath.CONFIGS.getPath(), true, true);
        treeCache.start(TreeCache.StartMode.NORMAL);
        treeCache.getListenable().addListener(this);
    }

    private void destroyCache() {
        if (treeCache != null) {
            treeCache.getListenable().removeListener(this);
            Closeables.closeQuitely(treeCache);
            treeCache = null;
        }
    }

    public void bind(CuratorFramework curator) throws Exception {
        destroyCache();
        if (curator != null) {
            String connectionString = curator.getZookeeperClient().getCurrentConnectionString();
            if (connectionString != null && !connectionString.isEmpty()) {
                createCache(curator);
            }
        }
    }

    public void unbind(CuratorFramework curator) throws IOException {
        destroyCache();
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

    private void runCallbacks() {
        for (Runnable callback : callbacks) {
            try {
                callback.run();
            } catch (Throwable t) {
                //ignore
            }
        }
    }

    public void trackConfiguration(Runnable callback) {
        callbacks.add(callback);
    }

    @Override
    public void unTrackConfiguration(Runnable callback) {
        callbacks.remove(callback);
    }

    @Override
    public List<String> getContainers() {
        try {
            return getChildren(curator, ZkPath.CONFIGS_CONTAINERS.getPath());
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
            String parentName = getStringData(curator, ZkPath.CONTAINER_PARENT.getPath(containerId));
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
            if (curator == null) {
                throw new IllegalStateException("Zookeeper service not available");
            }
            //Wipe all config entries that are related to the container for all versions.
            for (String version : getVersions()) {
                deleteSafe(curator, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(version, containerId));
            }
            deleteSafe(curator, ZkPath.CONFIG_CONTAINER.getPath(containerId));
            deleteSafe(curator, ZkPath.CONTAINER.getPath(containerId));
            deleteSafe(curator, ZkPath.CONTAINER_DOMAINS.getPath(containerId));
            deleteSafe(curator, ZkPath.CONTAINER_PROVISION.getPath(containerId));
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

            setData(curator, ZkPath.CONFIG_CONTAINER.getPath(containerId), versionId);
            setData(curator, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(versionId, containerId), sb.toString());
            setData(curator, ZkPath.CONTAINER_PARENT.getPath(containerId), parent);

            setContainerMetadata(metadata);

            Map<String, String> configuration = metadata.getContainerConfiguration();
            for (Map.Entry<String, String> entry : configuration.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                setData(curator, ZkPath.CONTAINER_ENTRY.getPath(metadata.getContainerName(), key), value);
            }

            // If no resolver specified but a resolver is already present in the registry, use the registry value
            String resolver = metadata.getOverridenResolver() != null ? metadata.getOverridenResolver() : options.getResolver();

            if (resolver == null && exists(curator, ZkPath.CONTAINER_RESOLVER.getPath(containerId)) != null) {
                resolver = getStringData(curator, ZkPath.CONTAINER_RESOLVER.getPath(containerId));
            } else if (options.getResolver() != null) {
                // Use the resolver specified in the options and do nothing.
            } else if (exists(curator, ZkPath.POLICIES.getPath(ZkDefs.RESOLVER)) != null) {
                // If there is a globlal resolver specified use it.
                resolver = getStringData(curator, ZkPath.POLICIES.getPath(ZkDefs.RESOLVER));
            } else {
                // Fallback to the default resolver
                resolver = ZkDefs.DEFAULT_RESOLVER;
            }
            // Set the resolver if not already set
            setData(curator, ZkPath.CONTAINER_RESOLVER.getPath(containerId), resolver);
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public CreateContainerMetadata getContainerMetadata(String containerId) {
        try {
            byte[] encoded = getByteData(treeCache, ZkPath.CONTAINER_METADATA.getPath(containerId));
            if (encoded == null) {
                return null;
            }
            byte[] decoded = Base64Encoder.decode(encoded);
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(decoded));
            return (CreateContainerMetadata) ois.readObject();
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
            setData(curator, ZkPath.CONTAINER_METADATA.getPath(metadata.getContainerName()), Base64Encoder.encode(ObjectUtils.toBytes(metadata)));
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
            String oldVersionId = getStringData(curator, ZkPath.CONFIG_CONTAINER.getPath(containerId));
            String oldProfileIds = getStringData(curator, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(oldVersionId, containerId));

            setData(curator, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(versionId, containerId), oldProfileIds);
            setData(curator, ZkPath.CONFIG_CONTAINER.getPath(containerId), versionId);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public List<String> getContainerProfiles(String containerId) {
        try {
            String versionId = getStringData(treeCache, ZkPath.CONFIG_CONTAINER.getPath(containerId));
            String str = getStringData(treeCache, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(versionId, containerId));
            return str == null || str.isEmpty() ? Collections.<String>emptyList() : Arrays.asList(str.trim().split(" +"));
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setContainerProfiles(String containerId, List<String> profileIds) {
        try {
            String versionId = getStringData(curator, ZkPath.CONFIG_CONTAINER.getPath(containerId));
            StringBuilder sb = new StringBuilder();
            for (String profileId : profileIds) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(profileId);
            }
            setData(curator, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(versionId, containerId), sb.toString());
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public boolean isContainerAlive(String id) {
        try {
            return exists(curator, ZkPath.CONTAINER_ALIVE.getPath(id)) != null;
        } catch (KeeperException.NoNodeException e) {
            return false;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public String getContainerAttribute(String containerId, ContainerAttribute attribute, String def, boolean mandatory, boolean substituted) {
        if (attribute == ContainerAttribute.Domains) {
            try {
                List<String> list = curator.getChildren().forPath(ZkPath.CONTAINER_DOMAINS.getPath(containerId));
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
                    return getSubstitutedPath(curator, getAttributePath(containerId, attribute));
                } else {
                    return getStringData(curator, getAttributePath(containerId, attribute));
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
                setData(curator, ZkPath.CONTAINER_IP.getPath(containerId), "${zk:" + containerId + "/" + value + "}");
                setData(curator, ZkPath.CONTAINER_RESOLVER.getPath(containerId), value);
            } catch (Exception e) {
                throw new FabricException(e);
            }
        } else {
            try {
//                if (value == null) {
//                    deleteSafe(zk, getAttributePath(containerId, attribute));
//                } else {
                setData(curator, getAttributePath(containerId, attribute), value);
//                }
            } catch (KeeperException.NoNodeException e) {
                // Ignore
            } catch (Exception e) {
                throw new FabricException(e);
            }
        }
    }

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
            default:
                throw new IllegalArgumentException("Unsupported container attribute " + attribute);
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
                setData(curator, ZkPath.CONFIG_DEFAULT_VERSION.getPath(), version);
                setData(curator, ZkPath.CONFIG_VERSION.getPath(version), (String) null);
            }
            return version;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setDefaultVersion(String versionId) {
        try {
            setData(curator, ZkPath.CONFIG_DEFAULT_VERSION.getPath(), versionId);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void createVersion(String version) {
        try {
            create(curator, ZkPath.CONFIG_VERSION.getPath(version));
            create(curator, ZkPath.CONFIG_VERSIONS_PROFILES.getPath(version));
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void createVersion(String parentVersionId, String toVersion) {
        try {
            String sourcePath = ZkPath.CONFIG_VERSION.getPath(parentVersionId);
            String targetPath = ZkPath.CONFIG_VERSION.getPath(toVersion);
            copy(curator, sourcePath, targetPath);
            //After copying a profile it takes a while before the cache is updated.
            //To avoid confusion its best to rebuild that portion of the cache before returning.
            //treeCache.getCurrentData(targetPath);
            treeCache.rebuildNode(targetPath);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void deleteVersion(String version) {
        try {
            deleteSafe(curator, ZkPath.CONFIG_VERSION.getPath(version));
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public List<String> getVersions() {
        try {
            return treeCache.getChildrenNames(ZkPath.CONFIG_VERSIONS.getPath());
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public boolean hasVersion(String name) {
        try {
            if (curator != null && curator.getZookeeperClient().isConnected() && treeCache.getCurrentData(ZkPath.CONFIG_VERSION.getPath(name)) == null) {
                return false;
            }
            return true;
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public List<String> getProfiles(String version) {
        try {
            List<String> profiles = new ArrayList<String>();
            profiles.addAll(treeCache.getChildrenNames(ZkPath.CONFIG_ENSEMBLE_PROFILES.getPath()));
            profiles.addAll(treeCache.getChildrenNames(ZkPath.CONFIG_VERSIONS_PROFILES.getPath(version)));
            return profiles;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public String getProfile(String version, String profile, boolean create) {
        try {
            String path = ZkProfiles.getPath(version, profile);
            if (treeCache.getCurrentData(path) == null) {
                if (!create) {
                    return null;
                } else {
                    createProfile(version, profile);
                    return profile;
                }
            }
            return profile;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public boolean hasProfile(String version, String profile) {
        return getProfile(version, profile, false) != null;
    }

    @Override
    public void createProfile(String version, String profile) {
        try {
            String path = ZkProfiles.getPath(version, profile);
            create(curator, path);
            createDefault(curator, ZKPaths.makePath(path, "org.fusesource.fabric.agent.properties"), "#Profile:" + profile);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void deleteProfile(String version, String name) {
        try {
            String path = ZkProfiles.getPath(version, name);
            deleteSafe(curator, path);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public Map<String, String> getVersionAttributes(String version) {
        try {
            String node = ZkPath.CONFIG_VERSION.getPath(version);
            return getPropertiesAsMap(treeCache, node);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setVersionAttribute(String version, String key, String value) {
        try {
            Map<String, String> props = getVersionAttributes(version);
            if (value != null) {
                props.put(key, value);
            } else {
                props.remove(key);
            }
            String node = ZkPath.CONFIG_VERSION.getPath(version);
            setPropertiesAsMap(curator, node, props);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }


    @Override
    public Map<String, String> getProfileAttributes(String version, String profile) {
        try {
            String path = ZkProfiles.getPath(version, profile);
            return getPropertiesAsMap(treeCache, path);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setProfileAttribute(String version, String profile, String key, String value) {
        try {
            String path = ZkProfiles.getPath(version, profile);
            Properties props = getProperties(curator, path);
            if (value != null) {
                props.setProperty(key, value);
            } else {
                props.remove(key);
            }
            setProperties(curator, path, props);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public long getLastModified(String version, String profile) {
        try {
            return lastModified(curator, ZkProfiles.getPath(version, profile));
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public Map<String, byte[]> getFileConfigurations(String version, String profile) {
        try {
            Map<String, byte[]> configurations = new HashMap<String, byte[]>();
            String path = ZkProfiles.getPath(version, profile);
            List<String> children = getAllChildren(treeCache, path);
            for (String child : children) {
                TreeData data = treeCache.getCurrentData(child);
                if (data.getData() != null && data.getData().length != 0) {
                    String relativePath = child.substring(path.length() + 1);
                    configurations.put(relativePath, getFileConfiguration(version, profile, relativePath));
                }
            }
            return configurations;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public byte[] getFileConfiguration(String version, String profile, String pid) {
        try {
            String path = ZkProfiles.getPath(version, profile) + "/" + pid;
            if (treeCache.getCurrentData(path) == null) {
                return null;
            }
            if (treeCache.getCurrentData(path).getData() == null) {
                List<String> children = treeCache.getChildrenNames(path);
                StringBuilder buf = new StringBuilder();
                for (String child : children) {
                    String value = new String(treeCache.getCurrentData(path + "/" + child).getData(), "UTF-8");
                    buf.append(String.format("%s = %s\n", child, value));
                }
                return buf.toString().getBytes();
            } else {
                return getByteData(treeCache, path);
            }
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setFileConfigurations(String version, String profile, Map<String, byte[]> configurations) {
        try {
            Map<String, byte[]> oldCfgs = getFileConfigurations(version, profile);
            String path = ZkProfiles.getPath(version, profile);

            for (Map.Entry<String, byte[]> entry : configurations.entrySet()) {
                String pid = entry.getKey();
                oldCfgs.remove(pid);
                byte[] newCfg = entry.getValue();
                setFileConfiguration(version, profile, pid, newCfg);
            }

            for (String pid : oldCfgs.keySet()) {
                deleteSafe(curator, path + "/" + pid);
            }
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setFileConfiguration(String version, String profile, String pid, byte[] configuration) {
        try {
            String path = ZkProfiles.getPath(version, profile);
            String configPath = path + "/" + pid;
            if (exists(curator, configPath) != null && getChildren(curator, configPath).size() > 0) {
                List<String> kids = getChildren(curator, configPath);
                ArrayList<String> saved = new ArrayList<String>();
                // old format, we assume that the byte stream is in
                // a .properties format
                for (String line : new String(configuration).split("\n")) {
                    if (line.startsWith("#") || line.length() == 0) {
                        continue;
                    }
                    String nameValue[] = line.split("=", 2);
                    if (nameValue.length < 2) {
                        continue;
                    }
                    String newPath = configPath + "/" + nameValue[0].trim();
                    setData(curator, newPath, nameValue[1].trim());
                    saved.add(nameValue[0].trim());
                }
                for (String kid : kids) {
                    if (!saved.contains(kid)) {
                        deleteSafe(curator, configPath + "/" + kid);
                    }
                }
            } else {
                setData(curator, configPath, configuration);
            }
        } catch (Exception e) {
            throw new FabricException(e);
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
                    configurations.put(pid, DataStoreHelpers.toMap(DataStoreHelpers.toProperties(entry.getValue())));
                }
            }
            return configurations;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public Map<String, String> getConfiguration(String version, String profile, String pid) {
        try {
            String path = ZkProfiles.getPath(version, profile) + "/" + pid + ".properties";
            if (treeCache.getCurrentData(path) == null) {
                return null;
            }
            byte[] data = getByteData(treeCache, path);
            return DataStoreHelpers.toMap(DataStoreHelpers.toProperties(data));
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setConfigurations(String version, String profile, Map<String, Map<String, String>> configurations) {
        try {
            Map<String, Map<String, String>> oldCfgs = getConfigurations(version, profile);
            // Store new configs
            String path = ZkProfiles.getPath(version, profile);
            for (Map.Entry<String, Map<String, String>> entry : configurations.entrySet()) {
                String pid = entry.getKey();
                oldCfgs.remove(pid);
                setConfiguration(version, profile, pid, entry.getValue());
            }
            for (String key : oldCfgs.keySet()) {
                deleteSafe(curator, path + "/" + key + ".properties");
            }
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setConfiguration(String version, String profile, String pid, Map<String, String> configuration) {
        try {
            String path = ZkProfiles.getPath(version, profile);
            byte[] data = DataStoreHelpers.toBytes(DataStoreHelpers.toProperties(configuration));
            String p = path + "/" + pid + ".properties";
            setData(curator, p, data);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public BundleContext getBundleContext() {
        try {
            return FrameworkUtil.getBundle(ZooKeeperDataStore.class).getBundleContext();
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public String getDefaultJvmOptions() {
        try {
            if (curator.getZookeeperClient().isConnected() && exists(curator, JVM_OPTIONS_PATH) != null) {
                return getStringData(treeCache, JVM_OPTIONS_PATH);
            } else {
                return "";
            }
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setDefaultJvmOptions(String jvmOptions) {
        try {
            String opts = jvmOptions != null ? jvmOptions : "";
            setData(curator, JVM_OPTIONS_PATH, opts);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public FabricRequirements getRequirements() {
        try {
            FabricRequirements answer = null;
            if (treeCache.getCurrentData(REQUIREMENTS_JSON_PATH) != null) {
                String json = getStringData(treeCache, REQUIREMENTS_JSON_PATH);
                answer = RequirementsJson.fromJSON(json);
            }
            if (answer == null) {
                answer = new FabricRequirements();
            }
            return answer;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setRequirements(FabricRequirements requirements) throws IOException {
        try {
            requirements.removeEmptyRequirements();
            String json = RequirementsJson.toJSON(requirements);
            setData(curator, REQUIREMENTS_JSON_PATH, json);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public String getClusterId() {
        try {
            return getStringData(curator, ZkPath.CONFIG_ENSEMBLES.getPath());
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public List<String> getEnsembleContainers() {
        List<String> containers = new ArrayList<String>();
        try {
            String ensemble = getStringData(curator, ZkPath.CONFIG_ENSEMBLE.getPath(getClusterId()));
            if (ensemble != null) {
                for (String name : ensemble.trim().split(",")) {
                    containers.add(name);
                }
            }
        } catch (Exception e) {
            throw new FabricException(e);
        }
        return containers;
    }

    private static String substituteZookeeperUrl(String key, CuratorFramework curator) {
        try {
            return new String(ZkPath.loadURL(curator, key), "UTF-8");
        } catch (KeeperException.NoNodeException e) {
            return key;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }
}
