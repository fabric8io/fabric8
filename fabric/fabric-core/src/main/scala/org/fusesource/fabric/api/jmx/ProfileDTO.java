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
package org.fusesource.fabric.api.jmx;

import org.fusesource.fabric.api.HasId;
import org.fusesource.fabric.api.Ids;
import org.fusesource.fabric.api.Profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * A DTO for returning profiles from JSON MBeans
 */
public class ProfileDTO implements HasId {
    private String id;
    private Properties attributes;
    private List<String> bundles;
    private List<String> fabs;
    private List<String> features;
    private List<String> overrides;
    private List<String> repositories;
    private List<String> parentIds;

    /**
     * Factory method which handles nulls gracefully
     */
    public static ProfileDTO newInstance(Profile profile) {
        if (profile != null) {
            return new ProfileDTO(profile);
        } else {
            return null;
        }
    }

    public static List<ProfileDTO> newInstances(Profile... profiles) {
        List<ProfileDTO> answer = new ArrayList<ProfileDTO>();
        if (profiles != null) {
            for (Profile profile : profiles) {
                ProfileDTO dto = newInstance(profile);
                if (dto != null) {
                    answer.add(dto);
                }
            }
        }
        return answer;
    }

    public ProfileDTO() {
    }

    public ProfileDTO(Profile profile) {
        this.id = profile.getId();
        this.attributes = profile.getAttributes();
        this.bundles = profile.getBundles();
        this.fabs = profile.getFabs();
        this.features = profile.getFeatures();
        this.overrides = profile.getOverrides();
        this.repositories = profile.getRepositories();
        this.parentIds = Ids.getIds(profile.getParents());
    }

    public String toString() {
        return "ProfileDTO(" + id + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProfileDTO that = (ProfileDTO) o;
        if (!id.equals(that.id)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public Properties getAttributes() {
        return attributes;
    }

    public void setAttributes(Properties attributes) {
        this.attributes = attributes;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getOverrides() {
        return overrides;
    }

    public void setOverrides(List<String> overrides) {
        this.overrides = overrides;
    }

    public List<String> getParentIds() {
        return parentIds;
    }

    public void setParentIds(List<String> parentIds) {
        this.parentIds = parentIds;
    }

    public List<String> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<String> repositories) {
        this.repositories = repositories;
    }
}
