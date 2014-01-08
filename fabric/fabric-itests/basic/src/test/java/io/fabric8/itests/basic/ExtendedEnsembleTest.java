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
package io.fabric8.itests.basic;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.ZooKeeperClusterService;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.FabricEnsembleTest;
import io.fabric8.itests.paxexam.support.Provision;
import org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ExtendedEnsembleTest extends FabricEnsembleTest {

    private final ExecutorService excutorService = Executors.newCachedThreadPool();

    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }

    @Test
    @Ignore("[FABRIC-784] Fix ExtendedEnsembleTest")
    public void testAddAndRemoveWithVersions() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        FabricService fabricService = getFabricService();
        System.err.println(executeCommand("fabric:version-create --parent 1.0 1.1"));
        System.err.println(executeCommand("fabric:container-upgrade --all 1.1"));

        Deque<Container> containerQueue = new LinkedList<Container>(ContainerBuilder.create(2).withName("ens").withProfiles("default").assertProvisioningResult().build());
        Deque<Container> addedContainers = new LinkedList<Container>();

        System.err.println(executeCommand("fabric:version-create"));
        System.err.println(executeCommand("fabric:container-upgrade --all 1.2"));

        for (int e = 0; e < 3 && containerQueue.size() >= 2 && containerQueue.size() % 2 == 0; e++) {
            Container cnt1 = containerQueue.removeFirst();
            Container cnt2 = containerQueue.removeFirst();
            addedContainers.add(cnt1);
            addedContainers.add(cnt2);
            addToEnsemble(cnt1, cnt2);
            System.err.println(executeCommand("config:proplist --pid io.fabric8.zookeeper"));

            System.err.println(executeCommand("fabric:container-list"));
            System.err.println(executeCommand("fabric:ensemble-list"));
            ZooKeeperClusterService zooKeeperClusterService = ServiceLocator.getOsgiService(ZooKeeperClusterService.class);
            org.junit.Assert.assertNotNull(zooKeeperClusterService);
            List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
            org.junit.Assert.assertTrue(ensembleContainersResult.contains(cnt1.getId()));
            org.junit.Assert.assertTrue(ensembleContainersResult.contains(cnt2.getId()));
            Provision.provisioningSuccess(Arrays.asList(getFabricService().getContainers()), PROVISION_TIMEOUT);
        }


        System.err.println(executeCommand("fabric:version-create"));
        System.err.println(executeCommand("fabric:container-upgrade --all 1.3"));

        for (int e = 0; e < 3 && addedContainers.size() >= 2 && addedContainers.size() % 2 == 0; e++) {
            Container cnt1 = addedContainers.removeFirst();
            Container cnt2 = addedContainers.removeFirst();
            containerQueue.add(cnt1);
            containerQueue.add(cnt2);
            removeFromEnsemble(cnt1, cnt2);
            System.err.println(executeCommand("config:proplist --pid io.fabric8.zookeeper"));

            System.err.println(executeCommand("fabric:container-list"));
            System.err.println(executeCommand("fabric:ensemble-list"));
            ZooKeeperClusterService zooKeeperClusterService = ServiceLocator.getOsgiService(ZooKeeperClusterService.class);
            org.junit.Assert.assertNotNull(zooKeeperClusterService);
            List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
            org.junit.Assert.assertFalse(ensembleContainersResult.contains(cnt1.getId()));
            org.junit.Assert.assertFalse(ensembleContainersResult.contains(cnt2.getId()));
            Provision.provisioningSuccess(Arrays.asList(getFabricService().getContainers()), PROVISION_TIMEOUT);
        }
    }

    /**
     * We want to test the ensemble health is not affected if part of the ensemble is switched to an other version.
     * @throws Exception
     */
    @Test
    public void testAddAndRemoveWithPartialVersionUpgrades() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        FabricService fabricService = getFabricService();
        System.err.println(executeCommand("fabric:version-create"));
        System.err.println(executeCommand("fabric:container-upgrade --all 1.1"));

        LinkedList<Container> containerQueue = new LinkedList<Container>(ContainerBuilder.create(2).withName("ens").withProfiles("fabric").assertProvisioningResult().build());
        LinkedList<Container> addedContainers = new LinkedList<Container>();

        System.err.println(executeCommand("fabric:version-create"));
        System.err.println(executeCommand("fabric:container-upgrade --all 1.2"));


        Random rand = new Random();

        for (int version = 3; version < 5; version++) {

            Provision.provisioningSuccess(Arrays.asList(getFabricService().getContainers()), PROVISION_TIMEOUT);

            for (int e = 0; e < 3 && containerQueue.size() >= 2 && containerQueue.size() % 2 == 0; e++) {
                Container cnt1 = containerQueue.removeFirst();
                Container cnt2 = containerQueue.removeFirst();
                addedContainers.add(cnt1);
                addedContainers.add(cnt2);
                addToEnsemble(cnt1, cnt2);
                System.err.println(executeCommand("config:proplist --pid io.fabric8.zookeeper"));

                System.err.println(executeCommand("fabric:container-list"));
                System.err.println(executeCommand("fabric:ensemble-list"));
                ZooKeeperClusterService zooKeeperClusterService = ServiceLocator.getOsgiService(ZooKeeperClusterService.class);
                org.junit.Assert.assertNotNull(zooKeeperClusterService);
                List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
                org.junit.Assert.assertTrue(ensembleContainersResult.contains(cnt1.getId()));
                org.junit.Assert.assertTrue(ensembleContainersResult.contains(cnt2.getId()));
                Provision.provisioningSuccess(Arrays.asList(getFabricService().getContainers()), PROVISION_TIMEOUT);
            }


            int index = rand.nextInt(addedContainers.size());
            String randomContainer = addedContainers.get(index).getId();
            System.err.println(executeCommand("fabric:version-create 1." + version));
            System.err.println(executeCommand("fabric:container-upgrade 1." + version + " " + randomContainer));

            Provision.provisioningSuccess(Arrays.asList(getFabricService().getContainers()), PROVISION_TIMEOUT);

            for (int e = 0; e < 3 && addedContainers.size() >= 2 && addedContainers.size() % 2 == 0; e++) {
                Container cnt1 = addedContainers.removeFirst();
                Container cnt2 = addedContainers.removeFirst();
                containerQueue.add(cnt1);
                containerQueue.add(cnt2);
                removeFromEnsemble(cnt1, cnt2);
                System.err.println(executeCommand("config:proplist --pid io.fabric8.zookeeper"));

                System.err.println(executeCommand("fabric:container-list"));
                System.err.println(executeCommand("fabric:ensemble-list"));
                ZooKeeperClusterService zooKeeperClusterService = ServiceLocator.getOsgiService(ZooKeeperClusterService.class);
                org.junit.Assert.assertNotNull(zooKeeperClusterService);
                List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
                org.junit.Assert.assertFalse(ensembleContainersResult.contains(cnt1.getId()));
                org.junit.Assert.assertFalse(ensembleContainersResult.contains(cnt2.getId()));
                Provision.provisioningSuccess(Arrays.asList(getFabricService().getContainers()), PROVISION_TIMEOUT);
            }
            System.err.println(executeCommand("fabric:container-rollback 1." + (version - 1) + " " + randomContainer));
            //System.err.println(executeCommand("fabric:container-rollback " + getFabricService().getDefaultVersion().getId() + " " + randomContainer));
        }
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
        };
    }
}
