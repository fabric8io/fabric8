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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An immutable Version state
 */
public final class VersionState {

    private final Version delegate;
    
    @ConstructorProperties({"id", "revision", "attributes", "profileStates"})
    public VersionState(String versionId, String revision, Map<String, String> attributes, List<ProfileState> profileStates) {
        VersionBuilder builder = VersionBuilder.Factory.create(versionId);
        builder.setRevision(revision);
        if (attributes != null) {
            builder.setAttributes(attributes);
        }
        if (profileStates != null) {
            for (ProfileState ps : profileStates) {
                builder.addProfile(ps.toProfile());
            }
        }
        delegate = builder.getVersion();
    }

    public VersionState(Version Version) {
        this.delegate = Version;
    }

    public Version toVersion() {
        return delegate;
    }
    
    public String getId() {
        return delegate.getId();
    }

    public String getRevision() {
        return delegate.getRevision();
    }

    public Map<String, String> getAttributes() {
        return delegate.getAttributes();
    }

    public List<String> getProfiles() {
        return delegate.getProfileIds();
    }

    public List<ProfileState> getProfileStates() {
        List<ProfileState> result = new ArrayList<>();
        for (Profile prf : delegate.getProfiles()) {
            result.add(new ProfileState(prf));
        }
        return result;
    }

    public ProfileState getProfileState(String profileId) {
        Profile prf = delegate.getProfile(profileId);
        return new ProfileState(prf);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VersionState)) return false;
        VersionState other = (VersionState) obj;
        return delegate.equals(other.delegate);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
