/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.runtime.itests.karaf;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.ZooKeeperClusterService;
import io.fabric8.runtime.itests.support.CommandSupport;
import io.fabric8.runtime.itests.support.ContainerBuilder;
import io.fabric8.runtime.itests.support.FabricEnsembleSupport;
import io.fabric8.runtime.itests.support.FabricTestSupport;
import io.fabric8.runtime.itests.support.Provision;
import io.fabric8.runtime.itests.support.ServiceLocator;
import io.fabric8.runtime.itests.support.ServiceProxy;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("[FABRIC-819] Provide initial set of portable fabric smoke tests")
public class EnsembleTest {

    @Test
    public void testAddAndRemove() throws Exception {
        System.err.println(CommandSupport.executeCommand("fabric:create -n"));
        ModuleContext moduleContext = RuntimeLocator.getRequiredRuntime().getModuleContext();
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            Set<Container> containers = ContainerBuilder.create(2).withName("ens").assertProvisioningResult().build();
            try {
                Deque<Container> containerQueue = new LinkedList<Container>(containers);
                Deque<Container> addedContainers = new LinkedList<Container>();

                for (int e = 0; e < 3 && containerQueue.size() >= 2 && containerQueue.size() % 2 == 0; e++) {
                    Container cnt1 = containerQueue.removeFirst();
                    Container cnt2 = containerQueue.removeFirst();
                    addedContainers.add(cnt1);
                    addedContainers.add(cnt2);
                    FabricEnsembleSupport.addToEnsemble(fabricService, cnt1, cnt2);
                    System.err.println(CommandSupport.executeCommand("config:proplist --pid io.fabric8.zookeeper"));

                    System.err.println(CommandSupport.executeCommand("fabric:container-list"));
                    System.err.println(CommandSupport.executeCommand("fabric:ensemble-list"));
                    ZooKeeperClusterService zooKeeperClusterService = ServiceLocator.awaitService(moduleContext, ZooKeeperClusterService.class);
                    Assert.assertNotNull(zooKeeperClusterService);
                    List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
                    Assert.assertTrue(ensembleContainersResult.contains(cnt1.getId()));
                    Assert.assertTrue(ensembleContainersResult.contains(cnt2.getId()));
                    Provision.provisioningSuccess(Arrays.asList(fabricService.getContainers()), FabricTestSupport.PROVISION_TIMEOUT);
                }

                for (int e = 0; e < 3 && addedContainers.size() >= 2 && addedContainers.size() % 2 == 0; e++) {
                    Container cnt1 = addedContainers.removeFirst();
                    Container cnt2 = addedContainers.removeFirst();
                    containerQueue.add(cnt1);
                    containerQueue.add(cnt2);
                    FabricEnsembleSupport.removeFromEnsemble(fabricService, cnt1, cnt2);
                    System.err.println(CommandSupport.executeCommand("config:proplist --pid io.fabric8.zookeeper"));

                    System.err.println(CommandSupport.executeCommand("fabric:container-list"));
                    System.err.println(CommandSupport.executeCommand("fabric:ensemble-list"));
                    ZooKeeperClusterService zooKeeperClusterService = ServiceLocator.awaitService(moduleContext, ZooKeeperClusterService.class);
                    Assert.assertNotNull(zooKeeperClusterService);
                    List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
                    Assert.assertFalse(ensembleContainersResult.contains(cnt1.getId()));
                    Assert.assertFalse(ensembleContainersResult.contains(cnt2.getId()));
                    Provision.provisioningSuccess(Arrays.asList(fabricService.getContainers()), FabricTestSupport.PROVISION_TIMEOUT);
                }
            } finally {
                ContainerBuilder.destroy(containers);
            }
        } finally {
            fabricProxy.close();
        }
    }
}
