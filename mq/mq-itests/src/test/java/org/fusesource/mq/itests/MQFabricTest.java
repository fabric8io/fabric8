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
package org.fusesource.mq.itests;

import org.apache.activemq.command.DiscoveryEvent;
import org.apache.activemq.transport.discovery.DiscoveryListener;
import org.apache.curator.framework.CuratorFramework;
import org.apache.karaf.tooling.exam.options.LogLevelOption;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.itests.paxexam.support.ContainerBuilder;
import org.fusesource.fabric.itests.paxexam.support.Provision;
import org.fusesource.mq.fabric.FabricDiscoveryAgent;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.keepRuntimeFolder;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.logLevel;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class MQFabricTest extends MQTestSupport {

    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }

    @Test
    public void testLocalFabricCluster() throws Exception {
        final String brokerName = "root";
        final String groupName = "default";
        final AtomicBoolean master = new AtomicBoolean();

        System.out.println(executeCommand("fabric:create -n --clean root"));
        //Wait for zookeeper service to become available.
        CuratorFramework curatorFramework = getCurator();
        final CountDownLatch serviceLatch = new CountDownLatch(1);
        final FabricDiscoveryAgent discoveryAgent = new FabricDiscoveryAgent();

        discoveryAgent.setCurator(curatorFramework);
        discoveryAgent.setGroupName(groupName);
        discoveryAgent.setDiscoveryListener( new DiscoveryListener() {
            @Override
            public void onServiceAdd(DiscoveryEvent discoveryEvent) {
                System.out.println("Service added:" + discoveryEvent.getServiceName());
                serviceLatch.countDown();
            }

            @Override
            public void onServiceRemove(DiscoveryEvent discoveryEvent) {
                System.out.println("Service removed:" + discoveryEvent.getServiceName());
            }
        });

        discoveryAgent.start();
        assertTrue(serviceLatch.await(20, TimeUnit.SECONDS));
        System.out.println(executeCommand("fabric:cluster-list"));
    }


    @Test
    public void testMQCreateWithFailover() throws Exception {
        final String brokerName = "testBroker";
        final String groupName = "testGroup";
        final Semaphore semaphore = new Semaphore(0);

        System.out.println(executeCommand("fabric:create -n"));
        Thread.sleep(5000);
        //Wait for zookeeper service to become available.
        CuratorFramework curatorFramework = getCurator();

        final FabricDiscoveryAgent discoveryAgent = new FabricDiscoveryAgent();
        discoveryAgent.setCurator(curatorFramework);
        discoveryAgent.setGroupName(groupName);
        discoveryAgent.setDiscoveryListener(new DiscoveryListener() {
            @Override
            public void onServiceAdd(DiscoveryEvent discoveryEvent) {
                System.out.println("Service added:" + discoveryEvent);
                semaphore.release(1);
            }

            @Override
            public void onServiceRemove(DiscoveryEvent discoveryEvent) {
            }
        });
        discoveryAgent.start();

        System.out.println(executeCommand("fabric:mq-create --group " + groupName + " " + brokerName));
        Set<Container> containers = ContainerBuilder.child(2).withName("child").withProfiles("mq-broker-"+groupName+"."+brokerName).assertProvisioningResult().build();
        System.out.println(executeCommand("fabric:container-list"));

        for (int i = 0; i < 2; i++) {
            System.out.println("Waiting for master.");
            assertTrue(semaphore.tryAcquire(30, TimeUnit.SECONDS));
            semaphore.drainPermits();
            System.out.println(executeCommand("fabric:cluster-list | grep -A 1 " + groupName));

            //Get the master and stop it gracefully.
            FabricDiscoveryAgent.ActiveMQNode master = discoveryAgent.getGroup().master();
            assertNotNull(master);
            String masterName = master.getContainer();
            assertNotNull(master.getContainer());
            FabricService fabricService = getFabricService();

            System.out.println("Stopping the master: "+masterName+".");
            Container masterContainer = fabricService.getContainer(masterName);
            masterContainer.stop();
            masterContainer.start();
            Provision.provisioningSuccess(Arrays.asList(masterContainer), PROVISION_TIMEOUT);

            System.out.println("Waiting for failover.");
            assertTrue(semaphore.tryAcquire(30, TimeUnit.SECONDS));
            semaphore.drainPermits();
            System.out.println(executeCommand("fabric:cluster-list | grep -A 1 " + groupName));
            master = discoveryAgent.getGroup().master();
            masterName = master.getContainer();
            assertNotNull(master.getContainer());
            System.out.println("Stopping the master: "+masterName+".");
            masterContainer = fabricService.getContainer(masterName);
            masterContainer.stop();
            masterContainer.start();
            Provision.provisioningSuccess(Arrays.asList(masterContainer), PROVISION_TIMEOUT);
        }
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(mqDistributionConfiguration()), keepRuntimeFolder(),
                logLevel(LogLevelOption.LogLevel.INFO)
        };
    }
}
