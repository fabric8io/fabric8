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

import io.fabric8.api.Profile;

/**
 */
public class DeployResults {
    private String profileId;
    private String versionId;

    public DeployResults() {
    }

    public DeployResults(Profile profile) {
        this.profileId = profile.getId();
        this.versionId = profile.getVersion();
    }

    @Override
    public String toString() {
        return "DeployResults{" +
                "profileId='" + profileId + '\'' +
                ", versionId='" + versionId + '\'' +
                '}';
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }
}
