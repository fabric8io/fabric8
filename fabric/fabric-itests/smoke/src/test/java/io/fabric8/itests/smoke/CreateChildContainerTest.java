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

package io.fabric8.itests.smoke;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.ContainerProxy;
import io.fabric8.itests.paxexam.support.FabricTestSupport;

import java.util.Set;

import org.apache.karaf.tooling.exam.options.KarafDistributionOption;
import org.junit.Assert;
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
public class CreateChildContainerTest extends FabricTestSupport {

    @Test
    public void testContainerWithJvmOpts() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        String jvmopts = "-Xms512m -XX:MaxPermSize=512m -Xmx2048m -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5008";
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            Set<ContainerProxy> containers = ContainerBuilder.child(fabricProxy, 1).withName("child").withJvmOpts(jvmopts).assertProvisioningResult().build();
            try {
                Assert.assertEquals("One container", 1, containers.size());
                Container child = containers.iterator().next();
                Assert.assertEquals("child1", child.getId());
                Assert.assertEquals("root", child.getParent().getId());
            } finally {
                ContainerBuilder.destroy(containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

    /**
     * [FABRIC-822] Cannot create child container repeatedly
     */
    @Test
    public void testCreateChildContainerRepeatedly() throws Exception {

        System.err.println(executeCommand("fabric:create --clean -n"));
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            Set<ContainerProxy> containers = ContainerBuilder.child(fabricProxy, 1).withName("child").assertProvisioningResult().build();
            try {
                Assert.assertEquals("One container", 1, containers.size());
                Container child = containers.iterator().next();
                Assert.assertEquals("child1", child.getId());
                Assert.assertEquals("root", child.getParent().getId());
            } finally {
                ContainerBuilder.destroy(containers);
            }

            System.err.println(executeCommand("fabric:create --clean -n"));
            containers = ContainerBuilder.child(fabricProxy, 1).withName("child").assertProvisioningResult().build();
            try {
                Assert.assertEquals("One container", 1, containers.size());
                Container child = containers.iterator().next();
                Assert.assertEquals("child1", child.getId());
                Assert.assertEquals("root", child.getParent().getId());
            } finally {
                ContainerBuilder.destroy(containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

    @Configuration
    public Option[] config() {
        return new Option[] { new DefaultCompositeOption(fabricDistributionConfiguration()),
                KarafDistributionOption.debugConfiguration("5005", false) };
    }
}