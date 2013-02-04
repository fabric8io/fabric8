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
import org.fusesource.fabric.api.ZooKeeperClusterService;
import org.fusesource.fabric.zookeeper.IZKClient;
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

import java.util.List;

import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.debugConfiguration;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class EnsembleTest extends FabricTestSupport {

    @After
    public void tearDown() throws InterruptedException {
        destroyChildContainer("child1");
        destroyChildContainer("child2");
    }

    @Test
    public void testAddAndRemove() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        createAndAssertChildContainer("child1", "root", "default");
        createAndAssertChildContainer("child2", "root", "default");
        for (int i = 0; i < 3; i++) {
            System.err.println(executeCommand("fabric:ensemble-add child1 child2"));
            ZooKeeperClusterService service = getOsgiService(ZooKeeperClusterService.class);
            List<String> containers = service.getEnsembleContainers();
            Assert.assertTrue(containers.contains("child1"));
            Assert.assertTrue(containers.contains("child2"));
            IZKClient zookeeper = getOsgiService(IZKClient.class);
            Thread.sleep(5000);
            zookeeper.waitForConnected();
            System.err.println(executeCommand("fabric:container-list"));
            System.err.println(executeCommand("fabric:ensemble-remove child1 child2"));
            Thread.sleep(5000);
            zookeeper.waitForConnected();
            System.err.println(executeCommand("fabric:container-list"));
            containers = service.getEnsembleContainers();
            Assert.assertFalse(containers.contains("child1"));
            Assert.assertFalse(containers.contains("child2"));
        }
    }

    @Test
    public void testAddAndRemoveWithVersions() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        createAndAssertChildContainer("child1", "root", "default");
        createAndAssertChildContainer("child2", "root", "default");
        System.err.println(executeCommand("fabric:version-create"));
        System.err.println(executeCommand("fabric:container-upgrade --all 1.1"));
        System.err.println(executeCommand("fabric:ensemble-add child1 child2"));
        ZooKeeperClusterService service = getOsgiService(ZooKeeperClusterService.class);
        List<String> containers = service.getEnsembleContainers();
        Assert.assertTrue(containers.contains("child1"));
        Assert.assertTrue(containers.contains("child2"));
        IZKClient zookeeper = getOsgiService(IZKClient.class);
        Thread.sleep(5000);
        zookeeper.waitForConnected();
        System.err.println(executeCommand("fabric:container-list"));
        System.err.println(executeCommand("fabric:ensemble-remove child1 child2"));
        Thread.sleep(5000);
        zookeeper.waitForConnected();
        System.err.println(executeCommand("fabric:container-list"));
        containers = service.getEnsembleContainers();
        Assert.assertFalse(containers.contains("child1"));
        Assert.assertFalse(containers.contains("child2"));
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                debugConfiguration("5005", false)
        };
    }
}
