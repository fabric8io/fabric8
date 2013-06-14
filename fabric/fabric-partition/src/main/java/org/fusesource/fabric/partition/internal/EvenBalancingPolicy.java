/*
 * Copyright 2010 Red Hat, Inc.
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

package org.fusesource.fabric.partition.internal;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.apache.curator.framework.CuratorFramework;
import org.codehaus.jackson.map.ObjectMapper;
import org.fusesource.fabric.partition.BalancingPolicy;
import org.fusesource.fabric.partition.WorkerNode;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class EvenBalancingPolicy implements BalancingPolicy {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvenBalancingPolicy.class);
    private static final String TYPE = "even";
    private final ObjectMapper mapper = new ObjectMapper();

    private CuratorFramework curator;

    public EvenBalancingPolicy() {
        this.mapper.registerSubtypes(WorkerNode.class);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void rebalance(String workId, String[] items, String[] members) {
        Multimap<String, String> distribution = LinkedHashMultimap.create();
        //First pass - calculate the work distribution
        int index = 0;
        for (String item : items) {
            String path = members[index];
            distribution.put(path, item);
            index = (index + 1) % members.length;
        }
        //Second pass - assignment
        for (String member : members) {
            try {
                WorkerNode node = mapper.readValue(curator.getData().forPath(member), WorkerNode.class);
                Collection<String> assignedItems = distribution.get(member);

                if (assignedItems != null) {
                    node.setPartitions(assignedItems.toArray(new String[assignedItems.size()]));
                } else {
                    node.setPartitions(new String[0]);
                }
                String targetPath = ZkPath.TASK_MEMBER_PARTITIONS.getPath(node.getContainer(), workId);
                curator.setData().forPath(targetPath, mapper.writeValueAsBytes(node));
            } catch (Exception ex) {
                LOGGER.error("Error while assigning work", ex);
            }
        }
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }
}
