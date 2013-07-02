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
package org.fusesource.fabric.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Allows the requirements of a profile to be defined so that we can do automatic provisioning,
 * can ensure that required services stay running and can provide health checks
 */
public class FabricRequirements {
    private List<ProfileRequirements> profileRequirements = new ArrayList<ProfileRequirements>();

    public FabricRequirements() {
    }

    public FabricRequirements(List<ProfileRequirements> profileRequirements) {
        this();
        this.profileRequirements = profileRequirements;
        sortProfilesRequirements();
    }

    public List<ProfileRequirements> getProfileRequirements() {
        return profileRequirements;
    }

    public void setProfileRequirements(List<ProfileRequirements> profileRequirements) {
        this.profileRequirements = profileRequirements;
        sortProfilesRequirements();
    }

    /**
     * Returns or creates a new {@link ProfileRequirements} for the given profile id
     */
    public ProfileRequirements getOrCreateProfileRequirement(String profile) {
        ProfileRequirements requirements = findProfileRequirements(profile);
        if (requirements == null) {
            requirements = new ProfileRequirements(profile);
            profileRequirements.add(requirements);
        }
        return requirements;
    }

    public ProfileRequirements findProfileRequirements(String profile) {
        for (ProfileRequirements profileRequirement : profileRequirements) {
            if (profile.equals(profileRequirement.getProfile())) {
                return profileRequirement;
            }
        }
        return null;
    }

    public void removeProfileRequirements(String profile) {
        ProfileRequirements requirements = findProfileRequirements(profile);
        if (requirements != null) {
            profileRequirements.remove(requirements);
        }
    }

    public void addOrUpdateProfileRequirements(ProfileRequirements requirement) {
        removeProfileRequirements(requirement.getProfile());
        profileRequirements.add(requirement);
        sortProfilesRequirements();
    }

    protected void sortProfilesRequirements() {
        Collections.sort(profileRequirements);
    }

    /**
     * Removes all the empty requirements; usually used just before saving to JSON
     */
    public void removeEmptyRequirements() {
        Iterator<ProfileRequirements> iterator = profileRequirements.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().checkIsEmpty()) {
                iterator.remove();
            }
        }
    }

    @Override
    public String toString() {
        return "FabricRequirements" + profileRequirements;
    }
}
