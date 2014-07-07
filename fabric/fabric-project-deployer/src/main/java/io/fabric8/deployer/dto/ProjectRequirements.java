/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.deployer.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Represents the project requirements and dependencies and how they should be mapped to a profile in fabric8
 */
public class ProjectRequirements {
    private String profileId;
    private boolean abstractProfile;
    private String version;
    private String baseVersion;
    private String description;
    private List<String> parentProfiles;
    private List<String> features;
    private List<String> bundles;
    private List<String> featureRepositories;
    private Integer minimumInstances;
    private DependencyDTO rootDependency;
    private String webContextPath;

    @Override
    public String toString() {
        return "ProjectRequirements{" + rootDependency + "}";
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public boolean isAbstractProfile() {
        return abstractProfile;
    }

    public void setAbstractProfile(boolean abstractProfile) {
        this.abstractProfile = abstractProfile;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getBaseVersion() {
        return baseVersion;
    }

    /**
     * Sets the base version to use if creating a new version (which is like saying which branch to create the initial new version branch from)
     */
    public void setBaseVersion(String baseVersion) {
        this.baseVersion = baseVersion;
    }

    /**
     * Returns the root maven dependency and all of its child dependency tree
     */
    public DependencyDTO getRootDependency() {
        return rootDependency;
    }

    public void setRootDependency(DependencyDTO rootDependency) {
        this.rootDependency = rootDependency;
    }

    @JsonIgnore
    public String getGroupId() {
        return rootDependency != null ? rootDependency.getGroupId() : null;
    }

    @JsonIgnore
    public String getArtifactId() {
        return rootDependency != null ? rootDependency.getArtifactId() : null;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getParentProfiles() {
        return parentProfiles;
    }

    public void setParentProfiles(List<String> parentProfiles) {
        this.parentProfiles = parentProfiles;
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }

    public void setFeatureRepositories(List<String> featureRepositories) {
        this.featureRepositories = featureRepositories;
    }

    public List<String> getFeatureRepositories() {
        return featureRepositories;
    }

    public List<String> getBundles() {
        return bundles;
    }

    public void setBundles(List<String> bundles) {
        this.bundles = bundles;
    }

    public Integer getMinimumInstances() {
        return minimumInstances;
    }

    public void setMinimumInstances(Integer minimumInstances) {
        this.minimumInstances = minimumInstances;
    }

    public String getWebContextPath() {
        return webContextPath;
    }

    public void setWebContextPath(String webContextPath) {
        this.webContextPath = webContextPath;
    }
}
