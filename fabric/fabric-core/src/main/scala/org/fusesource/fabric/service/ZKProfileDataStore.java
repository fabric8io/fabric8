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

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.ProfileDataStore;
import org.fusesource.fabric.internal.DataStoreHelpers;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.ZkProfiles;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperRetriableUtils;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils;
import org.fusesource.fabric.zookeeper.utils.ZookeeperImportUtils;

import java.io.IOException;
import java.util.*;

/**
 * @author Stan Lewis
 */
public class ZKProfileDataStore implements ProfileDataStore {

    private IZKClient zk;

    public void setZk(IZKClient zk) {
        this.zk = zk;
    }

    public IZKClient getZk() {
        return zk;
    }

    @Override
    public void importFromFileSystem(String from) {
        try {
            ZookeeperImportUtils.importFromFileSystem(zk, from, "/", null, null, false, false, false);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public String getDefaultVersion() {
        try {
            String version = null;
            if (ZooKeeperUtils.exists(zk, ZkPath.CONFIG_DEFAULT_VERSION.getPath()) != null) {
                version = ZooKeeperUtils.get(zk, ZkPath.CONFIG_DEFAULT_VERSION.getPath());
            }
            if (version == null || version.isEmpty()) {
                version = ZkDefs.DEFAULT_VERSION;
                ZooKeeperUtils.set(zk, ZkPath.CONFIG_DEFAULT_VERSION.getPath(), version);
                ZooKeeperUtils.set(zk, ZkPath.CONFIG_VERSION.getPath(version), (String) null);
            }
            return version;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setDefaultVersion(String versionId) {
        try {
            ZooKeeperUtils.set(zk, ZkPath.CONFIG_DEFAULT_VERSION.getPath(), versionId);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void createVersion(String version) {
        try {
            zk.createWithParents(ZkPath.CONFIG_VERSION.getPath(version), CreateMode.PERSISTENT);
            zk.createWithParents(ZkPath.CONFIG_VERSIONS_PROFILES.getPath(version), CreateMode.PERSISTENT);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void createVersion(String parentVersionId, String toVersion) {
        try {
            ZooKeeperUtils.copy(zk, ZkPath.CONFIG_VERSION.getPath(parentVersionId), ZkPath.CONFIG_VERSION.getPath(toVersion));
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void deleteVersion(String version) {
        try {
            zk.deleteWithChildren(ZkPath.CONFIG_VERSION.getPath(version));
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public List<String> getVersions() {
        try {
            return zk.getChildren(ZkPath.CONFIG_VERSIONS.getPath());
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public boolean hasVersion(String name) {
        try {
            if (zk != null && zk.isConnected() && ZooKeeperUtils.exists(zk, ZkPath.CONFIG_VERSION.getPath(name)) == null) {
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
            profiles.addAll(zk.getChildren(ZkPath.CONFIG_ENSEMBLE_PROFILES.getPath()));
            profiles.addAll(zk.getChildren(ZkPath.CONFIG_VERSIONS_PROFILES.getPath(version)));
            return profiles;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public String getProfile(String version, String profile, boolean create) {
        try {
            String path = ZkProfiles.getPath(version, profile);
            if (ZooKeeperUtils.exists(zk, path) == null) {
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
            ZooKeeperUtils.create(zk, path);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void deleteProfile(String version, String name) {
        try {
            String path = ZkProfiles.getPath(version, name);
            zk.deleteWithChildren(path);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }
    
    @Override
    public Properties getVersionAttributes(String version) {
        try {
            String node = ZkPath.CONFIG_VERSION.getPath(version);
            return ZooKeeperUtils.getProperties(zk, node);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setVersionAttribute(String version, String key, String value) {
        try {
            Properties props = getVersionAttributes(version);
            if (value != null) {
                props.setProperty(key, value);
            } else {
                props.remove(key);
            }
            String node = ZkPath.CONFIG_VERSION.getPath(version);
            ZooKeeperUtils.setProperties(zk, node, props);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }


    @Override
    public Properties getProfileAttributes(String version, String profile) {
        try {
            String path = ZkProfiles.getPath(version, profile);
            return ZooKeeperUtils.getProperties(zk, path);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setProfileAttribute(String version, String profile, String key, String value) {
        try {
            String path = ZkProfiles.getPath(version, profile);
            Properties props = ZooKeeperUtils.getProperties(zk, path);
            if (value != null) {
                props.setProperty(key, value);
            } else {
                props.remove(key);
            }
            ZooKeeperUtils.setProperties(zk, path, props);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public Map<String, byte[]> getFileConfigurations(String version, String profile) {
        try {
            Map<String, byte[]> configurations = new HashMap<String, byte[]>();
            String path = ZkProfiles.getPath(version, profile);
            List<String> pids = zk.getChildren(path);
            for (String pid : pids) {
                configurations.put(pid, getFileConfiguration(version, profile, pid));
            }
            return configurations;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public byte[] getFileConfiguration(String version, String profile, String pid) throws InterruptedException, KeeperException {
        String path = ZkProfiles.getPath(version, profile) + "/" + pid;
        if (ZooKeeperUtils.exists(zk, path) == null) {
            return null;
        }
        if (zk.getData(path) == null) {
            List<String> children = zk.getChildren(path);
            StringBuffer buf = new StringBuffer();
            for (String child : children) {
                String value = zk.getStringData(path + "/" + child);
                buf.append(String.format("%s = %s\n", child, value));
            }
            return buf.toString().getBytes();
        } else {
            return zk.getData(path);
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
                zk.deleteWithChildren(path + "/" + pid);
            }
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public void setFileConfiguration(String version, String profile, String pid, byte[] configuration) {
        try {
            String path = ZkProfiles.getPath(version, profile);
            String configPath =  path + "/" + pid;
            if (ZooKeeperUtils.exists(zk, configPath) != null && zk.getChildren(configPath).size() > 0) {
                List<String> kids = zk.getChildren(configPath);
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
                    ZooKeeperRetriableUtils.set(zk, newPath, nameValue[1].trim());
                    saved.add(nameValue[0].trim());
                }
                for ( String kid : kids ) {
                    if (!saved.contains(kid)) {
                        zk.deleteWithChildren(configPath + "/" + kid);
                    }
                }
            } else {
                ZooKeeperRetriableUtils.set(zk, configPath, configuration);
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
            for (Map.Entry<String, byte[]> entry: configs.entrySet()){
                if(entry.getKey().endsWith(".properties")) {
                    String pid = DataStoreHelpers.stripSuffix(entry.getKey(), ".properties");
                    configurations.put(pid, getConfiguration(version, profile, pid));
                }
            }
            return configurations;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public Map<String, String> getConfiguration(String version, String profile, String pid) throws InterruptedException, KeeperException, IOException {
        String path = ZkProfiles.getPath(version, profile) + "/" + pid +".properties";
        if (ZooKeeperUtils.exists(zk, path) == null) {
            return null;
        }
        byte[] data = zk.getData(path);
        return DataStoreHelpers.toMap(DataStoreHelpers.toProperties(data));
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
                zk.deleteWithChildren(path + "/" + key +".properties");
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
            String p =  path + "/" + pid + ".properties";
            ZooKeeperUtils.set(zk, p, data);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }



}
