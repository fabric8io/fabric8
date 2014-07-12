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

/**
 * Represents a command request to auto-scale
 */
public class AutoScaleRequest {
    private final FabricService fabricService;
    private final String version;
    private final String profile;
    private final int delta;
    private final FabricRequirements fabricRequirements;
    private final ProfileRequirements profileRequirements;

    public AutoScaleRequest(FabricService fabricService, String version, String profile, int delta, FabricRequirements fabricRequirements, ProfileRequirements profileRequirements) {
        this.fabricService = fabricService;
        this.version = version;
        this.profile = profile;
        this.delta = delta;
        this.fabricRequirements = fabricRequirements;
        this.profileRequirements = profileRequirements;
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    public String getVersion() {
        return version;
    }

    public String getProfile() {
        return profile;
    }

    public int getDelta() {
        return delta;
    }

    public FabricRequirements getFabricRequirements() {
        return fabricRequirements;
    }

    public ProfileRequirements getProfileRequirements() {
        return profileRequirements;
    }
}
