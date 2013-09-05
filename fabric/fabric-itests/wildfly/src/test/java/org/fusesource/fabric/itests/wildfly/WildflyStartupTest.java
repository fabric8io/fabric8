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
package org.fusesource.fabric.itests.wildfly;

import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.utils.SystemProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import static junit.framework.Assert.assertEquals;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class WildflyStartupTest extends WildflyTestSupport {

	@Test
	public void testWildflyProcess() throws Exception {

		executeCommand("fabric:create");
		Container container = getFabricService().getContainers()[0];
		assertEquals("Expected to find the root container", "root", container.getId());

//      container = ContainerBuilder.child(1).withName("child").assertProvisioningResult().build().iterator().next();
//		assertEquals("Expected to find the child container", "child", container.getId());
//
//		waitForProvisionSuccess(container);
//
//		executeCommand("container-add-profile child controller-wildfly");
//		waitForProvisionSuccess(container);
//
//		Profile profile = getProfile(container, "controller-wildfly");
//		Assert.assertNotNull("Profile not null", profile);
	}

	@Configuration
	public Option[] config() {
		return new Option[] {
				new DefaultCompositeOption(fabricDistributionConfiguration()),
				// new VMOption("-agentlib:jdwp=transport=dt_socket,address=5005,server=y,suspend=y"),
				new VMOption("-D" + SystemProperties.ZOOKEEPER_PASSWORD + "=systempassword")
		};
	}
}
