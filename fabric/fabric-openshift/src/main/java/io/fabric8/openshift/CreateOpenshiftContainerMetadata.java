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
package io.fabric8.openshift;

import io.fabric8.api.CreateContainerBasicMetadata;

public class CreateOpenshiftContainerMetadata extends CreateContainerBasicMetadata<CreateOpenshiftContainerOptions> {

    private final String OPENSHIFT_RESOLVER_OVERRIDE = "publichostname";

    private final String domainId;
    private final String uuid;
    private final String createLog;
    private final String gitUrl;

    public CreateOpenshiftContainerMetadata(String domainId, String uuid, String createLog, String gitUrl) {
        this.domainId = domainId;
        this.uuid = uuid;
        this.createLog = createLog;
        this.gitUrl = gitUrl;
    }

    @Override
    public String getOverridenResolver() {
        return OPENSHIFT_RESOLVER_OVERRIDE;
    }

    public String getDomainId() {
        return domainId;
    }

    public String getUuid() {
        return uuid;
    }

    public String getCreateLog() {
        return createLog;
    }

    public String getGitUrl() {
        return gitUrl;
    }
}
