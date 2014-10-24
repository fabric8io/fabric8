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
import io.fabric8.api.EnsembleModificationFailed;
import io.fabric8.common.util.Strings;
import io.fabric8.utils.PasswordEncoder;
import io.fabric8.zookeeper.ZkPath;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.fabric8.internal.cluster.Constants.ENSEMBLE_ID_FORMAT;
import static io.fabric8.internal.cluster.Constants.MEMBER_PROFILE_FORMAT;
import static io.fabric8.internal.cluster.ZooKeeperClusterUtils.applyClusterProfiles;
import static io.fabric8.internal.cluster.ZooKeeperClusterUtils.updateEnsembleUrl;
import static io.fabric8.internal.cluster.ZooKeeperClusterUtils.waitForContainersToSwitch;
import static io.fabric8.internal.cluster.ZooKeeperClusterUtils.waitForZooKeeperServer;
import static io.fabric8.internal.cluster.ZooKeeperClusterUtils.setDataAtomic;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.copy;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getStringData;

public class DowngradeToSingleServerOperation implements ZooKeeperClusterOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(DowngradeToSingleServerOperation.class);

    public void execute(ZooKeeperClusterOperationContext ctx) throws Exception{
        ZooKeeperClusterState intermediateState = new ZooKeeperClusterState(ctx.getTargetState().getClusterId(),
                ctx.getCurrentState().getMembers(),
                ctx.getTargetState().getConfiguration(), ctx.getAclProvider(), ctx.getCreateEnsembleOptions());

        try {
            List<Container> clusterContainers = new LinkedList<>();
            clusterContainers.addAll(ctx.getCurrentState().getContainers());
            clusterContainers.removeAll(ctx.getContainersToRemove());

            for (Container container : clusterContainers) {
                intermediateState = intermediateState.removeMember(container);
            }

            updateEnsembleUrl(ctx.getCurrentState().getCuratorFramework(), intermediateState.getConnectionUrl(false), ctx.getCreateEnsembleOptions().getZookeeperPassword());
            waitForContainersToSwitch(ctx.getAllContainers().values(), intermediateState);

            for (Container container : clusterContainers) {
                applyClusterProfiles(ctx.getCurrentState(), container, ctx.getTargetState());
                waitForZooKeeperServer(container, ctx.getTargetState(), ctx.getUsersname(), ctx.getPassword(), 30000);
            }

            //Merge clusters without copying
            fromIntermediate(ctx.getCurrentState(), intermediateState, ctx.getTargetState());
        } finally {
            intermediateState.close();
        }
    }

    /**
     * Copy data from the intermediate ensemble to the target ensemble. Create all required entries to the intermediate ensemble.
     * The method is applicable for removing containers from an existing ensemble that will cause a quorum loss.
     *
     * @param from         The state object describing the existing ensemble.
     * @param intermediate The state object describing the intermediate ensemble.
     * @param to           The state obhect describing the target ensemble
     */
    private static void fromIntermediate(ZooKeeperClusterState from, ZooKeeperClusterState intermediate, ZooKeeperClusterState to) {
        CuratorFramework source = intermediate.getCuratorFramework();
        CuratorFramework target = to.getCuratorFramework();
        Set<String> nodesToCreate = new LinkedHashSet<>();
        Map<String, String> nodesToSet = new HashMap<>();
        String ensemblePath = ZkPath.CONFIG_ENSEMBLE.getPath(String.format(ENSEMBLE_ID_FORMAT, to.getClusterId()));
        try {
            source.getZookeeperClient().blockUntilConnectedOrTimedOut();
            target.getZookeeperClient().blockUntilConnectedOrTimedOut();

            copy(source, target, "/fabric");

            LOGGER.info("Configuring ensemble member profiles for: " + to.getClusterId());
            nodesToCreate.add(ensemblePath);
            nodesToSet.put(ZkPath.CONFIG_ENSEMBLE_PASSWORD.getPath(), PasswordEncoder.encode(to.getCreateEnsembleOptions().getZookeeperPassword()));
            nodesToSet.put(ZkPath.CONFIG_ENSEMBLE_URL.getPath(), to.getConnectionUrl(false));
            nodesToSet.put(ensemblePath, Strings.join(to.getMembers().values(), ","));
            nodesToSet.put(ZkPath.CONFIG_ENSEMBLES.getPath(), String.format(ENSEMBLE_ID_FORMAT, to.getClusterId()));

            //Remove old and add new profiles from new cluster members.
            for (Map.Entry<Integer, ZooKeeperClusterMember> entry : to.getMembers().entrySet()) {
                int membershipId = entry.getKey();
                ZooKeeperClusterMember member = entry.getValue();
                String versionId = getStringData(target, ZkPath.CONFIG_CONTAINER.getPath(member.getId()));

                Set<String> profiles = new HashSet<>(Strings.splitAndTrimAsList(getStringData(target, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(versionId, member.getId())), " "));
                profiles.add(String.format(MEMBER_PROFILE_FORMAT, to.getClusterId(), membershipId));
                int oldMembershipId = from.getContainerMembershipId(member.getId());
                if (oldMembershipId > 0) {
                    profiles.remove(String.format(MEMBER_PROFILE_FORMAT, from.getClusterId(), oldMembershipId));
                }
                nodesToSet.put(ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(versionId, member.getId()), Strings.join(profiles, " "));
            }

            LOGGER.info("Migrating containers to the new ensemble using url {}.", to.getConnectionUrl(true));
            setDataAtomic(target, nodesToCreate, nodesToSet);
            setDataAtomic(source, nodesToCreate, nodesToSet);
        } catch (Exception ex) {
            throw new EnsembleModificationFailed("Failed to migrate data.", ex, EnsembleModificationFailed.Reason.UNKNOWN);
        }
    }
}
