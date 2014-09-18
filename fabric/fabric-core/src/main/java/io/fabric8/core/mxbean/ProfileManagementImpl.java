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
import io.fabric8.api.mxbean.ProfileState;
import io.fabric8.api.mxbean.VersionState;

import java.util.List;
import java.util.Map;

/**
 * An MXBean for the {@link}
 */
public final class ProfileManagementImpl implements ProfileManagement {

    @Override
    public VersionState createVersion(VersionState versionState) {
        Version version = getProfileManager().createVersion(versionState.toVersion());
        return new VersionState(version);
    }

    @Override
    public VersionState createVersion(String sourceId, String targetId, Map<String, String> attributes) {
        Version version = getProfileManager().createVersion(sourceId, targetId, attributes);
        return new VersionState(version);
    }

    @Override
    public List<String> getVersionIds() {
        return getProfileManager().getVersions();
    }

    @Override
    public VersionState getVersion(String versionId) {
        Version version = getProfileManager().getVersion(versionId);
        return version != null ? new VersionState(version) : null;
    }

    @Override
    public void deleteVersion(String versionId) {
        getProfileManager().deleteVersion(versionId);
    }

    @Override
    public ProfileState createProfile(ProfileState profileState) {
        Profile profile = getProfileManager().createProfile(profileState.toProfile());
        return new ProfileState(profile);
    }

    @Override
    public ProfileState getProfile(String versionId, String profileId) {
        Profile profile = getProfileManager().getProfile(versionId, profileId);
        return profile != null ? new ProfileState(profile) : null;
    }

    @Override
    public ProfileState updateProfile(ProfileState profileState) {
        Profile profile = getProfileManager().updateProfile(profileState.toProfile());
        return new ProfileState(profile);
    }

    @Override
    public void deleteProfile(String versionId, String profileId, boolean force) {
        getProfileManager().deleteProfile(versionId, profileId, force);
    }

    private ProfileManager getProfileManager() {
        return ProfileManagerLocator.getProfileManager();
    }
}