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


import java.util.Set;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.demo.cxf.Hello;
import org.fusesource.fabric.itests.paxexam.support.ContainerBuilder;
import org.fusesource.fabric.itests.paxexam.support.FabricTestSupport;
import static org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator.getOsgiService;
import org.junit.After;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import org.ops4j.pax.exam.Option;
import static org.ops4j.pax.exam.OptionUtils.combine;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.junit.ProbeBuilder;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.osgi.framework.Constants;



@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ExampleCxfProfileLongTest extends FabricTestSupport {
    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }

    @Before
    public void setUp() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        // This test need to take sometime to download the cxf feature related bundles
        System.err.println(executeCommand("features:install fabric-cxf", 600000, false));
        String projectVersion = System.getProperty("fabricitest.version");
        // install bundle of CXF demo client
        System.err.println(executeCommand("osgi:install -s mvn:org.fusesource.examples/fabric-cxf-demo-client/" + projectVersion));
        System.err.println(executeCommand("osgi:list"));
        System.err.println(executeCommand("packages:imports 141"));

    }

    @Test
    public void testExample() throws Exception {


        System.err.println("creating the cxf-server container.");
        Set<Container> containers = ContainerBuilder.create().withName("child").withProfiles("example-cxf").assertProvisioningResult().build();
        assertTrue("We should create the cxf-server container.", containers.size() ==1);
        System.err.println("created the cxf-server container.");
        // install bundle of CXF
        Thread.sleep(2000);
        System.err.println(executeCommand("fabric:cluster-list"));
        // install bundle of CXF
        Thread.sleep(2000);
        // calling the client here
        Hello proxy = getOsgiService(Hello.class);
        assertNotNull(proxy);
        String result1 = proxy.sayHello();
        String result2 = proxy.sayHello();
        assertNotSame("We should get the two different result", result1, result2);
    }

    @Configuration
    public Option[] config() {
        return combine(
                fabricDistributionConfiguration(),
                mavenBundle("org.fusesource.examples", "fabric-cxf-demo-common"),
                // Passing the system property to the test container
                systemProperty("fabricitest.version").value(System.getProperty("fabricitest.version"))
        );
    }

    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*,org.apache.felix.service.*;status=provisional");
        probe.setHeader(Constants.IMPORT_PACKAGE, "org.fusesource.fabric.demo.cxf,org.fusesource.tooling.testing.pax.exam.karaf,*");
        return probe;
    }
}
