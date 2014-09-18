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
package io.fabric8.testkit;

import io.fabric8.api.FabricRequirements;
import io.fabric8.api.jmx.ContainerDTO;

import java.util.List;
import java.util.Map;

/**
 * Represents a (usually) remote API to working with the Fabric.
 */
public interface FabricController {
    FabricRequirements getRequirements();

    void setRequirements(FabricRequirements requirements) throws Exception;

    List<Map<String, Object>> containerProperties(String... properties);

    List<String> containerIdsForProfile(String versionId, String profileId);

    List<String> containerIds() throws Exception;

    List<ContainerDTO> containers(List<String> ids);

    List<ContainerDTO> containersForProfile(String version, String profileId);

    String getDefaultVersion();

    ContainerDTO getContainer(String containerId);

    List<ContainerDTO> containers() throws Exception;

}
