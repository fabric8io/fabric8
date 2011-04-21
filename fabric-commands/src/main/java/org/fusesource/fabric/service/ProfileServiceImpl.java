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
import java.util.*;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.ProfileService;
import org.fusesource.fabric.util.ZkPath;
import org.linkedin.zookeeper.client.IZKClient;

public class ProfileServiceImpl implements ProfileService {

    private IZKClient zooKeeper;
    private ZooKeeperTemplate template;


    public IZKClient getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(IZKClient zooKeeper) {
        this.zooKeeper = zooKeeper;
        this.template = new ZooKeeperTemplate(zooKeeper);
    }

    public Map<String, Profile> getProfiles(String version) {
        return template.execute(ZkPath.PROFILES.getPath(version), new ProfileReadCallback(version));
    }

    public Profile createProfile(String version, String name) {
        try {
            zooKeeper.createWithParents(ZkPath.PROFILE.getPath(version, name), null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            zooKeeper.createWithParents(ZkPath.PROFILE_PARENTS.getPath(version, name), null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            zooKeeper.createWithParents(ZkPath.PROFILE_BUNDLES.getPath(version, name), null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            zooKeeper.createWithParents(ZkPath.PROFILE_REPOSITORIES.getPath(version, name), null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            zooKeeper.createWithParents(ZkPath.PROFILE_FEATURES.getPath(version, name), null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            zooKeeper.createWithParents(ZkPath.PROFILE_CONFIG_PIDS.getPath(version, name), null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

            return new ProfileImpl(name, version);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteProfile(Profile profile) {
        try {
            zooKeeper.deleteWithChildren(ZkPath.PROFILE.getPath(profile.getVersion(), profile.getName()));

            // remove references in children profiles
            for (Profile child : profile.getExtensions()) {
                zooKeeper.delete(ZkPath.PROFILE_PARENT.getPath(profile.getVersion(), child.getName(), profile.getName()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void updateProfile(String name, Profile newProfile) throws Exception {
        String version = newProfile.getVersion(); // version is read only in interface
        Map<String, Profile> profiles = getProfiles(newProfile.getVersion());

        if (!profiles.containsKey(name)) {
            throw new IllegalArgumentException("Unknown profile " + name);
        }

        Profile oldProfile = profiles.get(name);
        if (!Arrays.equals(oldProfile.getBundles(), newProfile.getBundles())) {
           zooKeeper.setData(ZkPath.PROFILE_BUNDLES.getPath(version, name), arrayToString(newProfile.getBundles()));
        }

        if (!Arrays.equals(oldProfile.getFeatures(), newProfile.getFeatures())) {
           zooKeeper.setData(ZkPath.PROFILE_FEATURES.getPath(version, name), arrayToString(newProfile.getFeatures()));
        }

        if (!Arrays.equals(oldProfile.getFeatureRepositories(), newProfile.getFeatureRepositories())) {
           zooKeeper.setData(ZkPath.PROFILE_REPOSITORIES.getPath(version, name), arrayToString(newProfile.getFeatureRepositories()));
        }

        Map<String, Map<String, String>> cfg = newProfile.getConfigurations();
        if (!oldProfile.getConfigurations().equals(cfg)) {
            zooKeeper.deleteWithChildren(ZkPath.PROFILE_CONFIG_PIDS.getPath(version, name));

            for (String key : cfg.keySet()) {
                zooKeeper.createWithParents(ZkPath.PROFILE_CONFIG_KEYS.getPath(version, name, key),
                    "", ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

                for (Map.Entry<String, String> entry : cfg.get(key).entrySet()) {
                    zooKeeper.createWithParents(ZkPath.PROFILE_CONFIG_VALUE.getPath(version, name, key, entry.getKey()),
                        entry.getValue(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
            }
        }
    }

    private String arrayToString(Object[] array) {
        StringBuffer buffer = new StringBuffer();
        for (Object elem : array) {
            buffer.append(elem).append("\n");
        }
        return buffer.toString().trim(); //remove last new line
    }

    public void removeProfile(Profile profile) throws Exception {
        zooKeeper.deleteWithChildren(ZkPath.PROFILE.getPath(profile.getName()));
    }

    /**
     * This callback reads tree structure and fill Profile pojo.
     */
    public class ProfileReadCallback implements ZooKeeperTemplate.ChildrenCallback<Map<String, Profile>> {

        private String version;

        public ProfileReadCallback(String version) {
            this.version = version;
        }

        public Map<String, Profile> execute(List<String> children) throws Exception {
            Map<String, Profile> profiles = new HashMap<String, Profile>();
            Map<String, List<Profile>> extensions = new HashMap<String, List<Profile>>();

            for (String profileName : children) {
                if (!profiles.containsKey(profileName)) { // profile may be created before all children are read
                    profiles.put(profileName, createProfile(profiles, extensions, profileName));
                }
            }

            for (Map.Entry<String, List<Profile>> entry : extensions.entrySet()) {
                String key = entry.getKey();
                List<Profile> value = entry.getValue();

                if (profiles.containsKey(key)) {
                    profiles.get(key).setExtensions(value.toArray(new Profile[value.size()]));
                }
            }

            return profiles;
        }

        public Profile createProfile(final Map<String, Profile> profiles, final Map<String, List<Profile>> extensions, final String name) {
            final ProfileImpl profile = new ProfileImpl(name, version);
            profile.setBundles(template.execute(ZkPath.PROFILE_BUNDLES.getPath(version, name), new DataToUriCallback()));
            profile.setFeatures(template.execute(ZkPath.PROFILE_FEATURES.getPath(version, name), new ZooKeeperTemplate.StringArrayCallback()));
            profile.setConfigurations(template.execute(ZkPath.PROFILE_CONFIG_PIDS.getPath(version, name), new ConfigurationsCallback(version, name)));
            profile.setFeatureRepositories(template.execute(ZkPath.PROFILE_REPOSITORIES.getPath(version, name), new DataToUriCallback()));

            profile.setParents(template.execute(ZkPath.PROFILE_PARENTS.getPath(version, name), new ZooKeeperTemplate.ChildrenCallback<Profile[]>() {
                public Profile[] execute(List<String> children) throws Exception {
                    Profile[] array = new Profile[children.size()];

                    int i = 0;
                    for (String profileName : children) {
                        if (profiles.containsKey(profileName)) {
                            array[i++] = profiles.get(profileName);
                        } else {
                            array[i++] = createProfile(profiles, extensions, profileName);
                        }


                        if (!extensions.containsKey(profileName)) {
                            extensions.put(profileName, new ArrayList<Profile>());
                        }

                        extensions.get(profileName).add(profile);
                    }

                    return array;
                }
            }));

            return profile;
        }
    }

    public class DataToUriCallback implements ZooKeeperTemplate.DataCallback<URI[]> {
        public URI[] execute(String data) throws Exception {
            if (data == null) {
                return new URI[0];
            }

            String[] uris = data.length() > 0 ? data.split("\n") : new String[0];
            URI[] urisArray = new URI[uris.length];

            for (int i = 0; i < uris.length; i++) {
                urisArray[i] = new URI(uris[i]);
            }

            return urisArray;
        }
    }

    /**
     * Read
     */
    public class ConfigurationsCallback implements ZooKeeperTemplate.ChildrenCallback<Map<String, Map<String, String>>> {

        /**
         * Profile version.
         */
        private String version;

        /**
         * Profile name.
         */
        private String name;

        public ConfigurationsCallback(String version, String name) {
            this.version = version;
            this.name = name;
        }

        public Map<String, Map<String, String>> execute(List<String> children) throws Exception {
            final Map<String, Map<String, String>> configs = new HashMap<String, Map<String, String>>();

            for (String pid : children) {
                final String localPid = pid;
                configs.put(pid, template.execute(ZkPath.PROFILE_CONFIG_KEYS.getPath(version, name, pid), new ZooKeeperTemplate.ChildrenCallback<Map<String, String>>() {
                    public Map<String, String> execute(List<String> children) throws Exception {
                        Map<String, String> configs = new HashMap<String, String>();
                        for (String key : children) {
                            configs.put(key, template.execute(ZkPath.PROFILE_CONFIG_VALUE.getPath(version, name, localPid, key)));
                        }
                        return configs;
                    }
                }));
            }
            return configs;
        }
    }
}
