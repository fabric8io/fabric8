/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.zookeeper.KeeperException;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.service.FabricServiceImpl;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.linkedin.zookeeper.client.IZKClient;

public class ProfileOverlayImpl implements Profile {

    private final String id;
    private final String version;
    private final FabricServiceImpl service;


    public ProfileOverlayImpl(String id, String version, FabricServiceImpl service) {
        this.id = id;
        this.version = version;
        this.service = service;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setParents(Profile[] parents) {
        throw new UnsupportedOperationException("Overlay profiles are read-only.");
    }

    @Override
    public void setConfigurations(Map<String, Map<String, String>> configurations) {
        throw new UnsupportedOperationException("Overlay profiles are read-only.");
    }

    @Override
    public Profile getOverlay() {
        return this;
    }

    @Override
    public boolean isOverlay() {
        return true;
    }

    @Override
    public Profile[] getParents() {
        try {
            IZKClient zooKeeper = service.getZooKeeper();
            String node = ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, id);
            String str = zooKeeper.getStringData(node);
            if (str == null) {
                return new Profile[0];
            }
            List<Profile> profiles = new ArrayList<Profile>();
            for (String p : str.split(" ")) {
                profiles.add(new ProfileImpl(p, version, service));
            }
            return profiles.toArray(new Profile[profiles.size()]);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    @Override
    public Map<String, Map<String, String>> getConfigurations() {
        try {
            Map<String, Map<String, String>> configs = new HashMap<String, Map<String, String>>();
            doGetConfigurations(configs, ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, id));
            return configs;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    private void doGetConfigurations(Map<String, Map<String, String>> configs, String node) throws InterruptedException, KeeperException {
        IZKClient zooKeeper = service.getZooKeeper();
        String data = zooKeeper.getStringData(node);
        String[] parents = data != null ? data.split(" ") : new String[0];
        for (String parent : parents) {
            doGetConfigurations(configs, ZkPath.CONFIG_VERSIONS_PROFILE.getPath(version, parent));
        }
        for (String pid : zooKeeper.getChildren(node)) {
            data = zooKeeper.getStringData(node + "/" + pid);
            if (DELETED.equals(data)) {
                configs.remove(pid);
            } else {
                Map<String, String> cfg = configs.get(pid);
                if (cfg == null) {
                    cfg = new HashMap<String, String>();
                    configs.put(pid, cfg);
                }
                for (String key : zooKeeper.getChildren(node + "/" + pid)) {
                    data = zooKeeper.getStringData(node + "/" + pid + "/" + key);
                    if (DELETED.equals(data)) {
                        cfg.remove(key);
                    } else {
                        cfg.put(key, data != null ? data : "");
                    }
                }
            }
        }
    }

    public void delete() {
        throw new UnsupportedOperationException("Can not delete an overlay profile");
    }

}
