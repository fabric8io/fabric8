/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.fabric8.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.api.FabricRequirements;
import io.fabric8.api.ProfileRequirements;
import io.fabric8.commands.support.ChangeRequirementSupport;

import java.util.List;

@Command(name = "require-profile-set", scope = "fabric", description = "Sets the requirements of a profile in terms of its minimum and maximum required instances", detailedDescription = "classpath:status.txt")
public class RequireProfileSet extends ChangeRequirementSupport {

    @Option(name = "--minimum", multiValued = false, required = false, description = "The minimum number of instances expected of this profile in the fabric")
    protected Integer minimumInstances;
    @Option(name = "--maximum", multiValued = false, required = false, description = "The maximum number of instances expected of this profile in the fabric")
    protected Integer maximumInstances;
    @Option(name = "--dependsOn", multiValued = true, required = false, description = "The profile IDs which need to be provisioned before this profile")
    protected List<String> dependentProfiles;

    @Argument(index = 0, required = true, description = "Profile ID")
    protected String profile;

    @Override
    protected boolean updateRequirements(FabricRequirements requirements) {
        ProfileRequirements requirement = new ProfileRequirements(profile);
        if (minimumInstances != null) {
            requirement.setMinimumInstances(minimumInstances);
        }
        if (maximumInstances != null) {
            requirement.setMaximumInstances(maximumInstances);
        }
        if (dependentProfiles != null) {
            requirement.setDependentProfiles(dependentProfiles);
        }
        requirements.addOrUpdateProfileRequirements(requirement);
        return true;
    }
}
