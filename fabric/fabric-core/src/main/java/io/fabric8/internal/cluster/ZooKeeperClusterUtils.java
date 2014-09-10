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
import io.fabric8.service.ContainerTemplate;
import io.fabric8.service.JmxTemplateSupport;
import io.fabric8.utils.PasswordEncoder;
import io.fabric8.zookeeper.ZkPath;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.transaction.CuratorTransaction;
import org.apache.curator.framework.api.transaction.CuratorTransactionFinal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.fabric8.internal.cluster.Constants.CLUSTER_PROFILE_PREFIX;
import static io.fabric8.internal.cluster.Constants.ENSEMBLE_ID_FORMAT;
import static io.fabric8.internal.cluster.Constants.MEMBER_PROFILE_FORMAT;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.exists;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getStringData;

public class ZooKeeperClusterUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZooKeeperClusterUtils.class);

    /**
     * Wait until the specified containers appear as "alive" in the ensemble defined by the specified state object.
     * @param containers    The list of containers.
     * @param state         The specified state object.
     */
    static void waitForContainersToSwitch(Collection<Container> containers, ZooKeeperClusterState state) {
        // Wait until all containers switched
        boolean allStarted = false;
        long t0 = System.currentTimeMillis();
        CuratorFramework curator = state.getCuratorFramework();
        try {
            curator.getZookeeperClient().blockUntilConnectedOrTimedOut();
            while (!allStarted && System.currentTimeMillis() - t0 < state.getCreateEnsembleOptions().getMigrationTimeout()) {
                allStarted = true;
                for (Container container : containers) {
                    allStarted &= exists(curator, ZkPath.CONTAINER_ALIVE.getPath(container.getId())) != null;
                }
                if (!allStarted) {
                    Thread.sleep(1000);
                }
            }
            if (!allStarted) {
                throw new EnsembleModificationFailed("Timeout waiting for containers to join the new ensemble", EnsembleModificationFailed.Reason.TIMEOUT);
            }
        } catch (Exception e) {

        }
    }

    /**
     * Polls via JMX the specified container, until it switches to the ensemble with the specified id.
     * @param container     The target container.
     * @param state         The state object describing the "ensemble".
     * @param user          The jmx user.
     * @param password      The jmx password.
     * @param timeout       The timeout.
     * @return
     * @throws InterruptedException
     */
    static boolean waitForZooKeeperServer(Container container, ZooKeeperClusterState state, String user, String password, long timeout) throws InterruptedException {
        String expectedEnsembleId = String.format(ENSEMBLE_ID_FORMAT, state.getClusterId());
        long startMillis = System.currentTimeMillis();
        while (System.currentTimeMillis() - startMillis < timeout) {
            try {
                ContainerTemplate containerTemplate = new ContainerTemplate(container, user, password, false);
                String ensembleId = containerTemplate.execute(new JmxTemplateSupport.JmxConnectorCallback<String>() {
                    @Override
                    public String doWithJmxConnector(JMXConnector connector) throws Exception {
                        return String.valueOf(connector.getMBeanServerConnection().getAttribute(new ObjectName("io.fabric8:type=ZooKeeperServerInfo"), "EnsembleId"));
                    }
                });
                if (expectedEnsembleId.equals(ensembleId)) {
                    return true;
                } else {
                    Thread.sleep(5000);
                }
            } catch (Exception e) {
                //ignore
            }
        }
        return false;
    }

    static void cleanUp(ZooKeeperClusterState state, Collection<Container> containers) throws Exception {
        for (Container container : containers) {
            applyClusterProfiles(state, container, state);
        }
    }


    /**
     * Updates the ensemble url entries.
     *
     * @param curator       The curator framework instance to use.
     * @param url           The url of the ensemble.
     * @param password      The password.
     * @throws Exception
     */
    static void updateEnsembleUrl(CuratorFramework curator, String url, String password) throws Exception {
        Map<String, String> nodesToSet = new HashMap<>();
        nodesToSet.put(ZkPath.CONFIG_ENSEMBLE_PASSWORD.getPath(), PasswordEncoder.encode(password));
        nodesToSet.put(ZkPath.CONFIG_ENSEMBLE_URL.getPath(), url);
        curator.getZookeeperClient().blockUntilConnectedOrTimedOut();
        setDataAtomic(curator, new HashSet<String>(), nodesToSet);
    }

    /**
     * Low level operation to set profiles and clea up "obsolete ensemble profiles to a container.
     * @param current           The curator framework intance to use.
     * @param container         The target container.
     * @param state             The state of the "ensemble".
     * @throws Exception
     */
    static void applyClusterProfiles(ZooKeeperClusterState current, Container container, ZooKeeperClusterState state) throws Exception {
        CuratorFramework curator = current.getCuratorFramework();
        curator.getZookeeperClient().blockUntilConnectedOrTimedOut();

        Map<String, String> nodesToSet = new HashMap<>();

        int membershipId = state.getContainerMembershipId(container);
        String versionId = getStringData(curator, ZkPath.CONFIG_CONTAINER.getPath(container.getId()));

        Set<String> profiles = new HashSet<>(Strings.splitAndTrimAsList(getStringData(curator, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(versionId, container.getId())), " "));
        Set<String> newProfiles = new HashSet<>();
        for (String profileId : profiles) {
            if (!profileId.startsWith(CLUSTER_PROFILE_PREFIX)) {
                newProfiles.add(profileId);
            }
        }
        if (membershipId > 0) {
            newProfiles.add(String.format(MEMBER_PROFILE_FORMAT, state.getClusterId(), membershipId));
        }
        nodesToSet.put(ZkPath.CONFIG_VERSIONS_CONTAINER.getPath(versionId, container.getId()), Strings.join(newProfiles, " "));
        setDataAtomic(curator, new HashSet<String>(), nodesToSet);
    }

    /**
     * Creates the entries in the registry that reflect the ensemble defined by the state.
     * @param curator       The curator framework instance to use.
     * @param state         The state object describing the "ensemble".
     * @throws Exception
     */
    static void createEnsembleEntries(CuratorFramework curator, ZooKeeperClusterState state) throws Exception {
        Set<String> nodesToCreate = new HashSet<>();
        Map<String, String> nodesToSet = new HashMap<>();
        String ensemblePath = ZkPath.CONFIG_ENSEMBLE.getPath(String.format(ENSEMBLE_ID_FORMAT, state.getClusterId()));
        nodesToCreate.add(ensemblePath);
        nodesToSet.put(ensemblePath, Strings.join(state.getMembers().values(), ","));
        nodesToSet.put(ZkPath.CONFIG_ENSEMBLES.getPath(), String.format(ENSEMBLE_ID_FORMAT, state.getClusterId()));
        curator.getZookeeperClient().blockUntilConnectedOrTimedOut();
        setDataAtomic(curator, nodesToCreate, nodesToSet);
    }

    /**
     * Create the specified nodes and set values, in an atomic operation.
     * @param curator           The curator framework instance to use.
     * @param nodesToCreate     The nodes to create.
     * @param nodesToSet        A map with the path/value pairs.
     * @throws Exception
     */
    static void setDataAtomic(CuratorFramework curator, Set<String> nodesToCreate, Map<String, String> nodesToSet) throws Exception {
        CuratorTransaction transaction = curator.inTransaction();

        for (String path : nodesToCreate) {
            transaction = transaction.create().forPath(path).and();
        }
        for (Map.Entry<String, String> entry : nodesToSet.entrySet()) {
            String path = entry.getKey();
            String value = entry.getValue();
            transaction = transaction.setData().forPath(path, value.getBytes()).and();
        }

        ((CuratorTransactionFinal) transaction).commit();
    }


}
