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
package io.fabric8.deployer.dto;

import io.fabric8.api.Profile;

/**
 */
public class DeployResults {
    private String profileUrl;
    private String profileId;
    private String versionId;

    public DeployResults() {
    }

    public DeployResults(Profile profile, String profileUrl) {
        this.profileUrl = profileUrl;
        this.profileId = profile.getId();
        this.versionId = profile.getVersion();
    }

    @Override
    public String toString() {
        return "DeployResults{" +
                "profileUrl='" + profileUrl + '\'' +
                ", profileId='" + profileId + '\'' +
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

    public String getProfileUrl() {
        return profileUrl;
    }

    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }
}
