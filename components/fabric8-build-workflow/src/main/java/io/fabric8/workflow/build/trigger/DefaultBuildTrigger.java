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
package io.fabric8.workflow.build.trigger;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.builds.Builds;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.client.OpenShiftClient;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 */
public class DefaultBuildTrigger implements BuildTrigger {

    private final KubernetesClient kubernetes;

    public DefaultBuildTrigger() {
        this(new DefaultKubernetesClient());
    }

    public DefaultBuildTrigger(KubernetesClient kubernetes) {
        this.kubernetes = kubernetes;
    }

    @Override
    public String trigger(String namespace, final String buildName) {
        final BlockingQueue<String> uuid = new ArrayBlockingQueue<>(1);
        try (Watch watch = kubernetes.adapt(OpenShiftClient.class).builds().inNamespace(namespace).watch(new Watcher<Build>() {
            @Override
            public void eventReceived(Action action, Build build) {
                if (action == Action.ADDED && KubernetesHelper.getName(build).equals(buildName)) {
                    uuid.add(Builds.getUid(build));
                }
            }

            @Override
            public void onClose(KubernetesClientException e) {
                // ignore
            }
        })) {
            return uuid.poll(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw KubernetesClientException.launderThrowable(e);
        }
    }
}
