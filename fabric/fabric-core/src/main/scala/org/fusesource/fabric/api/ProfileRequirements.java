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
import java.util.Arrays;
import java.util.List;

/**
 * Represents the requirements to successfully provision a profile such as the minimum instances required
 * and which other profiles should be profiled before hand.
 */
public class ProfileRequirements {
    private String profile;
    private Integer minimumInstances;
    private Integer maximumInstances;
    private List<String> dependentProfiles;

    public ProfileRequirements() {
    }

    public ProfileRequirements(String profile, Integer minimumInstances) {
        this.profile = profile;
        this.minimumInstances = minimumInstances;
    }

    public ProfileRequirements(String profile, Integer minimumInstances, Integer maximumInstances) {
        this(profile, minimumInstances);
        this.maximumInstances = maximumInstances;
    }

    public ProfileRequirements(String profile, Integer minimumInstances, Integer maximumInstances, List<String> dependentProfiles) {
        this(profile, minimumInstances, maximumInstances);
        this.dependentProfiles = dependentProfiles;
    }

    public ProfileRequirements(String profile, Integer minimumInstances, Integer maximumInstances, String... dependentProfiles) {
        this(profile, minimumInstances, maximumInstances);
        this.dependentProfiles = new ArrayList<String>(Arrays.asList(dependentProfiles));
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public List<String> getDependentProfiles() {
        return dependentProfiles;
    }

    public void setDependentProfiles(List<String> dependentProfiles) {
        this.dependentProfiles = dependentProfiles;
    }

    public Integer getMaximumInstances() {
        return maximumInstances;
    }

    public void setMaximumInstances(Integer maximumInstances) {
        this.maximumInstances = maximumInstances;
    }

    public Integer getMinimumInstances() {
        return minimumInstances;
    }

    public void setMinimumInstances(Integer minimumInstances) {
        this.minimumInstances = minimumInstances;
    }

    /**
     * Returns the health as a percentage for tools based on the current number of instances and the requirements
     */
    public double getHealth(int instances) {
        if (instances <= 0) {
            return 0.0;
        }
        if (minimumInstances != null) {
            int min = minimumInstances.intValue();
            if (min <= 0) {
                return 1.0;
            } else {
                return 1.0 * instances / min;
            }
        }
        // if no minimum assume fine?
        return 1.0;
    }
}
