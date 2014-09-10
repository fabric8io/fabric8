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

package io.fabric8.internal.cluster;

import io.fabric8.api.Container;
import io.fabric8.internal.ImmutableContainerBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ZooKeeperClusterMember {

    private final Container container;
    private final Map<ZooKeeperPortType, Integer> ports;

    public ZooKeeperClusterMember(Container container, Map<ZooKeeperPortType, Integer> ports) {
        this.container = container;
        this.ports = Collections.unmodifiableMap(ports);
    }

    public static ZooKeeperClusterMember create(String containerId, String ip, String jmxUrl, int clientPort, int peerPort, int electionPort) {
        Container container =  new ImmutableContainerBuilder()
                .id(containerId)
                .ip(ip)
                .jmxUrl(jmxUrl)
                .build();

        return create(container, clientPort, peerPort, electionPort);
    }

    public static ZooKeeperClusterMember create(Container container, int clientPort, int peerPort, int electionPort) {
        Map<ZooKeeperPortType, Integer> p = new HashMap<>();
        p.put(ZooKeeperPortType.CLIENT, clientPort);
        p.put(ZooKeeperPortType.PEER, peerPort);
        p.put(ZooKeeperPortType.ELECTION, electionPort);
        return new ZooKeeperClusterMember(container, p);
    }

    public String getId() {
        return container.getId();
    }

    public Container getContainer() {
        return container;
    }

    public String getAddress() {
        return container.getIp();
    }

    public Map<ZooKeeperPortType, Integer> getPorts() {
        return ports;
    }

    public boolean isPortOwner(int port) {
        return ports.values().contains(port);
    }

    @Override
    public String toString() {
        return container.getId();
    }
}