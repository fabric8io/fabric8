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

import io.fabric8.api.Constants;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An immutable profile state
 */
public final class ProfileState implements Profile {

    private final Profile delegate;
    
    @ConstructorProperties({"version", "id", "parentIds", "fileConfigurations", "profileHash", "overlay"})
    public ProfileState(String versionId, String profileId, List<String> parents, Map<String, byte[]> fileConfigs, String lastModified, boolean isOverlay) {
        ProfileBuilder builder = ProfileBuilder.Factory.create(versionId, profileId);
        builder.setFileConfigurations(fileConfigs).setLastModified(lastModified).setOverlay(isOverlay);
        builder.addConfiguration(Constants.AGENT_PID, Profile.ATTRIBUTE_PREFIX + Profile.PARENTS, parentsAttributeValue(parents));
        delegate = builder.getProfile();
    }

    public ProfileState(Profile profile) {
        this.delegate = profile;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public String getVersion() {
        return delegate.getVersion();
    }

    @Override
    public Map<String, String> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public List<String> getParentIds() {
        return delegate.getParentIds();
    }

    @Override
    public List<String> getLibraries() {
        return delegate.getLibraries();
    }

    @Override
    public List<String> getEndorsedLibraries() {
        return delegate.getEndorsedLibraries();
    }

    @Override
    public List<String> getExtensionLibraries() {
        return delegate.getExtensionLibraries();
    }

    @Override
    public List<String> getBundles() {
        return delegate.getBundles();
    }

    @Override
    public List<String> getFabs() {
        return delegate.getFabs();
    }

    @Override
    public List<String> getFeatures() {
        return delegate.getFeatures();
    }

    @Override
    public List<String> getRepositories() {
        return delegate.getRepositories();
    }

    @Override
    public List<String> getOverrides() {
        return delegate.getOverrides();
    }

    @Override
    public List<String> getOptionals() {
        return delegate.getOptionals();
    }

    @Override
    public String getIconURL() {
        return delegate.getIconURL();
    }

    @Override
    public String getIconRelativePath() {
        return delegate.getIconRelativePath();
    }

    @Override
    public String getSummaryMarkdown() {
        return delegate.getSummaryMarkdown();
    }

    @Override
    public List<String> getTags() {
        return delegate.getTags();
    }

    @Override
    public Set<String> getConfigurationFileNames() {
        return delegate.getConfigurationFileNames();
    }

    @Override
    public Map<String, byte[]> getFileConfigurations() {
        return delegate.getFileConfigurations();
    }

    @Override
    public byte[] getFileConfiguration(String fileName) {
        return delegate.getFileConfiguration(fileName);
    }

    @Override
    public Map<String, Map<String, String>> getConfigurations() {
        return delegate.getConfigurations();
    }

    @Override
    public Map<String, String> getConfiguration(String pid) {
        return delegate.getConfiguration(pid);
    }

    @Override
    public boolean isOverlay() {
        return delegate.isOverlay();
    }

    @Override
    public boolean isAbstract() {
        return delegate.isAbstract();
    }

    @Override
    public int compareTo(Profile o) {
        return delegate.compareTo(o);
    }

    @Override
    public boolean isLocked() {
        return delegate.isLocked();
    }

    @Override
    public boolean isHidden() {
        return delegate.isHidden();
    }

    @Override
    public String getProfileHash() {
        return delegate.getProfileHash();
    }

    private String parentsAttributeValue(List<String> parents) {
        String pspec = "";
        for (String parentId : parents) {
            pspec += " " + parentId;
        }
        pspec = pspec.substring(1);
        return pspec;
    }
    
    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
