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
package io.fabric8.io.fabric8.workflow.build.trigger;

import io.fabric8.kubernetes.api.KubernetesClient;

/**
 */
public class DefaultBuildTrigger implements BuildTrigger {
    private final KubernetesClient kubernetes;

    public DefaultBuildTrigger() {
        this(new KubernetesClient());
    }

    public DefaultBuildTrigger(KubernetesClient kubernetes) {
        this.kubernetes = kubernetes;
    }

    @Override
    public String trigger(String namespace, String buildName) {
        return kubernetes.triggerBuildAndGetUuid(buildName, namespace);
    }
}
