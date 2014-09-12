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
import io.fabric8.api.Version;
import io.fabric8.api.VersionBuilder;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Map;

/**
 * An immutable Version state
 */
public final class VersionState implements Version {

    private final Version delegate;
    
    @ConstructorProperties({"id", "revision", "attributes", "profiles"})
    public VersionState(String versionId, String revision, Map<String, String> attributes, List<Profile> profiles) {
        VersionBuilder builder = VersionBuilder.Factory.create(versionId);
        delegate = builder.setRevision(revision).setAttributes(attributes).addProfiles(profiles).getVersion();
    }

    public VersionState(Version Version) {
        this.delegate = Version;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public String getRevision() {
        return delegate.getRevision();
    }

    @Override
    public Map<String, String> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public List<String> getProfileIds() {
        return delegate.getProfileIds();
    }

    @Override
    public List<Profile> getProfiles() {
        return delegate.getProfiles();
    }

    @Override
    public Profile getProfile(String profileId) {
        return delegate.getProfile(profileId);
    }

    @Override
    public Profile getRequiredProfile(String profileId) {
        return delegate.getRequiredProfile(profileId);
    }

    @Override
    public boolean hasProfile(String profileId) {
        return delegate.hasProfile(profileId);
    }

    @Override
    public int compareTo(Version other) {
        return delegate.compareTo(other);
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
