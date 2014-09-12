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
package io.fabric8.api.jmx;

import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileService;
import io.fabric8.api.Profiles;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A DTO for the profile metadata
 */
public class ProfileDTO {
    private String id;
    private String version;
    private String profileHash;
    private List<String> parents;
    private boolean overlay;
    private boolean abstractProfile;
    private boolean locked;
    private boolean hidden;
    private Map<String, String> attributes;
    private Set<String> configurations;
    private List<String> bundles;
    private List<String> fabs;
    private List<String> features;
    private List<String> repositories;
    private List<String> overrides;
    private String containersLink;
    private String overlayLink;
    private String requirementsLink;
    private String fileNameLinks;

    public ProfileDTO() {
    }

    public ProfileDTO(Profile profile) {
        this.id = profile.getId();
        this.version = profile.getVersion();
        this.profileHash = profile.getProfileHash();
        this.parents = profile.getParentIds();

        this.overlay = profile.isOverlay();
        this.abstractProfile = profile.isAbstract();
        this.locked = profile.isLocked();
        this.hidden = profile.isHidden();

        this.attributes = profile.getAttributes();
        this.bundles = profile.getBundles();
        this.features = profile.getFeatures();
        this.repositories = profile.getRepositories();
        this.fabs = profile.getFabs();
        this.overrides = profile.getOverrides();

        this.configurations = profile.getConfigurationFileNames();
    }

    public ProfileDTO(Profile profile, String containersLink, String overlayLink, String requirementsLink, String fileNameLinks) {
        this(profile);
        this.containersLink = containersLink;
        this.overlayLink = overlayLink;
        this.requirementsLink = requirementsLink;
        this.fileNameLinks = fileNameLinks;
    }

    @Override
    public String toString() {
        return "ProfileDTO{" +
                "id='" + id + '\'' +
                ", version='" + version + '\'' +
                '}';
    }

    /**
     * Uses the profile DTO as input to the builder
     */
    public void populateBuilder(FabricService fabricService, ProfileService profileService, ProfileBuilder builder) {
        System.out.println("Parents are: " + parents);
        if (parents != null && parents.size() > 0 && version != null) {
            List<Profile> parentProfiles = Profiles.getProfiles(fabricService, parents, version);
            builder.setParents(parentProfiles);
            System.out.println("Found parents: " + parentProfiles);
        }
        builder.setOverlay(overlay);

        // TODO builder doesn't expose it
        //builder.setAbstractProfile(abstractProfile);

        builder.setLocked(locked);

        // TODO builder doesn't expose it
        //builder.setHidden(hidden);

        if (attributes != null) {
            builder.setAttributes(attributes);
        }

        // have to post configuration files individually after creation
        // builder.setConfigurations(configurations);

        if (bundles != null) {
            builder.setBundles(bundles);
        }
        if (fabs != null) {
            builder.setBundles(fabs);
        }
        if (features != null) {
            builder.setBundles(features);
        }
        if (repositories != null) {
            builder.setBundles(repositories);
        }
        if (overrides != null) {
            builder.setBundles(overrides);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getProfileHash() {
        return profileHash;
    }

    public void setProfileHash(String profileHash) {
        this.profileHash = profileHash;
    }

    public List<String> getParents() {
        return parents;
    }

    public void setParents(List<String> parents) {
        this.parents = parents;
    }

    public boolean isOverlay() {
        return overlay;
    }

    public void setOverlay(boolean overlay) {
        this.overlay = overlay;
    }

    public boolean isAbstractProfile() {
        return abstractProfile;
    }

    public void setAbstractProfile(boolean abstractProfile) {
        this.abstractProfile = abstractProfile;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public Set<String> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(Set<String> configurations) {
        this.configurations = configurations;
    }

    public List<String> getBundles() {
        return bundles;
    }

    public void setBundles(List<String> bundles) {
        this.bundles = bundles;
    }

    public List<String> getFabs() {
        return fabs;
    }

    public void setFabs(List<String> fabs) {
        this.fabs = fabs;
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }

    public List<String> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<String> repositories) {
        this.repositories = repositories;
    }

    public List<String> getOverrides() {
        return overrides;
    }

    public void setOverrides(List<String> overrides) {
        this.overrides = overrides;
    }

    public String getContainersLink() {
        return containersLink;
    }

    public void setContainersLink(String containersLink) {
        this.containersLink = containersLink;
    }

    public String getOverlayLink() {
        return overlayLink;
    }

    public void setOverlayLink(String overlayLink) {
        this.overlayLink = overlayLink;
    }

    public String getRequirementsLink() {
        return requirementsLink;
    }

    public void setRequirementsLink(String requirementsLink) {
        this.requirementsLink = requirementsLink;
    }

    public String getFileNameLinks() {
        return fileNameLinks;
    }

    public void setFileNameLinks(String fileNameLinks) {
        this.fileNameLinks = fileNameLinks;
    }

}
