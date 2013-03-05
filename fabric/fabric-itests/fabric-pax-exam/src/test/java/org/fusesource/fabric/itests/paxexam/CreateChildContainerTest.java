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
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.debugConfiguration;
import static org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator.getOsgiService;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class CreateChildContainerTest extends FabricTestSupport {

    @Test
    public void testLocalChildCreation() throws Exception {
         System.err.println(executeCommand("fabric:create -n"));
         try {
         createAndAssertChildContainer("child1", "root", "default");
         } finally {
             destroyChildContainer("child1");
         }
    }

    /**
     * This is a test for: http://fusesource.com/issues/browse/FABRIC-370
     * @throws Exception
     */
    @Test
    public void testContainerDelete() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        createAndAssertChildContainer("child1", "root", "default");
        System.err.println(executeCommand("fabric:version-create"));
        destroyChildContainer("child1");
        IZKClient zooKeeper = getOsgiService(IZKClient.class);
        Assert.assertNull(zooKeeper.exists(ZkPath.CONFIG_VERSIONS_CONTAINER.getPath("1.1", "child1")));
        Assert.assertNull(zooKeeper.exists(ZkPath.CONFIG_VERSIONS_CONTAINER.getPath("1.0", "child1")));
        Assert.assertNull(zooKeeper.exists(ZkPath.CONTAINER.getPath("child1")));
        Assert.assertNull(zooKeeper.exists(ZkPath.CONTAINER_DOMAINS.getPath("child1")));
        Assert.assertNull(zooKeeper.exists(ZkPath.CONTAINER_PROVISION.getPath("child1")));
    }

    @Configuration
    public Option[] config() {
        return new Option[] {
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                //debugConfiguration("5005",false)
        };
    }
}
