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
package io.fabric8.workflow.build.simulator;

import io.fabric8.workflow.build.BuildCorrelationKey;
import io.fabric8.kubernetes.api.builds.BuildFinishedEvent;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.BuildStatus;

import java.util.Date;

/**
 */
public class SimulatorBuildFinishedEvent extends BuildFinishedEvent {
    private final BuildCorrelationKey key;

    public SimulatorBuildFinishedEvent(BuildCorrelationKey key, String buildLink) {
        super(key.getBuildUuid(), createBuild(key), false, buildLink);
        this.key = key;
    }

    @Override
    public String getConfigName() {
        return key.getBuildName();
    }

    protected static Build createBuild(BuildCorrelationKey key) {
        Build build = new Build();
        build.setMetadata(new ObjectMeta());
        build.getMetadata().setName(key.getBuildName());
        build.getMetadata().setNamespace(key.getNamespace());
        build.getMetadata().setUid(key.getBuildUuid());
        BuildStatus status = new BuildStatus();
        status.setCompletionTimestamp(new Date().toString());
        build.setStatus(status);
        return build;
    }
}
