/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.fusesource.fabric.itests.paxexam.examples;


import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.itests.paxexam.FabricTestSupport;
import org.fusesource.fabric.itests.paxexam.support.ContainerBuilder;
import org.fusesource.fabric.itests.paxexam.support.Provision;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import scala.actors.threadpool.Arrays;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ExampleCamelProfileTest extends FabricTestSupport {

    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }

    @Test
    public void testExample() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        IZKClient zooKeeper = getZookeeper();
        Set<Container> containers = ContainerBuilder.create(2).withName("cnt").withProfiles("default").assertProvisioningResult().build();
        Container brokerContainer = containers.iterator().next();
        containers.remove(brokerContainer);

        zooKeeper.setData(ZkPath.CONTAINER_PROVISION_RESULT.getPath(brokerContainer.getId()), "changing");
        System.err.println(executeCommand("fabric:container-change-profile " + brokerContainer.getId() + " mq"));
        Provision.assertSuccess(Arrays.asList(new Container[]{brokerContainer}), PROVISION_TIMEOUT);
        System.err.println(executeCommand("fabric:cluster-list"));

        for(Container c : containers) {
            zooKeeper.setData(ZkPath.CONTAINER_PROVISION_RESULT.getPath(c.getId()), "changing");
            System.err.println(executeCommand("fabric:container-change-profile " + c.getId() + " example-camel"));
        }
        Provision.assertSuccess(containers, PROVISION_TIMEOUT);

        int completedCount = 0;
        Thread.sleep(5000);
        for (Container c : containers) {
            System.err.println(executeCommand("fabric:container-connect -u admin -p admin "+c.getId()+" osgi:list"));
            System.err.println(executeCommand("fabric:container-connect -u admin -p admin "+c.getId()+" camel:route-list"));
            String completed = executeCommand("fabric:container-connect -u admin -p admin "+c.getId()+" camel:route-info route2 | grep \"Exchanges Completed\"");
            System.err.println(completed);
            if (completed.contains("Exchanges Completed") && !completed.contains("Exchanges Completed: 0")) {
                completedCount++;
            }
        }

        Assert.assertTrue("Expected at least 1 completed exchange. Found: "+ completedCount, completedCount > 0);
    }

    @Configuration
    public Option[] config() {
        return fabricDistributionConfiguration();
    }
}
