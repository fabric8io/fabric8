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
package io.fabric8.commands.support;

import io.fabric8.api.FabricRequirements;
import io.fabric8.boot.commands.support.FabricCommand;

/**
 */
public abstract class ChangeRequirementSupport extends FabricCommand {
    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        FabricRequirements requirements = fabricService.getRequirements();
        if (requirements == null) {
            requirements = new FabricRequirements();
        }
        if (updateRequirements(requirements)) {
            fabricService.setRequirements(requirements);
        }
        return null;
    }

    /**
     * Performs the updates on the requirements, returning true if the requirements are updated.
     */
    protected abstract boolean updateRequirements(FabricRequirements requirements);
}
