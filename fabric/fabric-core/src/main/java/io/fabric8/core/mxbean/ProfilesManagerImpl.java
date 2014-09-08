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
import io.fabric8.api.ProfileService;
import io.fabric8.api.Version;
import io.fabric8.api.mxbean.ProfilesManager;

import java.util.List;
import java.util.Map;

import org.jboss.gravia.runtime.ServiceLocator;

/**
 * An MXBean for the {@link}
 */
public final class ProfilesManagerImpl implements ProfilesManager {

    @Override
    public Version createVersion(Version version) {
        return getProfileService().createVersion(version);
    }

    @Override
    public Version createVersion(String sourceId, String targetId, Map<String, String> attributes) {
        return getProfileService().createVersion(sourceId, targetId, attributes);
    }

    @Override
    public List<String> getVersions() {
        return getProfileService().getVersions();
    }

    @Override
    public Version getVersion(String versionId) {
        return getProfileService().getVersion(versionId);
    }

    @Override
    public void deleteVersion(String versionId) {
        getProfileService().deleteVersion(versionId);
    }

    @Override
    public Profile createProfile(Profile profile) {
        return getProfileService().createProfile(profile);
    }

    @Override
    public Profile getProfile(String versionId, String profileId) {
        return getProfileService().getProfile(versionId, profileId);
    }

    @Override
    public Profile updateProfile(Profile profile) {
        return getProfileService().updateProfile(profile);
    }

    @Override
    public void deleteProfile(String versionId, String profileId, boolean force) {
        getProfileService().deleteProfile(null, versionId, profileId, force);
    }

    private ProfileService getProfileService() {
        return ServiceLocator.getRequiredService(ProfileService.class);
    }
}