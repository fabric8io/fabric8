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

import io.fabric8.api.ServiceProxy;

import java.util.concurrent.CountDownLatch;
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
public class MQFabricTest extends MQTestSupport {

    @Test
    public void testLocalFabricCluster() throws Exception {
        final String groupName = "default";

        System.out.println(executeCommand("fabric:create -n --clean root"));
        ServiceProxy<CuratorFramework> curatorProxy = ServiceProxy.createServiceProxy(bundleContext, CuratorFramework.class);
        try {
            CuratorFramework curator = curatorProxy.getService();
            final CountDownLatch serviceLatch = new CountDownLatch(1);
            final FabricDiscoveryAgent discoveryAgent = new FabricDiscoveryAgent();

            discoveryAgent.setCurator(curator);
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
            Assert.assertTrue(serviceLatch.await(15, TimeUnit.MINUTES));
            System.out.println(executeCommand("fabric:cluster-list"));
        } finally {
            curatorProxy.close();
        }
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
