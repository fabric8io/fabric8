/*
 * Copyright 2005-2014 Red Hat, Inc.                                    
 *                                                                      
 * Red Hat licenses this file to you under the Apache License, version  
 * 2.0 (the "License"); you may not use this file except in compliance  
 * with the License.  You may obtain a copy of the License at           
 *                                                                      
 *    http://www.apache.org/licenses/LICENSE-2.0                        
 *                                                                      
 * Unless required by applicable law or agreed to in writing, software  
 * distributed under the License is distributed on an "AS IS" BASIS,    
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or      
 * implied.  See the License for the specific language governing        
 * permissions and limitations under the License.
 */

package io.fabric8.jgroups;

import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.KubernetesFactory;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Port;
import io.fabric8.utils.Filter;
import org.jgroups.PhysicalAddress;
import org.jgroups.annotations.MBean;
import org.jgroups.annotations.Property;
import org.jgroups.protocols.Discovery;
import org.jgroups.stack.IpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@MBean(description = "Kubernetes discovery protocol")
public class KubernetesDiscovery extends Discovery {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesDiscovery.class);

    @Property
    private String address;
    
    
    private KubernetesClient client;

    @Override
    public void start() throws Exception {
        client = new KubernetesClient(new KubernetesFactory(address));
    }

    
    @Override
    public Collection<PhysicalAddress> fetchClusterMembers(String cluster_name) {
        List<PhysicalAddress> result = new ArrayList<>();
        Map<String, String> labels = Collections.singletonMap(Constants.JGROUPS_CLUSTER_NAME, cluster_name);
        Filter<Pod> podFilter = KubernetesHelper.createPodFilter(labels);
        List<Pod> podList = filterPods(client.getPods().getItems(), podFilter);
        for (Pod pod : podList) {
            if (podFilter.matches(pod)) {
                List<Container> containers = KubernetesHelper.getContainers(pod);
                for (Container container : containers) {

                    for (Port port : container.getPorts()) {
                        if (Constants.JGROUPS_TCP_PORT.equals(port.getName())) {
                            try {
                                IpAddress address = new IpAddress(pod.getCurrentState().getPodIP(), port.getContainerPort());
                                result.add(address);
                            } catch (Exception ex) {
                                LOGGER.warn("Failed to create Address {}.", pod.getCurrentState().getPodIP());
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public boolean sendDiscoveryRequestsInParallel() {
        return false;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    private static List<Pod> filterPods(List<Pod> pods, Filter<Pod> podFilter) {
        List<Pod> result = new ArrayList<>();
        for (Pod pod: pods) {
            if (podFilter.matches(pod)) {
                result.add(pod);
            }
        }
        return result;
    }
}
