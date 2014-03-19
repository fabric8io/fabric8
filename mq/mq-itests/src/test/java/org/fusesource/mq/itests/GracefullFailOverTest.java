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

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.ContainerProxy;
import io.fabric8.itests.paxexam.support.Provision;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.command.DiscoveryEvent;
import org.apache.activemq.transport.discovery.DiscoveryListener;
import org.apache.curator.framework.CuratorFramework;
import org.apache.karaf.tooling.exam.options.KarafDistributionOption;
import org.apache.karaf.tooling.exam.options.LogLevelOption;
import org.fusesource.mq.fabric.FabricDiscoveryAgent;
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
public class GracefullFailOverTest extends MQTestSupport {

    @Test
    public void testMQCreateWithFailover() throws Exception {
        final String brokerName = "testBroker";
        final String groupName = "testGroup";
        final Semaphore semaphore = new Semaphore(0);

        System.out.println(executeCommand("fabric:create -n"));

        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            CuratorFramework curator = fabricService.adapt(CuratorFramework.class);

            final FabricDiscoveryAgent discoveryAgent = new FabricDiscoveryAgent();
            discoveryAgent.setCurator(curator);
            discoveryAgent.setGroupName(groupName);
            discoveryAgent.setDiscoveryListener(new DiscoveryListener() {
                @Override
                public void onServiceAdd(DiscoveryEvent discoveryEvent) {
                    System.out.println("Service added:" + discoveryEvent.getServiceName());
                    semaphore.release(1);
                }

                @Override
                public void onServiceRemove(DiscoveryEvent discoveryEvent) {
                }
            });
            discoveryAgent.start();

            Set<ContainerProxy> containers = setupCluster(fabricProxy, groupName, brokerName);
            try {
                System.out.println(executeCommand("fabric:container-list"));
                for (int i = 0; i < 2; i++) {
                    System.out.println("Waiting for master.");
                    semaphore.tryAcquire(30, TimeUnit.SECONDS);
                    semaphore.drainPermits();
                    System.out.println(executeCommand("fabric:cluster-list | grep -A 1 " + groupName));

                    //Get the master and stop it gracefully.
                    FabricDiscoveryAgent.ActiveMQNode master = discoveryAgent.getGroup().master();
                    Assert.assertNotNull(master);
                    String masterName = master.getContainer();
                    Assert.assertNotNull(master.getContainer());

                    System.out.println("Causing the master: "+masterName+" to failover.");
                    failOver(fabricService, masterName);

                    System.out.println("Waiting for failover.");
                    semaphore.tryAcquire(30, TimeUnit.SECONDS);
                    semaphore.drainPermits();
                    System.out.println(executeCommand("fabric:cluster-list | grep -A 1 " + groupName));
                    master = discoveryAgent.getGroup().master();
                    masterName = master.getContainer();
                    Assert.assertNotNull(master.getContainer());
                    System.out.println("Causing the master: " + masterName + " to failover.");
                    failOver(fabricService, masterName);
                }
            } finally {
                ContainerBuilder.destroy(containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

    Set<ContainerProxy> setupCluster(ServiceProxy<FabricService> fabricProxy, String groupName, String brokerName) throws Exception {
        System.out.println(executeCommand("fabric:mq-create --group " + groupName + " " + brokerName));
        String profileName = "mq-broker-"+groupName+"."+brokerName;
        return ContainerBuilder.child(fabricProxy, 2).withName("child").withProfiles(profileName).assertProvisioningResult().build();
    }


    void failOver(FabricService fabricService, String container) throws Exception {
        Container masterContainer = fabricService.getContainer(container);
        masterContainer.stop();
        masterContainer.start();
        Provision.provisioningSuccess(Arrays.asList(masterContainer), PROVISION_TIMEOUT);
    }


    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(mqDistributionConfiguration()),
                KarafDistributionOption.keepRuntimeFolder(),
                KarafDistributionOption.logLevel(LogLevelOption.LogLevel.INFO)
        };
    }
}
