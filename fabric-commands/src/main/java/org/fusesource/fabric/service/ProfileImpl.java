/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.zookeeper.ZooKeeper;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.Profile;
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
            String node = "/fabric/configs/versions/" + version + "/profiles/" + id;
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
            zooKeeper.setData( "/fabric/configs/versions/" + version + "/profiles/" + id, str );
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public void setBundles(URI[] bundles) {
        // TODO
    }

    public URI[] getBundles() {
        return null; // TODO
    }

    public void setFeatures(String[] features) {
        // TODO
    }

    public String[] getFeatures() {
        return null; // TODO
    }

    public void setFeatureRepositories(URI[] repositories) {
        // TODO
    }

    public URI[] getFeatureRepositories() {
        return null; // TODO
    }

    public boolean isOverlay() {
        return false; // TODO
    }

    public Profile getOverlay() {
        return null; // TODO
    }

    /*
    private Map<String, String> getAgentConfiguration() {
        Map<String, String> agent = getProfileConfigurations().get(AGENT_PID);
        if (agent == null) {
            agent = new HashMap<String, String>();
        }
        return agent;
    }
    */

    public Map<String, Map<String, String>> getConfigurations() {
        try {
            Map<String, Map<String, String>> configuration = new HashMap<String, Map<String, String>>();

            // read parents settings
            for (Profile parent : getParents()) {
                configuration.putAll(parent.getConfigurations());
            }

            return readConfigurationTree(configuration);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public void setConfigurations(Map<String, Map<String, String>> configurations) {
        try {
            Map<String, Map<String, String>> oldCfg = readConfigurationTree(new HashMap());

            if (!oldCfg.get(AGENT_PID).equals(configurations.get(AGENT_PID))) {
                throw new FabricException("The " + AGENT_PID + " is read only. Use setBundles/Features/Repositories methods to modify deployment");
            }
            storeInternal(configurations);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    /**
     * Store configuration in zookeeper.
     *
     * @param configurations Configuration to store.
     */
    private void storeInternal(Map<String, Map<String, String>> configurations) {
        try {
            for (String pid : configurations.keySet()) {
                if (zooKeeper.exists("/fabric/configs/versions/" + version + "/profiles/" + id + "/" + pid) == null) {
                    ZooKeeperUtils.createDefault(zooKeeper, "/fabric/configs/versions/" + version + "/profiles/" + id + "/" + pid, null);
                }

                for (Map.Entry<String, String> entry : configurations.get(pid).entrySet()) {
                    if (zooKeeper.exists("/fabric/configs/versions/" + version + "/profiles/" + id + "/" + pid + "/" + entry.getKey()) == null) {
                        ZooKeeperUtils.createDefault(zooKeeper, "/fabric/configs/versions/" + version + "/profiles/" + id + "/" + pid + "/" + entry.getKey(), entry.getValue());
                    } else {
                        zooKeeper.setData("/fabric/configs/versions/" + version + "/profiles/" + id + "/" + pid + "/" + entry.getKey(), entry.getValue());
                    }
                }
            }
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    /**
     * Reads configurations tree and add entries to given map.
     *
     * @param configuration Configuration map.
     * @throws Exception If data cannot be read from zookeeper.
     */
    private Map<String, Map<String, String>> readConfigurationTree(Map<String, Map<String, String>> configuration) throws Exception {
        List<String> pids = zooKeeper.getChildren("/fabric/configs/versions/" + version + "/profiles/" + id);

        for (String pid : pids) {
            if (!configuration.containsKey(pid)) {
                configuration.put(pid, new HashMap<String, String>());
            }
            List<String> keys = zooKeeper.getChildren("/fabric/configs/versions/" + version + "/profiles/" + id + "/" + pid);

            for (String key : keys) {
                configuration.get(pid).put(key, zooKeeper.getStringData("/fabric/configs/versions/" + version + "/profiles/" + id + "/" + pid + "/" + key));
            }
        }

        return configuration;
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
