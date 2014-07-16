/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the current status of the autoscaler for tools to report
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AutoScaleStatus {
    private List<AutoScaleProfileStatus> profileStatuses = new ArrayList<>();

    public AutoScaleStatus() {
    }

    public List<AutoScaleProfileStatus> getProfileStatuses() {
        return profileStatuses;
    }

    public void setProfileStatuses(List<AutoScaleProfileStatus> profileStatuses) {
        this.profileStatuses = profileStatuses;
    }

    /**
     * Returns the current profile status; lazily creating a new entry if its not already there
     */
    public AutoScaleProfileStatus profileStatus(String profile) {
        AutoScaleProfileStatus requirements = findProfileStatus(profile);
        if (requirements == null) {
            requirements = new AutoScaleProfileStatus(profile);
            profileStatuses.add(requirements);
        }
        return requirements;
    }

    public AutoScaleProfileStatus findProfileStatus(String profile) {
        for (AutoScaleProfileStatus profileStatus : profileStatuses) {
            if (profile.equals(profileStatus.getProfile())) {
                return profileStatus;
            }
        }
        return null;
    }

    public boolean removeAutoScaleProfileStatus(String profile) {
        AutoScaleProfileStatus requirements = findProfileStatus(profile);
        if (requirements != null) {
            profileStatuses.remove(requirements);
            return true;
        }
        return false;
    }

    public void addOrUpdateAutoScaleProfileStatus(AutoScaleProfileStatus requirement) {
        removeAutoScaleProfileStatus(requirement.getProfile());
        profileStatuses.add(requirement);
        sortProfilesStatuses();
    }

    protected void sortProfilesStatuses() {
        Collections.sort(profileStatuses);
    }

    
}
