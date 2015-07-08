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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a snapshot of the entire system and how they relate to apps
 */
public class AppViewSnapshot {
    private final Map<NamespaceAndAppPath, AppViewDetails> appMap = new HashMap<>();
    private final List<AppViewDetails> apps = new ArrayList<>();
    private final Map<String, Service> servicesMap;
    private final Map<String, ReplicationController> controllerMap;
    private final Map<String, Pod> podMap;

    public AppViewSnapshot(Map<String, Service> servicesMap, Map<String, ReplicationController> controllerMap, Map<String, Pod> podMap) {
        this.servicesMap = servicesMap;
        this.controllerMap = controllerMap;
        this.podMap = podMap;
    }


    public AppViewDetails getOrCreateAppView(String appPath, String namespace) {
        NamespaceAndAppPath key = new NamespaceAndAppPath(namespace, appPath);
        AppViewDetails answer = appMap.get(key);
        if (answer == null) {
            answer = new AppViewDetails(this, appPath, namespace);
            appMap.put(key, answer);
            apps.add(answer);
        }
        return answer;
    }

    public List<AppViewDetails> getApps() {
        return apps;
    }

    public AppViewDetails createApp(String namespace) {
        AppViewDetails answer = new AppViewDetails(this, null, namespace);
        apps.add(answer);
        return answer;
    }

    public Map<NamespaceAndAppPath, AppViewDetails> getAppMap() {
        return appMap;
    }

    public Map<String, ReplicationController> getControllerMap() {
        return controllerMap;
    }

    public Map<String, Pod> getPodMap() {
        return podMap;
    }

    public Map<String, Service> getServicesMap() {
        return servicesMap;
    }

    public List<Pod> podsForReplicationController(ReplicationController controller) {
        return KubernetesHelper.getPodsForReplicationController(controller, podMap.values());
    }
}
