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
import io.fabric8.api.FabricRequirements;
import io.fabric8.commands.support.ChangeRequirementSupport;

@Command(name = "require-profile-delete", scope = "fabric", description = "Deletes the requirements for a profile", detailedDescription = "classpath:status.txt")
public class RequireProfileDelete extends ChangeRequirementSupport {

    @Argument(index = 0, required = true, description = "Profile ID")
    protected String profile;

    @Override
    protected boolean updateRequirements(FabricRequirements requirements) {
        requirements.removeProfileRequirements(profile);
        return true;
    }
}
