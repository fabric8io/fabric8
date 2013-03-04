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
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.ZooKeeperClusterService;
import org.fusesource.fabric.itests.paxexam.support.ContainerBuilder;
import org.fusesource.fabric.itests.paxexam.support.Provision;
import org.fusesource.fabric.itests.paxexam.support.WaitForZookeeperUrlChange;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.linkedin.util.clock.Timespan;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.debugConfiguration;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class EnsembleTest extends FabricTestSupport {

    private final ExecutorService excutorService = Executors.newCachedThreadPool();

    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }

    @Test
    public void testAddAndRemove() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        ZooKeeperClusterService zooKeeperClusterService = ServiceLocator.getOsgiService(ZooKeeperClusterService.class);
        Assert.assertNotNull(zooKeeperClusterService);

        IZKClient zookeeper = getZookeeper();
        FabricService fabricService = getFabricService();

        Deque<Container> containerQueue = new LinkedList<Container>(ContainerBuilder.create(2).withName("ens").assertProvisioningResult().build());
        Deque<Container> addedContainers = new LinkedList<Container>();

            for (int e = 0; e < 3 && containerQueue.size() >= 2 && containerQueue.size() % 2 == 0; e++) {
                Container cnt1 = containerQueue.removeFirst();
                Container cnt2 = containerQueue.removeFirst();
                addedContainers.add(cnt1);
                addedContainers.add(cnt2);
                WaitForZookeeperUrlChange waitTask = new WaitForZookeeperUrlChange(zookeeper, zookeeper.getConnectString());
                System.err.println(executeCommand("fabric:container-resolver-list"));
                System.err.println(executeCommand("fabric:ensemble-add " + cnt1.getId() + " " + cnt2.getId()));
                Future<String> future = excutorService.submit(waitTask);
                future.get(120, TimeUnit.SECONDS);
                zookeeper.waitForConnected(new Timespan(30, Timespan.TimeUnit.SECOND));
                System.err.println(executeCommand("config:proplist --pid org.fusesource.fabric.zookeeper"));
                System.err.println(executeCommand("fabric:container-list"));
                List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
                Assert.assertTrue(ensembleContainersResult.contains(cnt1.getId()));
                Assert.assertTrue(ensembleContainersResult.contains(cnt2.getId()));
                Provision.waitForContainerAlive(Arrays.asList(fabricService.getContainers()), PROVISION_TIMEOUT);
            }


            for (int e = 0; e < 3 && addedContainers.size() >= 2 && addedContainers.size() % 2 == 0; e++) {
                Container cnt1 = addedContainers.removeFirst();
                Container cnt2 = addedContainers.removeFirst();
                containerQueue.add(cnt1);
                containerQueue.add(cnt2);
                WaitForZookeeperUrlChange waitTask = new WaitForZookeeperUrlChange(zookeeper, zookeeper.getConnectString());
                System.err.println(executeCommand("fabric:container-resolver-list"));
                System.err.println(executeCommand("fabric:ensemble-remove " + cnt1.getId() + " " + cnt2.getId()));
                Future<String> future = excutorService.submit(waitTask);
                future.get(120, TimeUnit.SECONDS);
                zookeeper.waitForConnected(new Timespan(30, Timespan.TimeUnit.SECOND));
                System.err.println(executeCommand("config:proplist --pid org.fusesource.fabric.zookeeper"));
                System.err.println(executeCommand("fabric:container-list"));
                List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
                Assert.assertFalse(ensembleContainersResult.contains(cnt1.getId()));
                Assert.assertFalse(ensembleContainersResult.contains(cnt2.getId()));
                Provision.waitForContainerAlive(Arrays.asList(fabricService.getContainers()), PROVISION_TIMEOUT);
            }
    }

    @Test
    public void testAddAndRemoveWithVersions() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        ZooKeeperClusterService zooKeeperClusterService = ServiceLocator.getOsgiService(ZooKeeperClusterService.class);
        Assert.assertNotNull(zooKeeperClusterService);

        IZKClient zookeeper = getZookeeper();
        FabricService fabricService = getFabricService();

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
            WaitForZookeeperUrlChange waitTask = new WaitForZookeeperUrlChange(zookeeper, zookeeper.getConnectString());
            System.err.println(executeCommand("fabric:ensemble-add " + cnt1.getId() + " " + cnt2.getId()));
            Future<String> future = excutorService.submit(waitTask);
            future.get(60, TimeUnit.SECONDS);
            zookeeper.waitForConnected(new Timespan(30, Timespan.TimeUnit.SECOND));
            System.err.println(executeCommand("config:proplist --pid org.fusesource.fabric.zookeeper"));
            System.err.println(executeCommand("fabric:container-list"));
            List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
            Assert.assertTrue(ensembleContainersResult.contains(cnt1.getId()));
            Assert.assertTrue(ensembleContainersResult.contains(cnt2.getId()));
            System.err.println(executeCommand("fabric:container-list"));
            Provision.waitForContainerAlive(Arrays.asList(fabricService.getContainers()), PROVISION_TIMEOUT);
        }



        System.err.println(executeCommand("fabric:version-create"));
        System.err.println(executeCommand("fabric:container-upgrade --all 1.3"));

        for (int e = 0; e < 3 && addedContainers.size() >= 2 && addedContainers.size() % 2 == 0; e++) {
            Container cnt1 = addedContainers.removeFirst();
            Container cnt2 = addedContainers.removeFirst();
            WaitForZookeeperUrlChange waitTask = new WaitForZookeeperUrlChange(zookeeper, zookeeper.getConnectString());
            System.err.println(executeCommand("fabric:ensemble-remove " + cnt1.getId() + " " + cnt2.getId()));
            Future<String> future = excutorService.submit(waitTask);
            future.get(60, TimeUnit.SECONDS);
            zookeeper.waitForConnected(new Timespan(30, Timespan.TimeUnit.SECOND));
            System.err.println(executeCommand("config:proplist --pid org.fusesource.fabric.zookeeper"));
            System.err.println(executeCommand("fabric:container-list"));
            List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
            Assert.assertFalse(ensembleContainersResult.contains(cnt1.getId()));
            Assert.assertFalse(ensembleContainersResult.contains(cnt2.getId()));
            Provision.waitForContainerAlive(Arrays.asList(fabricService.getContainers()), PROVISION_TIMEOUT);
        }
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                debugConfiguration("5005", false)
        };
    }
}
