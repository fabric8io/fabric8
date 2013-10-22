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
package org.fusesource.fabric.itests.basic.examples;

import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.itests.paxexam.support.ContainerBuilder;
import org.fusesource.fabric.itests.paxexam.support.FabricTestSupport;
import org.fusesource.jansi.AnsiString;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.ops4j.pax.exam.Option;

import static org.ops4j.pax.exam.OptionUtils.combine;

import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import java.util.Set;


@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
@Ignore("[FABRIC-590] Fix fabric/fabric-itests/fabric-itests-basic")
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

        for (Container container : containers) {
            System.err.println(executeCommand("fabric:container-connect -u admin -p admin "+container.getId()+" osgi:list"));
            System.err.println(executeCommand("fabric:container-connect -u admin -p admin "+container.getId()+" camel:route-list"));
            String response = new AnsiString(executeCommand("fabric:container-connect -u admin -p admin "+container.getId()+" camel:route-list | grep fabric-camel-cxf")).getPlain().toString();
            Assert.assertTrue(response.contains("fabric-camel-cxf"));
        }
    }

    @Configuration
    public Option[] config() {
        return combine(
                fabricDistributionConfiguration()
        );
    }
}
