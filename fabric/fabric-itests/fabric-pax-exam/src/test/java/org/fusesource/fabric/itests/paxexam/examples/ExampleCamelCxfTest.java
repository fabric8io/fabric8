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

import org.fusesource.fabric.itests.paxexam.FabricTestSupport;
import org.fusesource.jansi.AnsiString;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.editConfigurationFilePut;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import static org.ops4j.pax.exam.OptionUtils.combine;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;


@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ExampleCamelCxfTest extends FabricTestSupport {
    @After
    public void tearDown() throws InterruptedException {
        destroyChildContainer("child1");
    }


    @Test
    public void testExample() throws Exception {
		String version = System.getProperty("fabric.version");
        System.err.println(executeCommand("fabric:create -n"));
		System.err.println(executeCommand("fabric:profile-create --parents camel example-camel-cxf"));
		System.err.println(executeCommand("fabric:profile-edit --repositories mvn:org.fusesource.fabric.examples/fabric-camel-cxf/"+version+"/xml/features example-camel-cxf"));
		System.err.println(executeCommand("fabric:profile-edit --features fabric-camel-cxf example-camel-cxf"));
        createAndAssertChildContainer("child1", "root", "example-camel-cxf");
		System.err.println(executeCommand("fabric:container-list"));
		Thread.sleep(5000);
		System.err.println(executeCommand("fabric:container-connect -u admin -p admin child1 osgi:list"));
		System.err.println(executeCommand("fabric:container-connect -u admin -p admin child1 camel:route-list"));
		String response = new AnsiString(executeCommand("fabric:container-connect -u admin -p admin child1 camel:route-list | grep fabric-camel-cxf")).getPlain().toString();
		Assert.assertTrue(response.contains("fabric-camel-cxf"));
	}

    @Configuration
    public Option[] config() {
        return combine(
                fabricDistributionConfiguration(),
                // Passing the system property to the test container
				editConfigurationFilePut("etc/system.properties", "fabric.version", MavenUtils.getArtifactVersion("org.fusesource.fabric", "fuse-fabric"))
        );
    }
}
