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
package org.fusesource.esb.itests.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.ops4j.pax.exam.CoreOptions.scanFeatures;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceProxy;
import io.fabric8.groups.internal.ZooKeeperMultiGroup;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.itests.paxexam.support.Provision;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.curator.framework.CuratorFramework;
import org.fusesource.mq.fabric.FabricDiscoveryAgent;
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
@Ignore("[FABRIC-796] Fix esb basic EsbProfileRedeployTest")
public class EsbProfileRedeployTest extends FabricTestSupport {

    private long timeout = 60 * 1000L;

    @Test
    public void testProfileRedeploy() throws Exception {
        executeCommand("fabric:create -n");

        Set<Container> containers = ContainerBuilder.create(1).withName("node").withProfiles("jboss-fuse-full").assertProvisioningResult().build();
        try {
            Container node = containers.iterator().next();
            Provision.provisioningSuccess(Arrays.asList(node), PROVISION_TIMEOUT);

            ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
            ServiceProxy<CuratorFramework> curatorProxy = ServiceProxy.createServiceProxy(bundleContext, CuratorFramework.class);
            try {
                FabricService fabricService = fabricProxy.getService();
                CuratorFramework curator = curatorProxy.getService();

                final ZooKeeperMultiGroup group = new ZooKeeperMultiGroup<FabricDiscoveryAgent.ActiveMQNode>(curator, "/fabric/registry/clusters/fusemq/default", FabricDiscoveryAgent.ActiveMQNode.class);
                group.start();
                FabricDiscoveryAgent.ActiveMQNode master = null;
                Provision.waitForCondition(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        while ((FabricDiscoveryAgent.ActiveMQNode) group.master() == null) {
                            Thread.sleep(1000);
                        }
                        return true;
                    }
                }, timeout);

                master = (FabricDiscoveryAgent.ActiveMQNode)group.master();
                String masterContainer = master.getContainer();
                assertEquals("node1", masterContainer);

                for (int i = 0; i < 5; i++) {

                    Thread.sleep(5000);

                    executeCommand("container-remove-profile node1 jboss-fuse-full");
                    Provision.provisioningSuccess(Arrays.asList(fabricService.getContainers()), PROVISION_TIMEOUT);

                    Provision.waitForCondition(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            while ((FabricDiscoveryAgent.ActiveMQNode) group.master() != null) {
                                Thread.sleep(1000);
                            }
                            return true;
                        }
                    }, timeout);
                    master = (FabricDiscoveryAgent.ActiveMQNode) group.master();
                    assertNull(master);

                    Thread.sleep(5000);

                    executeCommand("container-add-profile node1 jboss-fuse-full");
                    Provision.provisioningSuccess(Arrays.asList(fabricService.getContainers()), PROVISION_TIMEOUT);

                    Provision.waitForCondition(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            while ((FabricDiscoveryAgent.ActiveMQNode) group.master() == null) {
                                Thread.sleep(1000);
                            }
                            return true;
                        }
                    }, timeout);

                    master = (FabricDiscoveryAgent.ActiveMQNode) group.master();
                    masterContainer = master.getContainer();
                    assertEquals("node1", masterContainer);

                }
            } finally {
                fabricProxy.close();
                curatorProxy.close();
            }
        } finally {
            ContainerBuilder.destroy(containers);
        }
    }

    @Configuration
   	public Option[] config() {
   		return new Option[]{
   				new DefaultCompositeOption(fabricDistributionConfiguration()),
                scanFeatures("default", "mq-fabric").start()
   		};
   	}

}
