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
package org.fusesource.fabric.api.jmx;

import java.util.Map;

import org.fusesource.fabric.api.FabricRequirements;
import org.fusesource.fabric.api.FabricStatus;
import org.fusesource.fabric.api.ProfileStatus;

/**
 * A DTO for easier marshalling across remote JMX
 */
public class FabricStatusDTO {
    private final Map<String, ProfileStatus> profileStatusMap;
    private final FabricRequirements requirements;

    public FabricStatusDTO(FabricStatus status) {
        this.profileStatusMap = status.getProfileStatusMap();
        this.requirements = status.getRequirements();
    }

    public Map<String, ProfileStatus> getProfileStatusMap() {
        return profileStatusMap;
    }

    public FabricRequirements getRequirements() {
        return requirements;
    }
}
