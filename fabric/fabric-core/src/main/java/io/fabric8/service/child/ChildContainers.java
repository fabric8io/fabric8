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
package io.fabric8.service.child;

import io.fabric8.api.CreateChildContainerOptions;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profiles;

import java.util.Map;
import java.util.Set;

/**
 * Helper methods for detecting the kinds of child containers based on the profiles
 */
public class ChildContainers {

    public static boolean isJavaContainer(FabricService fabricService, CreateChildContainerOptions options) {
        Map<String, ?> javaContainerConfig = Profiles.getOverlayConfiguration(fabricService, options.getProfiles(), options.getVersion(), ChildConstants.JAVA_CONTAINER_PID);
        return !javaContainerConfig.isEmpty();
    }

    public static boolean isProcessContainer(FabricService fabricService, CreateChildContainerOptions options) {
        Set<String> profileIds = options.getProfiles();
        String versionId = options.getVersion();
        Map<String, ?> processConfig = Profiles.getOverlayConfiguration(fabricService, profileIds, versionId, ChildConstants.PROCESS_CONTAINER_PID);
        return processConfig != null;
    }
}
