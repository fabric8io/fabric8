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
package io.fabric8.core.mxbean;

import io.fabric8.api.Profile;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileManagerLocator;
import io.fabric8.api.Version;
import io.fabric8.api.mxbean.ProfileManagement;

import java.util.List;
import java.util.Map;

/**
 * An MXBean for the {@link}
 */
public final class ProfileManagementImpl implements ProfileManagement {

    @Override
    public Version createVersion(Version version) {
        return getProfileManager().createVersion(version);
    }

    @Override
    public Version createVersion(String sourceId, String targetId, Map<String, String> attributes) {
        return getProfileManager().createVersion(sourceId, targetId, attributes);
    }

    @Override
    public List<String> getVersions() {
        return getProfileManager().getVersions();
    }

    @Override
    public Version getVersion(String versionId) {
        return getProfileManager().getVersion(versionId);
    }

    @Override
    public void deleteVersion(String versionId) {
        getProfileManager().deleteVersion(versionId);
    }

    @Override
    public Profile createProfile(Profile profile) {
        return getProfileManager().createProfile(profile);
    }

    @Override
    public Profile getProfile(String versionId, String profileId) {
        return getProfileManager().getProfile(versionId, profileId);
    }

    @Override
    public Profile updateProfile(Profile profile) {
        return getProfileManager().updateProfile(profile);
    }

    @Override
    public void deleteProfile(String versionId, String profileId, boolean force) {
        getProfileManager().deleteProfile(versionId, profileId, force);
    }

    private ProfileManager getProfileManager() {
        ProfileManager manager = ProfileManagerLocator.getProfileManager();
        return manager;
    }
}