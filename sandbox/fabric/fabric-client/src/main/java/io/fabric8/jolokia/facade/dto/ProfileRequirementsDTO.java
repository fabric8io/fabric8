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
package io.fabric8.jolokia.facade.dto;

import java.util.Collection;

/**
 * Author: lhein
 */
public class ProfileRequirementsDTO {
    public String profile;
    public Integer count;
    public Double health;
    public Integer minimumInstances;
    public Integer maximumInstances;
    public Collection<String> dependentProfiles;

    @Override
    public String toString() {
        String deps = "";
        if (dependentProfiles != null) {
            for (String dep : dependentProfiles) {
                if (deps.length()>0) deps += ", ";
                deps += dep;
            }
        }

        return String.format("ProfileRequirements: (profile: %s, minimumInstances: %d, maximumInstances: %d, count: %d, health: %s, dependentProfiles: %s)",
                profile,
                minimumInstances,
                maximumInstances,
                count,
                health,
                deps);
    }
}
