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

import static io.fabric8.internal.cluster.ZooKeeperClusterUtils.applyClusterProfiles;
import static io.fabric8.internal.cluster.ZooKeeperClusterUtils.createEnsembleEntries;
import static io.fabric8.internal.cluster.ZooKeeperClusterUtils.updateEnsembleUrl;
import static io.fabric8.internal.cluster.ZooKeeperClusterUtils.waitForZooKeeperServer;
public class RollingAddOperation  implements ZooKeeperClusterOperation {

    public void execute(ZooKeeperClusterOperationContext ctx) throws Exception{
        //1. Apply profiles to the "to be added" containers.
        for (Container container : ctx.getContainersToAdd()) {
            applyClusterProfiles(ctx.getCurrentState(), container, ctx.getTargetState());
            waitForZooKeeperServer(container, ctx.getTargetState(), ctx.getUsersname(), ctx.getPassword(), 30000);
        }

        //2. Rolling Migrate existing containers
        for (ZooKeeperClusterMember member : ctx.getTargetState().getMembers().values()) {
            Container container = member.getContainer();
            if (!ctx.getContainersToAdd().contains(container)) {
                applyClusterProfiles(ctx.getCurrentState(), container, ctx.getTargetState());
                waitForZooKeeperServer(container, ctx.getTargetState(), ctx.getUsersname(), ctx.getPassword(), 30000);
            }
        }

        //7. Update the URL.
        updateEnsembleUrl(ctx.getCurrentState().getCuratorFramework(), ctx.getTargetState().getConnectionUrl(false), ctx.getCreateEnsembleOptions().getZookeeperPassword());
        createEnsembleEntries(ctx.getCurrentState().getCuratorFramework(), ctx.getTargetState());
    }
}
