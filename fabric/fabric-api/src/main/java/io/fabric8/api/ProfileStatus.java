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
package io.fabric8.api;

import java.util.List;

/**
 * Represents the status of a profile along with its requirements if defined
 */
public class ProfileStatus {
    private final String profile;
    private final ProfileRequirements requirements;
    private int count;

    public ProfileStatus(String profile, ProfileRequirements requirements) {
        this.profile = profile;
        this.requirements = requirements;
    }

    @Override
    public String toString() {
        return "ProfileStatus[" + profile + ": " + count + "; " + requirements + "]";
    }

    public void incrementCount() {
        ++count;
    }

    public ProfileRequirements requirements() {
        return requirements;
    }

    public int getCount() {
        return count;
    }

    public String getProfile() {
        return profile;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Integer getMinimumInstances() {
        return requirements.getMinimumInstances();
    }

    public double getHealth() {
        return requirements.getHealth(getCount());
    }

    public double getHealth(int instances) {
        return requirements.getHealth(instances);
    }

    public List<String> getDependentProfiles() {
        return requirements.getDependentProfiles();
    }

    public void setProfile(String profile) {
        requirements.setProfile(profile);
    }

    public void setMinimumInstances(Integer minimumInstances) {
        requirements.setMinimumInstances(minimumInstances);
    }

    public void setDependentProfiles(List<String> dependentProfiles) {
        requirements.setDependentProfiles(dependentProfiles);
    }

    public Integer getMaximumInstances() {
        return requirements.getMaximumInstances();
    }

    public void setMaximumInstances(Integer maximumInstances) {
        requirements.setMaximumInstances(maximumInstances);
    }
}
