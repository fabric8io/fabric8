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
package io.fabric8.deployer.dto;

import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * Represents the project requirements and dependencies and how they should be mapped to a profile in fabric8
 */
public class ProjectRequirements {
    private String profileId;
    private String version;

    @Override
    public String toString() {
        return "ProjectRequirements{" + rootDependency + "}";
    }

    private DependencyDTO rootDependency;

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Returns the root maven dependency and all of its child dependency tree
     */
    public DependencyDTO getRootDependency() {
        return rootDependency;
    }

    public void setRootDependency(DependencyDTO rootDependency) {
        this.rootDependency = rootDependency;
    }

    @JsonIgnore
    public String getGroupId() {
        return rootDependency != null ? rootDependency.getGroupId() : null;
    }

    @JsonIgnore
    public String getArtifactId() {
        return rootDependency != null ? rootDependency.getArtifactId() : null;
    }
}
