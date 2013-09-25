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
import org.apache.curator.framework.recipes.cache.TreeData;
import org.apache.curator.utils.ZKPaths;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.fabric.api.DataStorePlugin;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.FabricRequirements;
import org.fusesource.fabric.api.PlaceholderResolver;
import org.fusesource.fabric.api.jcip.ThreadSafe;
import org.fusesource.fabric.internal.DataStoreHelpers;
import org.fusesource.fabric.internal.RequirementsJson;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.ZkProfiles;
import org.fusesource.fabric.zookeeper.utils.ZookeeperImportUtils;
import org.osgi.service.component.ComponentContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.lastModified;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setData;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setProperties;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setPropertiesAsMap;

/**
 * A pure ZooKeeper based implementation.
 *
 * This implementation requires the fabric-git-zkbridge if you wish to be able to use
 * git to store configuration
 */
@ThreadSafe
@Component(name = "org.fusesource.datastore.zookeeper", description = "Fabric ZooKeeper DataStore")
@Service(DataStorePlugin.class)
@References({
        @Reference(referenceInterface = PlaceholderResolver.class, bind = "bindPlaceholderResolver", unbind = "unbindPlaceholderResolver", cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(referenceInterface = CuratorFramework.class, bind = "bindCurator", unbind = "unbindCurator")
}
)
public final class ZooKeeperDataStore extends AbstractDataStore implements DataStorePlugin<ZooKeeperDataStore> {

    public static final String TYPE = "zookeeper";

    public static final String[] SUPPORTED_CONFIGURATION = {DATASTORE_TYPE_PROPERTY};

    @Activate
    void activate(ComponentContext context) {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        super.stop();
    }

    @Override
    public void start() {
        throw new UnsupportedOperationException("DataStore life cycle is managed");
    }

    @Override
    public void stop() {
        throw new UnsupportedOperationException("DataStore life cycle is managed");
    }

    @Override
    public void importFromFileSystem(String from) {
        assertValid();
        try {
            ZookeeperImportUtils.importFromFileSystem(getCurator(), from, "/", null, null, false, false, false);
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void createVersion(String version) {
        assertValid();
        try {
            create(getCurator(), ZkPath.CONFIG_VERSION.getPath(version));
            create(getCurator(), ZkPath.CONFIG_VERSIONS_PROFILES.getPath(version));
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void createVersion(String parentVersionId, String toVersion) {
        assertValid();
        try {
            String sourcePath = ZkPath.CONFIG_VERSION.getPath(parentVersionId);
            String targetPath = ZkPath.CONFIG_VERSION.getPath(toVersion);
            copy(getCurator(), sourcePath, targetPath);
            //After copying a profile it takes a while before the cache is updated.
            //To avoid confusion its best to rebuild that portion of the cache before returning.
            //getTreeCache().getCurrentData(targetPath);
            getTreeCache().rebuildNode(targetPath);
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void deleteVersion(String version) {
        assertValid();
        try {
            deleteSafe(getCurator(), ZkPath.CONFIG_VERSION.getPath(version));
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public List<String> getVersions() {
        assertValid();
        try {
            return getTreeCache().getChildrenNames(ZkPath.CONFIG_VERSIONS.getPath());
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public boolean hasVersion(String name) {
        assertValid();
        try {
            if (getCurator() != null && getCurator().getZookeeperClient().isConnected() && getTreeCache().getCurrentData(ZkPath.CONFIG_VERSION.getPath(name)) == null) {
                return false;
            }
            return true;
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public List<String> getProfiles(String version) {
        assertValid();
        try {
            List<String> profiles = new ArrayList<String>();
            profiles.addAll(getTreeCache().getChildrenNames(ZkPath.CONFIG_ENSEMBLE_PROFILES.getPath()));
            profiles.addAll(getTreeCache().getChildrenNames(ZkPath.CONFIG_VERSIONS_PROFILES.getPath(version)));
            return profiles;
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public String getProfile(String version, String profile, boolean create) {
        assertValid();
        try {
            String path = ZkProfiles.getPath(version, profile);
            if (getTreeCache().getCurrentData(path) == null) {
                if (!create) {
                    return null;
                } else {
                    createProfile(version, profile);
                    return profile;
                }
            }
            return profile;
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void createProfile(String version, String profile) {
        assertValid();
        try {
            String path = ZkProfiles.getPath(version, profile);
            create(getCurator(), path);
            createDefault(getCurator(), ZKPaths.makePath(path, "org.fusesource.fabric.agent.properties"), "#Profile:" + profile);
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void deleteProfile(String version, String name) {
        assertValid();
        try {
            String path = ZkProfiles.getPath(version, name);
            deleteSafe(getCurator(), path);
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public Map<String, String> getVersionAttributes(String version) {
        assertValid();
        try {
            String node = ZkPath.CONFIG_VERSION.getPath(version);
            return getPropertiesAsMap(getTreeCache(), node);
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void setVersionAttribute(String version, String key, String value) {
        assertValid();
        try {
            Map<String, String> props = getVersionAttributes(version);
            if (value != null) {
                props.put(key, value);
            } else {
                props.remove(key);
            }
            String node = ZkPath.CONFIG_VERSION.getPath(version);
            setPropertiesAsMap(getCurator(), node, props);
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }


    @Override
    public Map<String, String> getProfileAttributes(String version, String profile) {
        assertValid();
        try {
            String path = ZkProfiles.getPath(version, profile);
            return getPropertiesAsMap(getTreeCache(), path);
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void setProfileAttribute(String version, String profile, String key, String value) {
        assertValid();
        try {
            String path = ZkProfiles.getPath(version, profile);
            Properties props = getProperties(getCurator(), path);
            if (value != null) {
                props.setProperty(key, value);
            } else {
                props.remove(key);
            }
            setProperties(getCurator(), path, props);
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public long getLastModified(String version, String profile) {
        assertValid();
        try {
            return lastModified(getCurator(), ZkProfiles.getPath(version, profile));
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public Map<String, byte[]> getFileConfigurations(String version, String profile) {
        assertValid();
        try {
            Map<String, byte[]> configurations = new HashMap<String, byte[]>();
            String path = ZkProfiles.getPath(version, profile);
            List<String> children = getAllChildren(getTreeCache(), path);
            for (String child : children) {
                TreeData data = getTreeCache().getCurrentData(child);
                if (data.getData() != null && data.getData().length != 0) {
                    String relativePath = child.substring(path.length() + 1);
                    configurations.put(relativePath, getFileConfiguration(version, profile, relativePath));
                }
            }
            return configurations;
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public byte[] getFileConfiguration(String version, String profile, String fileName) {
        assertValid();
        try {
            String path = ZkProfiles.getPath(version, profile) + "/" + fileName;
            if (getTreeCache().getCurrentData(path) == null) {
                return null;
            }
            if (getTreeCache().getCurrentData(path).getData() == null) {
                List<String> children = getTreeCache().getChildrenNames(path);
                StringBuilder buf = new StringBuilder();
                for (String child : children) {
                    String value = new String(getTreeCache().getCurrentData(path + "/" + child).getData(), "UTF-8");
                    buf.append(String.format("%s = %s\n", child, value));
                }
                return buf.toString().getBytes();
            } else {
                return getByteData(getTreeCache(), path);
            }
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void setFileConfigurations(String version, String profile, Map<String, byte[]> configurations) {
        assertValid();
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
                deleteSafe(getCurator(), path + "/" + pid);
            }
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void setFileConfiguration(String version, String profile, String fileName, byte[] configuration) {
        assertValid();
        try {
            String path = ZkProfiles.getPath(version, profile);
            String configPath = path + "/" + fileName;
            if (exists(getCurator(), configPath) != null && getChildren(getCurator(), configPath).size() > 0) {
                List<String> kids = getChildren(getCurator(), configPath);
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
                    setData(getCurator(), newPath, nameValue[1].trim());
                    saved.add(nameValue[0].trim());
                }
                for (String kid : kids) {
                    if (!saved.contains(kid)) {
                        deleteSafe(getCurator(), configPath + "/" + kid);
                    }
                }
            } else {
                setData(getCurator(), configPath, configuration);
            }
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public Map<String, String> getConfiguration(String version, String profile, String pid) {
        assertValid();
        try {
            String path = ZkProfiles.getPath(version, profile) + "/" + pid + ".properties";
            if (getTreeCache().getCurrentData(path) == null) {
                return null;
            }
            byte[] data = getByteData(getTreeCache(), path);
            return DataStoreHelpers.toMap(DataStoreHelpers.toProperties(data));
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void setConfigurations(String version, String profile, Map<String, Map<String, String>> configurations) {
        assertValid();
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
                deleteSafe(getCurator(), path + "/" + key + ".properties");
            }
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void setConfiguration(String version, String profile, String pid, Map<String, String> configuration) {
        assertValid();
        try {
            String path = ZkProfiles.getPath(version, profile);
            byte[] data = DataStoreHelpers.toBytes(DataStoreHelpers.toProperties(configuration));
            String p = path + "/" + pid + ".properties";
            setData(getCurator(), p, data);
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public String getDefaultJvmOptions() {
        assertValid();
        try {
            if (getCurator().getZookeeperClient().isConnected() && exists(getCurator(), JVM_OPTIONS_PATH) != null) {
                return getStringData(getTreeCache(), JVM_OPTIONS_PATH);
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
            setData(getCurator(), JVM_OPTIONS_PATH, opts);
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public FabricRequirements getRequirements() {
        assertValid();
        try {
            FabricRequirements answer = null;
            if (getTreeCache().getCurrentData(REQUIREMENTS_JSON_PATH) != null) {
                String json = getStringData(getTreeCache(), REQUIREMENTS_JSON_PATH);
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
    public void setRequirements(FabricRequirements requirements) throws IOException {
        assertValid();
        try {
            requirements.removeEmptyRequirements();
            String json = RequirementsJson.toJSON(requirements);
            setData(getCurator(), REQUIREMENTS_JSON_PATH, json);
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public String getClusterId() {
        assertValid();
        try {
            return getStringData(getCurator(), ZkPath.CONFIG_ENSEMBLES.getPath());
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public List<String> getEnsembleContainers() {
        assertValid();
        List<String> containers = new ArrayList<String>();
        try {
            String ensemble = getStringData(getCurator(), ZkPath.CONFIG_ENSEMBLE.getPath(getClusterId()));
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

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public ZooKeeperDataStore getDataStore() {
        return this;
    }


    @Override
    public Map<String, String> getDataStoreProperties() {
        assertValid();
        return super.getDataStoreProperties();
    }

    @Override
    public void setDataStoreProperties(Map<String, String> dataStoreProperties) {
        assertValid();
        Map<String, String> properties = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : dataStoreProperties.entrySet()) {
            String key = entry.getKey();
            if (Arrays.asList(SUPPORTED_CONFIGURATION).contains(key)) {
                properties.put(key, entry.getValue());
            }
        }
        super.setDataStoreProperties(properties);
    }
}
