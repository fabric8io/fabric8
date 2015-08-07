/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.workflow.build.dto;

import io.fabric8.kubernetes.api.builds.BuildFinishedEvent;

/**
 * Represents a DTO for a build which is finished with a status and link to the build.
 */
public class BuildFinishedDTO {
    private final String namespace;
    private final String buildName;
    private final String buildUuid;
    private final String status;
    private final String buildLink;

    public BuildFinishedDTO(BuildFinishedEvent event) {
        this(event.getNamespace(), event.getConfigName(), event.getUid(), event.getStatus(), event.getBuildLink());
    }

    public BuildFinishedDTO(String namespace, String buildName, String buildUuid, String status, String buildLink) {
        this.namespace = namespace;
        this.buildName = buildName;
        this.buildUuid = buildUuid;
        this.status = status;
        this.buildLink = buildLink;
    }

    @Override
    public String toString() {
        return "BuildFinishedDTO{" +
                "buildLink='" + buildLink + '\'' +
                ", namespace='" + namespace + '\'' +
                ", buildName='" + buildName + '\'' +
                ", buildUuid='" + buildUuid + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

    public String getBuildLink() {
        return buildLink;
    }

    public String getBuildName() {
        return buildName;
    }

    public String getBuildUuid() {
        return buildUuid;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getStatus() {
        return status;
    }
}
