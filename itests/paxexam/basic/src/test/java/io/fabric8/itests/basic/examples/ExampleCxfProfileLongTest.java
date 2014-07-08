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

import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceLocator;
import io.fabric8.api.ServiceProxy;
import io.fabric8.demo.cxf.Hello;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.ContainerProxy;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Constants;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.OptionUtils.combine;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
@Ignore("[FABRIC-1095] Fix basic ExampleCxfProfileLongTest")
//CXF Server fails provision cleanly in Karaf (restart the container and it works) due: Caused by: java.io.IOException: Cannot find any registered HttpDestinationFactory from the Bus.")
public class ExampleCxfProfileLongTest extends FabricTestSupport {

    @Before
    public void setUp() throws Exception {
        System.out.println(executeCommand("fabric:create -n --wait-for-provisioning"));
    }

    @Test
    public void testExample() throws Exception {
        System.out.println("creating the cxf-server container.");
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            Set<ContainerProxy> containers = ContainerBuilder.create(fabricProxy).withName("child").withProfiles("example-cxf-cxf.server").assertProvisioningResult().build();
            try {
                assertTrue("We should create the cxf-server container.", containers.size() == 1);
                System.out.println("created the cxf-server container.");
                // install bundle of CXF
                Thread.sleep(2000);
                System.out.println(executeCommand("fabric:cluster-list"));
                // install bundle of CXF
                Thread.sleep(2000);
                // calling the client here

                System.out.println("install the cxf client demo in root container");
                // This test need to take sometime to download the cxf feature related bundles
                System.out.println(executeCommand("features:install fabric-cxf", 600000, false));
                String projectVersion = System.getProperty("fabricitest.version");
                // install bundle of CXF demo client
                System.out.println(executeCommand("osgi:install -s mvn:io.fabric8.examples/fabric-cxf-demo-client/" + projectVersion));
                System.out.println(executeCommand("osgi:list"));

                System.out.println("invoking the web service");
                Hello proxy = ServiceLocator.awaitService(bundleContext, Hello.class);
                assertNotNull(proxy);
                String result1 = proxy.sayHello();
                String result2 = proxy.sayHello();
                assertNotSame("We should get the two different result", result1, result2);
            } finally {
                ContainerBuilder.destroy(containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

    @Configuration
    public Option[] config() {
        return combine(
                fabricDistributionConfiguration(),
                mavenBundle("io.fabric8.examples", "fabric-cxf-demo-common"),
                // Passing the system property to the test container
                systemProperty("fabricitest.version").value(System.getProperty("fabricitest.version"))
        );
    }

    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*,org.apache.felix.service.*;status=provisional");
        probe.setHeader(Constants.IMPORT_PACKAGE, "io.fabric8.api,io.fabric8.demo.cxf,io.fabric8.tooling.testing.pax.exam.karaf,*");
        return probe;
    }
}
