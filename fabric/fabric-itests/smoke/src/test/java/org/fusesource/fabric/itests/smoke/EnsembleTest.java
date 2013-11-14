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
package org.fusesource.fabric.itests.smoke;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.EnsembleModificationFailed;
import org.fusesource.fabric.api.ZooKeeperClusterService;
import org.fusesource.fabric.itests.paxexam.support.ContainerBuilder;
import org.fusesource.fabric.itests.paxexam.support.FabricTestSupport;
import org.fusesource.fabric.itests.paxexam.support.Provision;
import org.fusesource.tooling.testing.pax.exam.karaf.CommandExecutionException;
import org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
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
public class EnsembleTest extends FabricTestSupport {

    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }

    @Test
    @Ignore("[FABRIC-643] Fix fabric smoke EnsembleTest")
    public void testAddAndRemove() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        waitForFabricCommands();
        Deque<Container> containerQueue = new LinkedList<Container>(ContainerBuilder.create(2).withName("ens").assertProvisioningResult().build());
        Deque<Container> addedContainers = new LinkedList<Container>();

            for (int e = 0; e < 3 && containerQueue.size() >= 2 && containerQueue.size() % 2 == 0; e++) {
                Container cnt1 = containerQueue.removeFirst();
                Container cnt2 = containerQueue.removeFirst();
                addedContainers.add(cnt1);
                addedContainers.add(cnt2);
                addToEnsemble(cnt1, cnt2);
                System.err.println(executeCommand("config:proplist --pid org.fusesource.fabric.zookeeper"));
                Thread.sleep(5000);
                System.err.println(executeCommand("fabric:container-list"));
                System.err.println(executeCommand("fabric:ensemble-list"));
                ZooKeeperClusterService zooKeeperClusterService = ServiceLocator.getOsgiService(ZooKeeperClusterService.class);
                Assert.assertNotNull(zooKeeperClusterService);
                List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
                Assert.assertTrue(ensembleContainersResult.contains(cnt1.getId()));
                Assert.assertTrue(ensembleContainersResult.contains(cnt2.getId()));
                Provision.containerAlive(Arrays.asList(getFabricService().getContainers()), PROVISION_TIMEOUT);
            }


            for (int e = 0; e < 3 && addedContainers.size() >= 2 && addedContainers.size() % 2 == 0; e++) {
                Container cnt1 = addedContainers.removeFirst();
                Container cnt2 = addedContainers.removeFirst();
                containerQueue.add(cnt1);
                containerQueue.add(cnt2);
                removeFromEnsemble(cnt1, cnt2);
                System.err.println(executeCommand("config:proplist --pid org.fusesource.fabric.zookeeper"));
                Thread.sleep(5000);
                System.err.println(executeCommand("fabric:container-list"));
                System.err.println(executeCommand("fabric:ensemble-list"));
                ZooKeeperClusterService zooKeeperClusterService = ServiceLocator.getOsgiService(ZooKeeperClusterService.class);
                Assert.assertNotNull(zooKeeperClusterService);
                List<String> ensembleContainersResult = zooKeeperClusterService.getEnsembleContainers();
                Assert.assertFalse(ensembleContainersResult.contains(cnt1.getId()));
                Assert.assertFalse(ensembleContainersResult.contains(cnt2.getId()));
                Provision.containerAlive(Arrays.asList(getFabricService().getContainers()), PROVISION_TIMEOUT);
            }
    }

    void addToEnsemble(Container... containers) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("fabric:ensemble-add --force --migration-timeout 240000 ");
        for (Container c : containers) {
            sb.append(c.getId()).append(" ");
        }

        try {
            System.err.println(executeCommand(sb.toString(), 240000L, false));
        } catch (CommandExecutionException e) {
            if (isRetriable(e)) {
                System.err.println("Retrying...");
                Provision.containerAlive(Arrays.asList(getFabricService().getContainers()), PROVISION_TIMEOUT);
                System.err.println(executeCommand(sb.toString(), 240000L, false));
            } else {
                throw e;
            }
        }
    }

    void removeFromEnsemble(Container... containers) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("fabric:ensemble-remove --force --migration-timeout 240000 ");
        for (Container c : containers) {
            sb.append(c.getId()).append(" ");
        }

        try {
            System.err.println(executeCommand(sb.toString(), 240000L, false));
        } catch (CommandExecutionException e) {
            if (isRetriable(e)) {
                System.err.println("Retrying...");
                Provision.containerAlive(Arrays.asList(getFabricService().getContainers()), PROVISION_TIMEOUT);
                System.err.println(executeCommand(sb.toString(), 240000L, false));
            } else {
                throw e;
            }
        }
    }

    private static boolean isRetriable(Throwable t) {
        return t.getCause() instanceof EnsembleModificationFailed && ((EnsembleModificationFailed)t).getReason() == EnsembleModificationFailed.Reason.CONTAINERS_NOT_ALIVE;
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
        };
    }
}
