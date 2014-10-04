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
package io.fabric8.kubernetes.provider;

import io.fabric8.api.CreateContainerBasicMetadata;

import javax.management.AttributeList;
import java.util.ArrayList;
import java.util.List;

public class CreateKubernetesContainerMetadata extends CreateContainerBasicMetadata<CreateKubernetesContainerOptions> {
    private final String id;
    private final List<String> warnings;
    private String jolokiaUrl;
    private List<String> podIds = new ArrayList<>();
    private List<String> replicationControllerIds = new ArrayList<>();
    private List<String> serviceIds = new ArrayList<>();

    public CreateKubernetesContainerMetadata(String id, List<String> warnings) {
        this.id = id;
        this.warnings = warnings;
    }

    public String getId() {
        return id;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public String getJolokiaUrl() {
        return jolokiaUrl;
    }

    public void setJolokiaUrl(String jolokiaUrl) {
        this.jolokiaUrl = jolokiaUrl;
    }

    public List<String> getPodIds() {
        return podIds;
    }

    public void setPodIds(List<String> podIds) {
        this.podIds = podIds;
    }

    public List<String> getReplicationControllerIds() {
        return replicationControllerIds;
    }

    public void setReplicationControllerIds(List<String> replicationControllerIds) {
        this.replicationControllerIds = replicationControllerIds;
    }

    public List<String> getServiceIds() {
        return serviceIds;
    }

    public void setServiceIds(List<String> serviceIds) {
        this.serviceIds = serviceIds;
    }

    /**
     * Returns true if this container represents a kubelet; namely it creates/manages at least one pod, replicationController or service
     */
    public boolean isKubelet() {
        return podIds.size() > 0 || replicationControllerIds.size() > 0 || serviceIds.size() > 0;
    }

    public boolean addPodId(String id) {
        return addIfNotContained(podIds, id);
    }

    public boolean addReplicationControllerId(String id) {
        return addIfNotContained(replicationControllerIds, id);
    }

    public boolean addServiceId(String id) {
        return addIfNotContained(serviceIds, id);
    }

    protected static boolean addIfNotContained(List<String> list, String id) {
        if (list.contains(id)) {
            return false;
        } else {
            list.add(id);
            return true;
        }
    }
}


