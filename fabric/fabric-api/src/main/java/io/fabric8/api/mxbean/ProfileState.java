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
package io.fabric8.api.mxbean;

import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An immutable profile state
 */
public final class ProfileState {

    private final Profile delegate;
    
    @ConstructorProperties({"version", "id", "parentIds", "fileConfigurations", "profileHash", "overlay"})
    public ProfileState(String versionId, String profileId, List<String> parents, Map<String, byte[]> fileConfigs, String lastModified, boolean isOverlay) {
        ProfileBuilder builder = ProfileBuilder.Factory.create(versionId, profileId);
        builder.setFileConfigurations(fileConfigs).setLastModified(lastModified).setOverlay(isOverlay);
        delegate = builder.getProfile();
    }

    public ProfileState(Profile profile) {
        this.delegate = profile;
    }
    
    public Profile toProfile() {
        return delegate;
    }

    public String getId() {
        return delegate.getId();
    }

    public String getVersion() {
        return delegate.getVersion();
    }

    public Map<String, String> getAttributes() {
        return delegate.getAttributes();
    }

    public List<String> getParentIds() {
        return delegate.getParentIds();
    }

    public List<String> getLibraries() {
        return delegate.getLibraries();
    }

    public List<String> getEndorsedLibraries() {
        return delegate.getEndorsedLibraries();
    }

    public List<String> getExtensionLibraries() {
        return delegate.getExtensionLibraries();
    }

    public List<String> getBundles() {
        return delegate.getBundles();
    }

    public List<String> getFabs() {
        return delegate.getFabs();
    }

    public List<String> getFeatures() {
        return delegate.getFeatures();
    }

    public List<String> getRepositories() {
        return delegate.getRepositories();
    }

    public List<String> getOverrides() {
        return delegate.getOverrides();
    }

    public List<String> getOptionals() {
        return delegate.getOptionals();
    }

    public String getIconURL() {
        return delegate.getIconURL();
    }

    public String getIconRelativePath() {
        return delegate.getIconRelativePath();
    }

    public String getSummaryMarkdown() {
        return delegate.getSummaryMarkdown();
    }

    public List<String> getTags() {
        return delegate.getTags();
    }

    public Set<String> getConfigurationFileNames() {
        return delegate.getConfigurationFileNames();
    }

    public Map<String, byte[]> getFileConfigurations() {
        return delegate.getFileConfigurations();
    }

    public byte[] getFileConfiguration(String fileName) {
        return delegate.getFileConfiguration(fileName);
    }

    public Map<String, Map<String, String>> getConfigurations() {
        return delegate.getConfigurations();
    }

    public Map<String, String> getConfiguration(String pid) {
        return delegate.getConfiguration(pid);
    }

    public boolean isAbstract() {
        return delegate.isAbstract();
    }
    
    public boolean isLocked() {
        return delegate.isLocked();
    }

    public boolean isHidden() {
        return delegate.isHidden();
    }

    public boolean isOverlay() {
        return delegate.isOverlay();
    }

    public String getProfileHash() {
        return delegate.getProfileHash();
    }


    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ProfileState)) return false;
        ProfileState other = (ProfileState) obj;
        return delegate.equals(other.delegate);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
