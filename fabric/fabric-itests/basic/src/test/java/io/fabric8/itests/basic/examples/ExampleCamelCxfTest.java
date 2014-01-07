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

import static junit.framework.Assert.assertTrue;
import static org.ops4j.pax.exam.OptionUtils.combine;

import java.util.Set;

import io.fabric8.api.Container;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.ContainerCondition;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.itests.paxexam.support.Provision;
import org.fusesource.jansi.AnsiString;
import org.junit.After;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;


@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
//@Ignore("[FABRIC-670] The test is prone to provisioning issue due to spring namespace handlers.")
public class ExampleCamelCxfTest extends FabricTestSupport {
    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }


    @Test
    public void testExample() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        Set<Container> containers = ContainerBuilder.create().withName("child").withProfiles("example-camel-cxf").assertProvisioningResult().build();
		System.err.println(executeCommand("fabric:container-list"));


        assertTrue(Provision.waitForCondition(containers, new ContainerCondition() {
            @Override
            public Boolean checkConditionOnContainer(final Container c) {
                String response = new AnsiString(executeCommand("fabric:container-connect -u admin -p admin "+c.getId()+" camel:route-list | grep fabric-camel-cxf")).getPlain().toString();
                return response.contains("fabric-camel-cxf");
            }
        }, 60000L));

        for (Container container : containers) {
            System.err.println(executeCommand("fabric:container-connect -u admin -p admin " + container.getId() + " osgi:list"));
            System.err.println(executeCommand("fabric:container-connect -u admin -p admin " + container.getId() + " camel:route-list"));
        }
    }

    @Configuration
    public Option[] config() {
        return combine(
                fabricDistributionConfiguration()
        );
    }
}
