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

package io.fabric8.partition.internal;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.codehaus.jackson.map.ObjectMapper;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.partition.BalancingPolicy;
import io.fabric8.partition.WorkerNode;
import io.fabric8.zookeeper.ZkPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

@ThreadSafe
@Component(name = "io.fabric8.partition.balancing.even", description = "Fabric Partition Even Balancing Policy", immediate = true)
@Service(BalancingPolicy.class)
public final class EvenBalancingPolicy extends AbstractComponent implements BalancingPolicy {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvenBalancingPolicy.class);
    private static final String TYPE = "even";

    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

    private final ObjectMapper mapper = new ObjectMapper();

    public EvenBalancingPolicy() {
        mapper.registerSubtypes(WorkerNode.class);
    }

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public String getType() {
        assertValid();
        return TYPE;
    }

    /*
     * Only allow one thread to balance at a time
     */
    @Override
    public synchronized void rebalance(String workId, String[] items, String[] members) {
        assertValid();
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
                WorkerNode node = mapper.readValue(curator.get().getData().forPath(member), WorkerNode.class);
                Collection<String> assignedItems = distribution.get(member);

                if (assignedItems != null) {
                    node.setPartitions(assignedItems.toArray(new String[assignedItems.size()]));
                } else {
                    node.setPartitions(new String[0]);
                }
                String targetPath = ZkPath.TASK_MEMBER_PARTITIONS.getPath(node.getContainer(), workId);
                curator.get().setData().forPath(targetPath, mapper.writeValueAsBytes(node));
            } catch (Exception ex) {
                LOGGER.error("Error while assigning work", ex);
            }
        }
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }
}
