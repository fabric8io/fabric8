/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.jgroups;


import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Filter;
import io.fabric8.utils.Strings;
import org.jgroups.Address;
import org.jgroups.Event;
import org.jgroups.Message;
import org.jgroups.PhysicalAddress;
import org.jgroups.annotations.MBean;
import org.jgroups.annotations.Property;
import org.jgroups.protocols.Discovery;
import org.jgroups.protocols.PingData;
import org.jgroups.protocols.PingHeader;
import org.jgroups.stack.IpAddress;
import org.jgroups.util.BoundedList;
import org.jgroups.util.Responses;
import org.jgroups.util.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;

@MBean(description = "Kubernetes discovery protocol")
public class KubernetesDiscovery extends Discovery {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesDiscovery.class);

    @Property
    private String address;

    private KubernetesClient client;
    private List<PhysicalAddress> kubernetesHosts = Collections.emptyList();
    private BoundedList<PhysicalAddress> dynamic_hosts = new BoundedList<>(2000);
    
    @Override
    public void init() throws Exception {
        super.init();
        if (!Strings.isNullOrBlank(address)) {
            client = new DefaultKubernetesClient(new ConfigBuilder().withMasterUrl(address).build());
        } else {
            client = new DefaultKubernetesClient();
        }
    }

    public Object down(Event evt) {
        Object retval = super.down(evt);
        switch (evt.getType()) {
            case Event.VIEW_CHANGE:
                for (Address logical_addr : members) {
                    PhysicalAddress physical_addr = (PhysicalAddress) down_prot.down(new Event(Event.GET_PHYSICAL_ADDRESS, logical_addr));
                    if (physical_addr != null && !kubernetesHosts.contains(physical_addr)) {
                        dynamic_hosts.addIfAbsent(physical_addr);
                    }
                }
                break;
            case Event.SET_PHYSICAL_ADDRESS:
                Tuple<Address, PhysicalAddress> tuple = (Tuple<Address, PhysicalAddress>) evt.getArg();
                PhysicalAddress physical_addr = tuple.getVal2();
                if (physical_addr != null && !kubernetesHosts.contains(physical_addr))
                    dynamic_hosts.addIfAbsent(physical_addr);
                break;
        }
        return retval;
    }

    public void discoveryRequestReceived(Address sender, String logical_name, PhysicalAddress physical_addr) {
        super.discoveryRequestReceived(sender, logical_name, physical_addr);
        if (physical_addr != null) {
            if (!kubernetesHosts.contains(physical_addr))
                dynamic_hosts.addIfAbsent(physical_addr);
        }
    }

    @Override
    public void findMembers(List<Address> members, boolean initial_discovery, Responses responses) {
        kubernetesHosts = findKubernetesHosts();
        
        PhysicalAddress physical_addr = (PhysicalAddress) down(new Event(Event.GET_PHYSICAL_ADDRESS, local_addr));
        // https://issues.jboss.org/browse/JGRP-1670
        PingData data = new PingData(local_addr, false, org.jgroups.util.UUID.get(local_addr), physical_addr);
        PingHeader hdr = new PingHeader(PingHeader.GET_MBRS_REQ).clusterName(cluster_name);

        Set<PhysicalAddress> cluster_members = new HashSet<>(kubernetesHosts);
        cluster_members.addAll(dynamic_hosts);

        if (use_disk_cache) {
            // this only makes sense if we have PDC below us
            Collection<PhysicalAddress> list = (Collection<PhysicalAddress>) down_prot.down(new Event(Event.GET_PHYSICAL_ADDRESSES));
            if (list != null)
                for (PhysicalAddress phys_addr : list)
                    if (!cluster_members.contains(phys_addr))
                        cluster_members.add(phys_addr);
        }

        for (final PhysicalAddress addr : cluster_members) {
            if (physical_addr != null && addr.equals(physical_addr)) // no need to send the request to myself
                continue;
            // the message needs to be DONT_BUNDLE, see explanation above
            final Message msg = new Message(addr).setFlag(Message.Flag.INTERNAL, Message.Flag.DONT_BUNDLE, Message.Flag.OOB)
                    .putHeader(this.id, hdr).setBuffer(marshal(data));
            log.trace("%s: sending discovery request to %s", local_addr, msg.getDest());
            down_prot.down(new Event(Event.MSG, msg));
        }
    }


    public List<PhysicalAddress> findKubernetesHosts() {
        List<PhysicalAddress> addresses = new ArrayList<>();
        Map<String, String> labels = Collections.singletonMap(Constants.JGROUPS_CLUSTER_NAME, cluster_name);

        for (Pod pod : client.pods().withLabels(labels).list().getItems()) {
            List<Container> containers = KubernetesHelper.getContainers(pod);
            for (Container container : containers) {

                for (ContainerPort port : container.getPorts()) {
                    if (Constants.JGROUPS_TCP_PORT.equals(port.getName())) {
                        try {
                            String ip = pod.getStatus().getPodIP();
                            if (ip != null) {
                                addresses.add(new IpAddress(ip, port.getContainerPort()));
                            }
                        } catch (Exception ex) {
                            LOGGER.warn("Failed to create Address {}.", pod.getStatus().getPodIP());
                        }
                    }
                }
            }
        }
        return addresses;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    private static List<Pod> filterPods(List<Pod> pods, Filter<Pod> podFilter) {
        List<Pod> result = new ArrayList<>();
        for (Pod pod : pods) {
            if (podFilter.matches(pod)) {
                result.add(pod);
            }
        }
        return result;
    }
}
