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
package io.fabric8.itests.smoke;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceLocator;
import io.fabric8.api.ServiceProxy;
import io.fabric8.api.ZooKeeperClusterService;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.FabricEnsembleTest;
import io.fabric8.itests.paxexam.support.Provision;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class EnsembleTest extends FabricEnsembleTest {

    @Test
    public void testAddAndRemove() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            Set<Container> containers = ContainerBuilder.create(fabricProxy,2).withName("ens").assertProvisioningResult().build();
            try {
                Deque<Container> containerQueue = new LinkedList<Container>(containers);
                Deque<Container> addedContainers = new LinkedList<Container>();

                for (int e = 0; e < 3 && containerQueue.size() >= 2 && containerQueue.size() % 2 == 0; e++) {
                    Container cnt1 = containerQueue.removeFirst();
                    Container cnt2 = containerQueue.removeFirst();
                    addedContainers.add(cnt1);
                    addedContainers.add(cnt2);
                    addToEnsemble(fabricService, cnt1, cnt2);
                    System.err.println(executeCommand("config:proplist --pid io.fabric8.zookeeper"));

                    System.err.println(executeCommand("fabric:container-list"));
                    System.err.println(executeCommand("fabric:ensemble-list"));
                    ZooKeeperClusterService zooKeeperClusterService = ServiceLocator.awaitService(bundleContext, ZooKeeperClusterService.class);
                    Assert.assertNotNull(zooKeeperClusterService);
                    List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
                    Assert.assertTrue(ensembleContainersResult.contains(cnt1.getId()));
                    Assert.assertTrue(ensembleContainersResult.contains(cnt2.getId()));
                    Provision.provisioningSuccess(Arrays.asList(fabricService.getContainers()), PROVISION_TIMEOUT);
                }

                for (int e = 0; e < 3 && addedContainers.size() >= 2 && addedContainers.size() % 2 == 0; e++) {
                    Container cnt1 = addedContainers.removeFirst();
                    Container cnt2 = addedContainers.removeFirst();
                    containerQueue.add(cnt1);
                    containerQueue.add(cnt2);
                    removeFromEnsemble(fabricService, cnt1, cnt2);
                    System.err.println(executeCommand("config:proplist --pid io.fabric8.zookeeper"));

                    System.err.println(executeCommand("fabric:container-list"));
                    System.err.println(executeCommand("fabric:ensemble-list"));
                    ZooKeeperClusterService zooKeeperClusterService = ServiceLocator.awaitService(bundleContext, ZooKeeperClusterService.class);
                    Assert.assertNotNull(zooKeeperClusterService);
                    List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
                    Assert.assertFalse(ensembleContainersResult.contains(cnt1.getId()));
                    Assert.assertFalse(ensembleContainersResult.contains(cnt2.getId()));
                    Provision.provisioningSuccess(Arrays.asList(fabricService.getContainers()), PROVISION_TIMEOUT);
                }
            } finally {
                ContainerBuilder.destroy(containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

    @Configuration
    public Option[] config() {
        return new Option[] { new DefaultCompositeOption(fabricDistributionConfiguration()), };
    }
}
