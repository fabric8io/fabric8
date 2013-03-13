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

import org.fusesource.fabric.demo.cxf.client.Client;
import org.fusesource.fabric.itests.paxexam.FabricTestSupport;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import static org.junit.Assert.assertNotSame;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;


@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ExampleCxfProfileTest extends FabricTestSupport {
    @After
    public void tearDown() throws InterruptedException {
        destroyChildContainer("cxf-server");
    }

    @Test
    @Ignore
    public void testExample() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        createAndAssertChildContainer("cxf-server", "root", "example-cxf");
        System.err.println(executeCommand("fabric:cluster-list"));
        // install bundle of CXF
        System.err.println(executeCommand("features:install fabric-cxf"));
        String projectVersion = System.getProperty("fabricitest.version");
        // install bundle of CXF demo client
        System.err.println(executeCommand("osgi:install -s mvn:org.fusesource.examples/fabric-cxf-demo-client/" + projectVersion));
        // calling the client here
        Client client = new Client();
        String result1 = client.getProxy().sayHello();
        String result2 = client.getProxy().sayHello();
        assertNotSame("We should get the two different result", result1, result2);
    }

    @Configuration
    public Option[] config() {
        return combine(
                fabricDistributionConfiguration(),
                // Passing the system property to the test container
                systemProperty("fabricitest.version").value(System.getProperty("fabricitest.version"))
        );


    }
}
