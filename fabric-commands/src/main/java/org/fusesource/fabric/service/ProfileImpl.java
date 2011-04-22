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
import java.util.Map;

import org.fusesource.fabric.api.Profile;
import org.linkedin.zookeeper.client.IZKClient;

public class ProfileImpl implements Profile {

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
            throw new RuntimeException(e);
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
            throw new RuntimeException(e);
        }
    }

    public Map<String, Map<String, String>> getConfigurations() {
        // TODO
        return null;
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
