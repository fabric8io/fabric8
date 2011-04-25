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

import org.fusesource.fabric.api.Profile;
import org.linkedin.zookeeper.client.IZKClient;

public class ProfileImpl implements Profile {


    private String name;
    private String version;

    private Profile[] parents = new Profile[0];
    private Profile[] extensions = new Profile[0];

    private URI[] bundles = new URI[0];
    private String[] features = new String[0];
    private URI[] featureRepositories;
    private Map<String, Map<String,String>> configurations = new HashMap<String, Map<String, String>>();

    public ProfileImpl(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Profile[] getParents() {
        return this.parents;
    }

    public void setParents(Profile[] parents) {
        this.parents = parents;
    }

    public URI[] getBundles() {
        return this.bundles;
    }

    public void setBundles(URI[] bundles) {
        this.bundles = bundles;
    }

    public String[] getFeatures() {
        return this.features;
    }

    public void setFeatures(String[] features) {
        this.features = features;
    }

    public Map<String, Map<String, String>> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(Map<String, Map<String, String>> configurations) {
        this.configurations = configurations;
    }

    public URI[] getFeatureRepositories() {
        return featureRepositories;
    }

    public void setFeatureRepositories(URI[] uris) {
        featureRepositories = uris;
    }

    public Profile[] getExtensions() {
        return this.extensions;
    }

    public void setExtensions(Profile[] extensions) {
        this.extensions = extensions;
    }

    @Override
    public String toString() {
        return String.format("ProfileImpl[name=%s,version=%s]", name, version);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProfileImpl profile = (ProfileImpl) o;
        if (!name.equals(profile.name)) return false;
        if (!version.equals(profile.version)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }
}
