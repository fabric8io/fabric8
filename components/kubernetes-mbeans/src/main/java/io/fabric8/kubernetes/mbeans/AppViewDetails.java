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
package io.fabric8.kubernetes.mbeans;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.utils.Strings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.fabric8.kubernetes.api.KubernetesHelper.getId;

/**
 * Represents the App View of a single application
 */
public class AppViewDetails {
    private final AppViewSnapshot snapshot;
    private final String appPath;
    private final String namespace;
    private final Map<String, Service> services = new HashMap<>();
    private final Map<String, ReplicationController> controllers = new HashMap<>();
    private final Map<String, Pod> pods = new HashMap<>();

    public AppViewDetails(AppViewSnapshot snapshot, String appPath, String namespace) {
        this.snapshot = snapshot;
        this.appPath = appPath;
        this.namespace = namespace;
    }

    public AppViewSnapshot getSnapshot() {
        return snapshot;
    }

    public String getAppPath() {
        return appPath;
    }

    public String getNamespace() {
        return namespace;
    }

    public Map<String, Service> getServices() {
        return services;
    }

    public Map<String, ReplicationController> getControllers() {
        return controllers;
    }

    public Map<String, Pod> getPods() {
        return pods;
    }

    public void addService(Service service) {
        String id = KubernetesHelper.getName(service);
        if (Strings.isNotBlank(id)) {
            services.put(id, service);
        }
    }

    public void addController(ReplicationController controller) {
        String id = KubernetesHelper.getName(controller);
        if (Strings.isNotBlank(id)) {
            controllers.put(id, controller);


            // now lets find all the pods that are active for this
            List<Pod> pods = snapshot.podsForReplicationController(controller);
            for (Pod pod : pods) {
                addPod(pod);
            }
        }
    }

    public void addPod(Pod pod) {
        String id = KubernetesHelper.getName(pod);
        if (Strings.isNotBlank(id)) {
            pods.put(id, pod);
        }
    }

    public AppSummaryDTO getSummary() {
        AppSummaryDTO answer = new AppSummaryDTO(appPath, namespace);
        for (Service service : getServices().values()) {
            answer.addServiceSummary(new AppServiceSummaryDTO(service));
        }
        for (ReplicationController controller : getControllers().values()) {
            answer.addReplicationControllerSummary(new AppReplicationControllerSummaryDTO(controller));
        }
        for (Pod pod : getPods().values()) {
            answer.addPodSummary(new AppPodSummaryDTO(pod));
        }
        return answer;
    }
}
