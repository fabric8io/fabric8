/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.itests.basic.examples;

import java.util.Set;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.ContainerCondition;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.itests.paxexam.support.Provision;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class ExampleCamelProfileTest extends FabricTestSupport {

    @Test
    public void testExample() throws Exception {
        System.out.println(executeCommand("fabric:create -n --wait-for-provisioning"));
        //System.out.println(executeCommand("shell:info"));
        //System.out.println(executeCommand("fabric:info"));
        //System.out.println(executeCommand("fabric:profile-list"));

        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            Set<Container> containers = ContainerBuilder.create(fabricProxy, 1).withName("cnt").withProfiles("quickstarts-karaf-beginner-camel.log").assertProvisioningResult().build();
            Assert.assertEquals("Should be 1 container", 1, containers.size());
            try {
                Assert.assertTrue(Provision.waitForCondition(containers, new ContainerCondition() {
                    @Override
                    public Boolean checkConditionOnContainer(final Container c) {
                        System.out.println(executeCommand("fabric:container-connect -u admin -p admin " + c.getId() + " osgi:list"));
                        System.out.println(executeCommand("fabric:container-connect -u admin -p admin " + c.getId() + " camel:route-list"));
                        String completed = executeCommand("fabric:container-connect -u admin -p admin " + c.getId() + " camel:route-info log-route | grep \"Exchanges Completed\"");
                        System.out.println(completed);
                        if (completed.contains("Exchanges Completed") && !completed.contains("Exchanges Completed: 0")) {
                            return true;
                        } else {
                            return false;
                        }

                    }
                }, 10000L));
            } finally {
                ContainerBuilder.destroy(containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                //debugConfiguration("5005",false)
        };
    }
}
