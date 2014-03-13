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
package io.fabric8.itests.basic.examples;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.ContainerCondition;
import io.fabric8.itests.paxexam.support.ContainerProxy;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.itests.paxexam.support.Provision;

import java.util.Set;

import org.fusesource.jansi.AnsiString;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;


@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ExampleCamelCxfTest extends FabricTestSupport {


    @Test
    public void testExample() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            Set<ContainerProxy> containers = ContainerBuilder.create(fabricProxy).withName("child").withProfiles("example-camel-cxf").assertProvisioningResult().build();
            try {
                System.err.println(executeCommand("fabric:container-list"));

                Assert.assertTrue(Provision.waitForCondition(containers, new ContainerCondition() {
                    @Override
                    public Boolean checkConditionOnContainer(final Container c) {
                        String response = new AnsiString(executeCommand("fabric:container-connect -u admin -p admin "+c.getId()+" camel:route-list | grep fabric-camel-cxf")).getPlain().toString();
                        return response.contains("fabric-camel-cxf");
                    }
                }, 60000L));

                for (Container container : containers) {
                    System.err.println(executeCommand("fabric:container-connect -u admin -p admin " + container.getId() + " osgi:list"));
                    String response = executeCommand("fabric:container-connect -u admin -p admin " + container.getId() + " camel:route-list | grep fabric-camel-cxf");
                    System.err.println(response);
                    Assert.assertTrue(response.contains("Started"));
                }
            } finally {
                ContainerBuilder.destroy(containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

    @Configuration
    public Option[] config() {
        return OptionUtils.combine(
                fabricDistributionConfiguration()
        );
    }
}
