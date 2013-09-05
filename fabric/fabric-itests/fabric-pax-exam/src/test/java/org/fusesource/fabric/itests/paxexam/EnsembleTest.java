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
package org.fusesource.fabric.itests.paxexam;

import junit.framework.Assert;

import org.apache.curator.framework.CuratorFramework;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.ZooKeeperClusterService;
import org.fusesource.fabric.itests.paxexam.support.ContainerBuilder;
import org.fusesource.fabric.itests.paxexam.support.FabricTestSupport;
import org.fusesource.fabric.itests.paxexam.support.Provision;
import org.fusesource.fabric.itests.paxexam.support.WaitForServiceAddingTask;
import org.fusesource.fabric.itests.paxexam.support.WaitForZookeeperUrlChange;
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

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.debugConfiguration;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
@Ignore("[FABRIC-521] Fix fabric-pax-exam tests")
public class EnsembleTest extends FabricTestSupport {

    private final ExecutorService excutorService = Executors.newCachedThreadPool();

    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }

    @Test
    public void testAddAndRemove() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        //TODO: fix ensemble issues with git bridge
        System.err.println(executeCommand("fabric:profile-edit --delete --features fabric-git default"));

        Deque<Container> containerQueue = new LinkedList<Container>(ContainerBuilder.create(2).withName("ens").assertProvisioningResult().build());
        Deque<Container> addedContainers = new LinkedList<Container>();

            for (int e = 0; e < 3 && containerQueue.size() >= 2 && containerQueue.size() % 2 == 0; e++) {
                Container cnt1 = containerQueue.removeFirst();
                Container cnt2 = containerQueue.removeFirst();
                addedContainers.add(cnt1);
                addedContainers.add(cnt2);
                WaitForServiceAddingTask<CuratorFramework> waitTask = new WaitForServiceAddingTask<CuratorFramework>(CuratorFramework.class, null);
                System.err.println(executeCommand("fabric:container-resolver-list"));
                System.err.println(executeCommand("fabric:ensemble-add --force " + cnt1.getId() + " " + cnt2.getId()));
                Future<CuratorFramework> future = excutorService.submit(waitTask);
                CuratorFramework curator = future.get(120, TimeUnit.SECONDS);
                curator.getZookeeperClient().blockUntilConnectedOrTimedOut();
                System.err.println(executeCommand("config:proplist --pid org.fusesource.fabric.zookeeper"));
                Thread.sleep(5000);
                System.err.println(executeCommand("fabric:container-list"));
                System.err.println(executeCommand("fabric:ensemble-list"));
                ZooKeeperClusterService zooKeeperClusterService = ServiceLocator.getOsgiService(ZooKeeperClusterService.class);
                Assert.assertNotNull(zooKeeperClusterService);
                List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
                Assert.assertTrue(ensembleContainersResult.contains(cnt1.getId()));
                Assert.assertTrue(ensembleContainersResult.contains(cnt2.getId()));
                Provision.waitForContainerAlive(Arrays.asList(getFabricService().getContainers()), PROVISION_TIMEOUT);
            }


            for (int e = 0; e < 3 && addedContainers.size() >= 2 && addedContainers.size() % 2 == 0; e++) {
                Container cnt1 = addedContainers.removeFirst();
                Container cnt2 = addedContainers.removeFirst();
                containerQueue.add(cnt1);
                containerQueue.add(cnt2);
                WaitForServiceAddingTask<CuratorFramework> waitTask = new WaitForServiceAddingTask<CuratorFramework>(CuratorFramework.class, null);
                System.err.println(executeCommand("fabric:container-resolver-list"));
                System.err.println(executeCommand("fabric:ensemble-remove --force " + cnt1.getId() + " " + cnt2.getId()));
                Future<CuratorFramework> future = excutorService.submit(waitTask);
                CuratorFramework curator = future.get(120, TimeUnit.SECONDS);
                curator.getZookeeperClient().blockUntilConnectedOrTimedOut();
                System.err.println(executeCommand("config:proplist --pid org.fusesource.fabric.zookeeper"));
                Thread.sleep(5000);
                System.err.println(executeCommand("fabric:container-list"));
                System.err.println(executeCommand("fabric:ensemble-list"));
                ZooKeeperClusterService zooKeeperClusterService = ServiceLocator.getOsgiService(ZooKeeperClusterService.class);
                Assert.assertNotNull(zooKeeperClusterService);
                List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
                Assert.assertFalse(ensembleContainersResult.contains(cnt1.getId()));
                Assert.assertFalse(ensembleContainersResult.contains(cnt2.getId()));
                Provision.waitForContainerAlive(Arrays.asList(getFabricService().getContainers()), PROVISION_TIMEOUT);
            }
    }

    @Test
    public void testAddAndRemoveWithVersions() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        //TODO: fix ensemble issues with git bridge
        System.err.println(executeCommand("fabric:profile-edit --delete --features fabric-git default"));

        System.err.println(executeCommand("fabric:version-create"));
        System.err.println(executeCommand("fabric:container-upgrade --all 1.1"));

        Deque<Container> containerQueue = new LinkedList<Container>(ContainerBuilder.create(2).withName("ens").assertProvisioningResult().build());
        Deque<Container> addedContainers = new LinkedList<Container>();

        System.err.println(executeCommand("fabric:version-create"));
        System.err.println(executeCommand("fabric:container-upgrade --all 1.2"));

        for (int e = 0; e < 3 && containerQueue.size() >= 2 && containerQueue.size() % 2 == 0; e++) {
            Container cnt1 = containerQueue.removeFirst();
            Container cnt2 = containerQueue.removeFirst();
            addedContainers.add(cnt1);
            addedContainers.add(cnt2);
            WaitForServiceAddingTask<CuratorFramework> waitTask = new WaitForServiceAddingTask<CuratorFramework>(CuratorFramework.class, null);
            System.err.println(executeCommand("fabric:ensemble-add --force " + cnt1.getId() + " " + cnt2.getId()));
            Future<CuratorFramework> future = excutorService.submit(waitTask);
            CuratorFramework curator = future.get(60, TimeUnit.SECONDS);
            curator.getZookeeperClient().blockUntilConnectedOrTimedOut();
            System.err.println(executeCommand("config:proplist --pid org.fusesource.fabric.zookeeper"));
            Thread.sleep(5000);
            System.err.println(executeCommand("fabric:container-list"));
            System.err.println(executeCommand("fabric:ensemble-list"));
            ZooKeeperClusterService zooKeeperClusterService = ServiceLocator.getOsgiService(ZooKeeperClusterService.class);
            Assert.assertNotNull(zooKeeperClusterService);
            List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
            Assert.assertTrue(ensembleContainersResult.contains(cnt1.getId()));
            Assert.assertTrue(ensembleContainersResult.contains(cnt2.getId()));
            System.err.println(executeCommand("fabric:container-list"));
            Provision.waitForContainerAlive(Arrays.asList(getFabricService().getContainers()), PROVISION_TIMEOUT);
        }


        System.err.println(executeCommand("fabric:version-create"));
        System.err.println(executeCommand("fabric:container-upgrade --all 1.3"));

        for (int e = 0; e < 3 && addedContainers.size() >= 2 && addedContainers.size() % 2 == 0; e++) {
            Container cnt1 = addedContainers.removeFirst();
            Container cnt2 = addedContainers.removeFirst();
            WaitForServiceAddingTask<CuratorFramework> waitTask = new WaitForServiceAddingTask<CuratorFramework>(CuratorFramework.class, null);
            System.err.println(executeCommand("fabric:ensemble-remove --force " + cnt1.getId() + " " + cnt2.getId()));
            Future<CuratorFramework> future = excutorService.submit(waitTask);
            CuratorFramework curator = future.get(60, TimeUnit.SECONDS);
            curator.getZookeeperClient().blockUntilConnectedOrTimedOut();
            System.err.println(executeCommand("config:proplist --pid org.fusesource.fabric.zookeeper"));
            Thread.sleep(5000);
            System.err.println(executeCommand("fabric:container-list"));
            System.err.println(executeCommand("fabric:ensemble-list"));
            ZooKeeperClusterService zooKeeperClusterService = ServiceLocator.getOsgiService(ZooKeeperClusterService.class);
            Assert.assertNotNull(zooKeeperClusterService);
            List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
            Assert.assertFalse(ensembleContainersResult.contains(cnt1.getId()));
            Assert.assertFalse(ensembleContainersResult.contains(cnt2.getId()));
            Provision.waitForContainerAlive(Arrays.asList(getFabricService().getContainers()), PROVISION_TIMEOUT);
        }
    }

    /**
     * We want to test the ensemble health is not affected if part of the ensemble is switched to an other version.
     * @throws Exception
     */
    @Test
    public void testAddAndRemoveWithPartialVersionUpgrades() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));

        //TODO: fix ensemble issues with git bridge
        System.err.println(executeCommand("fabric:profile-edit --delete --features fabric-git default"));

        System.err.println(executeCommand("fabric:version-create"));
        System.err.println(executeCommand("fabric:container-upgrade --all 1.1"));

        LinkedList<Container> containerQueue = new LinkedList<Container>(ContainerBuilder.create(2).withName("ens").assertProvisioningResult().build());
        LinkedList<Container> addedContainers = new LinkedList<Container>();

        System.err.println(executeCommand("fabric:version-create"));
        System.err.println(executeCommand("fabric:container-upgrade --all 1.2"));


        Random rand = new Random();

        for (int version = 3; version < 6; version++) {

            Provision.waitForContainerAlive(containerQueue, PROVISION_TIMEOUT);
            Provision.waitForContainerAlive(addedContainers, PROVISION_TIMEOUT);
            for (int e = 0; e < 3 && containerQueue.size() >= 2 && containerQueue.size() % 2 == 0; e++) {
                Container cnt1 = containerQueue.removeFirst();
                Container cnt2 = containerQueue.removeFirst();
                addedContainers.add(cnt1);
                addedContainers.add(cnt2);
                WaitForServiceAddingTask<CuratorFramework> waitTask = new WaitForServiceAddingTask<CuratorFramework>(CuratorFramework.class, null);
                System.err.println(executeCommand("fabric:ensemble-add --force " + cnt1.getId() + " " + cnt2.getId()));
                Future<CuratorFramework> future = excutorService.submit(waitTask);
                CuratorFramework curator = future.get(60, TimeUnit.SECONDS);
                curator.getZookeeperClient().blockUntilConnectedOrTimedOut();
                System.err.println(executeCommand("config:proplist --pid org.fusesource.fabric.zookeeper"));
                Thread.sleep(5000);
                System.err.println(executeCommand("fabric:container-list"));
                System.err.println(executeCommand("fabric:ensemble-list"));
                ZooKeeperClusterService zooKeeperClusterService = ServiceLocator.getOsgiService(ZooKeeperClusterService.class);
                Assert.assertNotNull(zooKeeperClusterService);
                List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
                Assert.assertTrue(ensembleContainersResult.contains(cnt1.getId()));
                Assert.assertTrue(ensembleContainersResult.contains(cnt2.getId()));
                System.err.println(executeCommand("fabric:container-list"));
                Provision.waitForContainerAlive(Arrays.asList(getFabricService().getContainers()), PROVISION_TIMEOUT);
            }


            int index = rand.nextInt(addedContainers.size());
            String randomContainer = addedContainers.get(index).getId();
            System.err.println(executeCommand("fabric:version-create 1." + version));
            System.err.println(executeCommand("fabric:container-upgrade 1." + version + " " + randomContainer));

            Provision.waitForContainerAlive(containerQueue, PROVISION_TIMEOUT);
            Provision.waitForContainerAlive(addedContainers, PROVISION_TIMEOUT);
            for (int e = 0; e < 3 && addedContainers.size() >= 2 && addedContainers.size() % 2 == 0; e++) {
                Container cnt1 = addedContainers.removeFirst();
                Container cnt2 = addedContainers.removeFirst();
                containerQueue.add(cnt1);
                containerQueue.add(cnt2);
                WaitForServiceAddingTask<CuratorFramework> waitTask = new WaitForServiceAddingTask<CuratorFramework>(CuratorFramework.class, null);
                System.err.println(executeCommand("fabric:ensemble-remove --force " + cnt1.getId() + " " + cnt2.getId()));
                Future<CuratorFramework> future = excutorService.submit(waitTask);
                CuratorFramework curator = future.get(60, TimeUnit.SECONDS);
                curator.getZookeeperClient().blockUntilConnectedOrTimedOut();
                System.err.println(executeCommand("config:proplist --pid org.fusesource.fabric.zookeeper"));
                Thread.sleep(5000);
                System.err.println(executeCommand("fabric:container-list"));
                System.err.println(executeCommand("fabric:ensemble-list"));
                ZooKeeperClusterService zooKeeperClusterService = ServiceLocator.getOsgiService(ZooKeeperClusterService.class);
                Assert.assertNotNull(zooKeeperClusterService);
                List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
                Assert.assertFalse(ensembleContainersResult.contains(cnt1.getId()));
                Assert.assertFalse(ensembleContainersResult.contains(cnt2.getId()));
                Provision.waitForContainerAlive(Arrays.asList(getFabricService().getContainers()), PROVISION_TIMEOUT);
            }

            System.err.println(executeCommand("fabric:container-rollback --all 1."+(version-1)));
        }
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                //debugConfiguration("5005", false)
        };
    }
}
