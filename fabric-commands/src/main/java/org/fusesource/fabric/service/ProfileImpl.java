/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.zookeeper.KeeperException;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.linkedin.zookeeper.client.IZKClient;

public class ProfileImpl implements Profile {

    private final static String AGENT_PID = "org.fusesource.fabric.agent";

    private final String id;
    private final String version;
    private final IZKClient zooKeeper;

    public ProfileImpl(String id, String version, IZKClient zooKeeper) {
        this.id = id;
        this.version = version;
        this.zooKeeper = zooKeeper;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public Profile[] getParents() {
        try {
            String node = ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, id);
            String str = zooKeeper.getStringData(node);
            if (str == null) {
                return new Profile[0];
            }
            List<Profile> profiles = new ArrayList<Profile>();
            for (String p : str.split(" ")) {
                profiles.add(new ProfileImpl(p, version, zooKeeper));
            }
            return profiles.toArray(new Profile[profiles.size()]);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public void setParents(Profile[] parents) {
        try {
            String str = "";
            for (Profile parent : parents) {
                if (!version.equals(parent.getVersion())) {
                    throw new IllegalArgumentException("Bad profile: " + parent);
                }
                if (!str.isEmpty()) {
                    str += " ";
                }
                str += parent.getId();
            }
            zooKeeper.setData( ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, id), str );
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public boolean isOverlay() {
        return false;
    }

    public Profile getOverlay() {
        return new ProfileOverlayImpl(id, version, zooKeeper);
    }

    public Map<String, Map<String, String>> getConfigurations() {
        try {
            Map<String, Map<String, String>> configurations = new HashMap<String, Map<String, String>>();
            String path = ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, id);
            List<String> pids = zooKeeper.getChildren(path);
            for (String pid : pids) {
                configurations.put(pid, getConfiguration(pid));
            }
            return configurations;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public void setConfigurations(Map<String, Map<String, String>> configurations) {
        try {
            Map<String, Map<String, String>> oldCfgs = getConfigurations();
            // Store new configs
            String path = ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, id);
            for (String pid : configurations.keySet()) {
                Map<String, String> oldCfg = oldCfgs.remove(pid);
                Map<String, String> newCfg = configurations.get(pid);
                if (newCfg.containsKey(DELETED)) {
                    if (!oldCfg.containsKey(DELETED)) {
                        ZooKeeperUtils.set(zooKeeper, path + "/" + pid, DELETED);
                    }
                    for (String key : zooKeeper.getChildren(path + "/" + pid)) {
                        zooKeeper.deleteWithChildren(path + "/" + pid + "/" + key);
                    }
                } else {
                    ZooKeeperUtils.set(zooKeeper, path + "/" + pid, "");
                    for (Map.Entry<String, String> entry : configurations.get(pid).entrySet()) {
                        String oldValue = oldCfg != null ? oldCfg.remove(entry.getKey()) : null;
                        if (oldValue == null || !oldValue.equals(entry.getValue())) {
                            ZooKeeperUtils.set(zooKeeper, path + "/" + pid + "/" + entry.getKey(), entry.getValue());
                        }
                    }
                    for (String key : oldCfg.keySet()) {
                        zooKeeper.deleteWithChildren(path + "/" + pid + "/" + key);
                    }
                }
            }
            for (String key : oldCfgs.keySet()) {
                zooKeeper.deleteWithChildren(path + "/" + key);
            }
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    private Map<String, String> getConfiguration(String pid) throws InterruptedException, KeeperException {
        String path = ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, id) + "/" + pid;
        if (zooKeeper.exists(path) == null) {
            return null;
        }
        Map<String, String> map = new HashMap<String, String>();
        String data = zooKeeper.getStringData(path);
        if (DELETED.equals(data)) {
            map.put(DELETED,  "");
        } else {
            List<String> keys = zooKeeper.getChildren(path);
            for (String key : keys) {
                data = zooKeeper.getStringData(path + "/" + key);
                map.put(key, data != null ? data : "");
            }
        }
        return map;
    }

    @Override
    public String toString() {
        return "ProfileImpl[" +
                "id='" + id + '\'' +
                ", version='" + version + '\'' +
                ']';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProfileImpl profile = (ProfileImpl) o;
        if (!id.equals(profile.id)) return false;
        if (!version.equals(profile.version)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }
}
