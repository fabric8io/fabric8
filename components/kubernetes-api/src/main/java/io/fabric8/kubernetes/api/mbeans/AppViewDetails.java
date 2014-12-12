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
package io.fabric8.kubernetes.api.mbeans;

import io.fabric8.kubernetes.api.model.PodSchema;
import io.fabric8.kubernetes.api.model.ReplicationControllerSchema;
import io.fabric8.kubernetes.api.model.ServiceSchema;
import io.fabric8.utils.Strings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the App View of a single application
 */
public class AppViewDetails {
    private final AppViewSnapshot snapshot;
    private final String appPath;
    private final Map<String, ServiceSchema> services = new HashMap<>();
    private final Map<String, ReplicationControllerSchema> controllers = new HashMap<>();
    private final Map<String, PodSchema> pods = new HashMap<>();

    public AppViewDetails(AppViewSnapshot snapshot, String appPath) {
        this.snapshot = snapshot;
        this.appPath = appPath;
    }

    public AppViewSnapshot getSnapshot() {
        return snapshot;
    }

    public String getAppPath() {
        return appPath;
    }

    public Map<String, ServiceSchema> getServices() {
        return services;
    }

    public Map<String, ReplicationControllerSchema> getControllers() {
        return controllers;
    }

    public Map<String, PodSchema> getPods() {
        return pods;
    }

    public void addService(ServiceSchema service) {
        String id = service.getId();
        if (Strings.isNotBlank(id)) {
            services.put(id, service);
        }
    }

    public void addController(ReplicationControllerSchema controller) {
        String id = controller.getId();
        if (Strings.isNotBlank(id)) {
            controllers.put(id, controller);


            // now lets find all the pods that are active for this
            List<PodSchema> pods = snapshot.podsForReplicationController(controller);
            for (PodSchema pod : pods) {
                addPod(pod);
            }
        }
    }

    public void addPod(PodSchema pod) {
        String id = pod.getId();
        if (Strings.isNotBlank(id)) {
            pods.put(id, pod);
        }
    }

    public AppSummaryDTO getSummary() {
        AppSummaryDTO answer = new AppSummaryDTO(appPath);
        for (ServiceSchema service : getServices().values()) {
            answer.addServiceSummary(new AppServiceSummaryDTO(service));
        }
        for (ReplicationControllerSchema controller : getControllers().values()) {
            answer.addReplicationControllerSummary(new AppReplicationControllerSummaryDTO(controller));
        }
        for (PodSchema pod : getPods().values()) {
            answer.addPodSummary(new AppPodSummaryDTO(pod));
        }
        return answer;
    }
}
