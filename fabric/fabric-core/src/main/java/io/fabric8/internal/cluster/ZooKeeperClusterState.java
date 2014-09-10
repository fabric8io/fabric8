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
import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.common.util.Strings;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.RetryNTimes;

import java.io.Closeable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static io.fabric8.internal.cluster.Constants.*;

/**
 * A representation of the ZooKeeper cluster state.
 */
public class ZooKeeperClusterState implements Closeable {


    private final int clusterId;
    private final Map<Integer, ZooKeeperClusterMember> members;
    private final Map<String, String> configuration;
    private final ACLProvider aclProvider;
    private final CreateEnsembleOptions createEnsembleOptions;
    private CuratorFramework curatorFramework;


    public ZooKeeperClusterState(int clusterId, Map<Integer, ZooKeeperClusterMember> members, Map<String, String> configuration, ACLProvider aclProvider, CreateEnsembleOptions createEnsembleOptions) {
        this.clusterId = clusterId;
        this.members = members;
        this.aclProvider = aclProvider;
        this.createEnsembleOptions = createEnsembleOptions;
        this.configuration = Collections.unmodifiableMap(configuration);
    }

    public int getClusterId() {
        return clusterId;
    }

    public Map<Integer, ZooKeeperClusterMember> getMembers() {
        return Collections.unmodifiableMap(members);
    }

    public Set<Container> getContainers() {
        Set<Container> containers = new HashSet<>();
        for (ZooKeeperClusterMember member : getMembers().values()) {
            containers.add(member.getContainer());
       }
        return Collections.unmodifiableSet(containers);
    }

    public ZooKeeperClusterState updateConfiguration(Map<String, String> configuration) {
        return new ZooKeeperClusterState(clusterId, members, configuration, aclProvider, createEnsembleOptions);
    }

    /**
     * Returns the membershipId of the specified container or 0 if not found.
     *
     * @param container The target container.
     * @return
     */
    public int getContainerMembershipId(Container container) {
        return getContainerMembershipId(container.getId());
    }

    /**
     * Returns the membershipId of the specified container or 0 if not found.
     *
     * @param containerId The target container id.
     * @return
     */
    public int getContainerMembershipId(String containerId) {
        for (Map.Entry<Integer, ZooKeeperClusterMember> entry : members.entrySet()) {
            int membershipId = entry.getKey();
            ZooKeeperClusterMember member = entry.getValue();
            if (containerId.equals(member.getId())) {
                return membershipId;
            }
        }
        return 0;
    }

    /**
     * Returns the ZooKeeper Connection URL of the ensemble.
     * @param withSubstitution  Flag to specify if result substitute placeholders.
     * @return                  The connection url.
     */
    public String getConnectionUrl(boolean withSubstitution) {
        List<String> sockets = new LinkedList<>();
        for (ZooKeeperClusterMember member : members.values()) {
            String id = member.getId();
            String address = member.getAddress();
            int port = member.getPorts().get(ZooKeeperPortType.CLIENT);

            sockets.add(String.format(withSubstitution ?
                    String.format(CLIENT_ADDRESS_FORMAT, address, port) :
                    String.format(CLIENT_ADDRESS_FORMAT_PL, id, port)
            ));
        }
        return Strings.join(sockets, ",");
    }

    public Map<String, String>  getConfiguration() {
        return configuration;
    }

    /**
     * Returns the cluster configuration. The returned map actually contains the common properties for all servers.
     * The resulting map is an aggregation of the specified properties, with the server.x information for all servers.
     * @return
     */
    public Map<String, String> getClusterConfiguration() {
        Map<String, String> result = new HashMap<>(configuration);
        result.put("ensembleId", String.format("%04d", clusterId));
        if (members.size() > 1) {
            for (Map.Entry<Integer, ZooKeeperClusterMember> entry : members.entrySet()) {
                Integer membershipId = entry.getKey();
                ZooKeeperClusterMember member = entry.getValue();
                result.put("server." + membershipId, String.format(SERVER_ADDRESS_FORMAT, member.getAddress(),
                        member.getPorts().get(ZooKeeperPortType.PEER),
                        member.getPorts().get(ZooKeeperPortType.ELECTION)

                ));
            }
        }
        return result;
    }

    /**
     * Returns the member configuration.
     * @return
     */
    public Map<String, String> getMemberConfiguration(String id) {
        Map<String, String> result = new HashMap<>(configuration);
        for (Map.Entry<Integer, ZooKeeperClusterMember> entry : members.entrySet()) {
            Integer membershipId = entry.getKey();
            ZooKeeperClusterMember member = entry.getValue();
            if (member.getId().equals(id)) {
                result.put(CLIENT_PORT, String.valueOf(member.getPorts().get(ZooKeeperPortType.CLIENT)));
                result.put(CLIENT_PORT_ADDRESS, String.format(SERVER_BIND_ADDRESS_FORMAT, id));
                if (members.size() > 1) {
                    result.put("server.id", String.valueOf(membershipId));
                }
            }
        }
        return result;
    }

    public ZooKeeperClusterState addMember(Container container) {
        Map<ZooKeeperPortType, Integer> ports = new HashMap<>();
        for (ZooKeeperPortType type : ZooKeeperPortType.values()) {
            ports.put(type, allocatePort(container.getIp(), type));
        }

        int membershipId = allocateMembershipId();
        ZooKeeperClusterMember member = new ZooKeeperClusterMember(container, ports);
        return addMember(member, membershipId);
    }

    public ZooKeeperClusterState addMember(ZooKeeperClusterMember member, int membershipId) {
        if (membershipId <= 0) {
            throw new IllegalArgumentException("Membership id must be greater than 0.");
        }
        Map<Integer, ZooKeeperClusterMember> newMembers = new HashMap<>(members);
        newMembers.put(membershipId, member);
        return new ZooKeeperClusterState(clusterId, newMembers, configuration, aclProvider, createEnsembleOptions);
    }

    public ZooKeeperClusterState removeMember(Container container) {
        return removeMember(container.getId());
    }

    public ZooKeeperClusterState removeMember(String id) {
        int membershipId = findMembershipId(id);
        Map<Integer, ZooKeeperClusterMember> newMembers = new HashMap<>(members);
        newMembers.remove(membershipId);
        //Just removing the members, may leave gaps to the numbering.
        //We want to keep those gaps, as its the only way we can avoid server.id conflicts in rolling remove.
        return new ZooKeeperClusterState(clusterId, newMembers,configuration, aclProvider, createEnsembleOptions);
    }

    public ZooKeeperClusterState removeMembershipId(int membershipId) {
        Map<Integer, ZooKeeperClusterMember> newMembers = new HashMap<>(members);
        newMembers.remove(membershipId);
        int index = membershipId + 1;
        while (newMembers.containsKey(index)) {
            ZooKeeperClusterMember m = newMembers.remove(index);
            newMembers.put(index - 1, m);
            index++;
        }
        return new ZooKeeperClusterState(clusterId, newMembers,configuration, aclProvider, createEnsembleOptions);
    }

    public ZooKeeperClusterState newCluster() {
        return new ZooKeeperClusterState(clusterId + 1, members, configuration, aclProvider, createEnsembleOptions);
    }

    public CreateEnsembleOptions getCreateEnsembleOptions() {
        return createEnsembleOptions;
    }

    public synchronized CuratorFramework getCuratorFramework() {
        if (curatorFramework == null || curatorFramework.getState() == CuratorFrameworkState.STOPPED) {
            curatorFramework = CuratorFrameworkFactory.builder().connectString(getConnectionUrl(true)).retryPolicy(new RetryNTimes(10, 3000))
                    .canBeReadOnly(false)
                    .aclProvider(aclProvider).authorization("digest", ("fabric:" + createEnsembleOptions.getZookeeperPassword()).getBytes())
                    .build();
            curatorFramework.start();
        }
        return curatorFramework;
    }


    private int findMembershipId(String id) {
        for (Map.Entry<Integer, ZooKeeperClusterMember> entry : members.entrySet()) {
            int membershipId = entry.getKey();
            ZooKeeperClusterMember member = entry.getValue();
            if (member.getId().equals(id)) {
                return membershipId;
            }
        }
        throw new IllegalArgumentException("Member with id:" + id + " not found");
    }


    private int allocateMembershipId() {
        int result = 1;
        while (members.containsKey(result)) {
            result++;
        }
        return result;
    }

    private int allocatePort(String ip, ZooKeeperPortType type) {
        int result = type.getValue();
        while (!isPortAvailable(ip, result)) {
            result++;
        }
        return result;
    }

    private boolean isPortAvailable(String ip, int port) {
        for (ZooKeeperClusterMember member : members.values()) {
            if (member.getAddress().equals(ip) && member.isPortOwner(port)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public synchronized void close() {
        if (curatorFramework != null) {
            curatorFramework.close();
        }
    }
}