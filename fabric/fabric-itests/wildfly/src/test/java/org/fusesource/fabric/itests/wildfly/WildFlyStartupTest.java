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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.itests.paxexam.support.ContainerBuilder;
import org.fusesource.fabric.utils.SystemProperties;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class WildFlyStartupTest extends WildFlyTestSupport {

	@Test
	public void testWildFlyProcess() throws Exception {

		executeCommand("fabric:create -n");
		Container container = getFabricService().getContainers()[0];
		Assert.assertEquals("Expected to find the root container", "root", container.getId());

		container = ContainerBuilder.child(1).withName("child").assertProvisioningResult().build().iterator().next();
		try {
			Assert.assertEquals("Expected to find the child container", "child1", container.getId());

			// Add the WildFly profile and start the process
			executeCommand("container-add-profile child1 controller-wildfly");
			waitForProvisionSuccess(container);

			// FIXME: [FABRIC-541] process-list broken for remote containers
			// String response = executeCommand("process-list child1");

			SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss:SSS");

			// FIXME: [FABRIC-543] Provide a Process API that allows testing of managed processes
			File childHome = new File(System.getProperty("karaf.home") + "/instances/" + container.getId());
			Assert.assertTrue("[" + df.format(new Date()) + "] Child home exists: " + childHome, childHome.exists());

			File procHome = new File(childHome + "/processes/1");
			System.out.println("[" + df.format(new Date()) + "] Waiting for: " + procHome);
			for (int i=0; !procHome.exists() && i < 30; i++) {
				Thread.sleep(1000);
			}
			Assert.assertTrue("Process home exists: " + procHome, procHome.exists());

			File wildflyHome = new File(procHome + "/wildfly-8.0.0.Alpha4");
			System.out.println("[" + df.format(new Date()) + "] Waiting for: " + wildflyHome);
			for (int i=0; !wildflyHome.exists() && i < 30; i++) {
				Thread.sleep(1000);
			}
			Assert.assertTrue("WildFly home exists: " + wildflyHome, wildflyHome.exists());

			File pidFile = new File(wildflyHome + "/standalone/data/wildfly.pid");
			System.out.println("[" + df.format(new Date()) + "] Waiting for: " + pidFile);
			for (int i=0; !pidFile.exists() && i < 30; i++) {
				Thread.sleep(1000);
			}
			Assert.assertTrue("PID file exists", pidFile.exists());

			BufferedReader pidr = new BufferedReader(new FileReader(pidFile));
			Long pid = new Long(pidr.readLine());
			Assert.assertNotNull("PID not null", pid);
			pidr.close();

			System.out.println("[" + df.format(new Date()) + "] WildFly PID: " + pid);

			// TODO Check that WildFly started up properly

			// Stop the WildFly process
			// FIXME: [FABRIC-542] executeCommand may only return the first line of the cmd output
			// FIXME: [FABRIC-544] Cannot stop managed process in child container
			// executeCommands("container-connect child1", "process:stop 1");
			// for (int i=0; pidFile.exists() && i < 10; i++) {
		    //		Thread.sleep(1000);
			// }
			// Assert.assertFalse("PID file removed", pidFile.exists());

			// Hack to stop the WildFly process brute force
			Runtime runtime = Runtime.getRuntime();
			runtime.exec("kill -9 " + pid);
			pidFile.delete();

		} finally {
			container.stop();
		}
	}

	@Configuration
	public Option[] config() {
		return new Option[] { new DefaultCompositeOption(fabricDistributionConfiguration()),
				//new VMOption("-agentlib:jdwp=transport=dt_socket,address=5005,server=y,suspend=y"),
				new VMOption("-D" + SystemProperties.ZOOKEEPER_PASSWORD + "=systempassword") };
	}
}
