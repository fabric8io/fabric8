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
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.ProfileService;
import org.linkedin.zookeeper.client.IZKClient;

public class ProfileServiceImpl implements ProfileService {

    private IZKClient zooKeeper;

    public IZKClient getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(IZKClient zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    public String[] getVersions() {
        try {
            List<String> versions = zooKeeper.getChildren("/fabric/configs/versions");
            return versions.toArray(new String[versions.size()]);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public Profile[] getProfiles(String version) {
        try {

            List<String> names = zooKeeper.getChildren("/fabric/configs/versions/" + version + "/profiles");
            List<Profile> profiles = new ArrayList<Profile>();
            for (String name : names) {
                profiles.add(new ProfileImpl(name, version, zooKeeper));
            }
            return profiles.toArray(new Profile[profiles.size()]);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public Profile createProfile(String version, String name) {
        try {
            zooKeeper.createWithParents("/fabric/configs/versions/" + version + "/profiles/" + name, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT );
            return new ProfileImpl(name, version, zooKeeper);
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    public void deleteProfile(Profile profile) {
        try {
            zooKeeper.deleteWithChildren("/fabric/configs/versions/" + profile.getVersion() + "/profiles/" + profile.getId());
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }
}
