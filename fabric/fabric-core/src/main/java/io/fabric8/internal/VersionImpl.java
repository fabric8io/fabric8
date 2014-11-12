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
package io.fabric8.internal;

import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.api.VersionSequence;
import io.fabric8.api.jcip.Immutable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.fabric8.api.gravia.IllegalStateAssertion;

/**
 * The immutable profile version
 * 
 */
@Immutable
final class VersionImpl implements Version {

    private final String versionId;
    private final String revision;
    private final Map<String, String> attributes;
    private final Map<String, Profile> profiles = new LinkedHashMap<>();

    VersionImpl(String versionId, String revision, Map<String, String> attributes, List<Profile> prflist) {
        this.versionId = versionId;
        this.revision = revision;
        this.attributes = new HashMap<>(attributes);
        for (Profile prf : prflist) {
            profiles.put(prf.getId(), prf);
        }
    }

    @Override
    public String getId() {
        return versionId;
    }

    @Override
    public String getRevision() {
        return revision;
    }

    @Override
    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    public List<String> getProfileIds() {
        List<String> result = new ArrayList<>(profiles.keySet());
        return Collections.unmodifiableList(result);
    }

    @Override
    public List<Profile> getProfiles() {
        List<Profile> prflist = new ArrayList<>(profiles.values());
        return Collections.unmodifiableList(prflist);
    }

    @Override
    public Profile getProfile(String profileId) {
        return profiles.get(profileId);
    }

    @Override
    public Profile getRequiredProfile(String profileId) {
        Profile profile = profiles.get(profileId);
        IllegalStateAssertion.assertNotNull(profile, "Profile '" + profileId + "' does not exist in version: " + versionId);
        return profile;
    }

    @Override
    public boolean hasProfile(String profileId) {
        return profiles.get(profileId) != null;
    }

    @Override
    public int compareTo(Version other) {
        return new VersionSequence(versionId).compareTo(new VersionSequence(other.getId()));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VersionImpl)) return false;
        VersionImpl other = (VersionImpl) obj;
        return versionId.equals(other.versionId);
    }

    @Override
    public int hashCode() {
        return versionId.hashCode();
    }

    @Override
    public String toString() {
        return "Version[id=" + versionId + "]";
    }
}
