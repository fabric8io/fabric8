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
package io.fabric8.itests.wildfly;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.ContainerProxy;
import io.fabric8.itests.paxexam.support.Provision;
import io.fabric8.utils.SystemProperties;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;
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
@Ignore("[FABRIC-574][7.4] Fix fabric wildfly WildFlyStartupTest")
public class WildFlyStartupTest extends WildFlyTestSupport {

	@Test
	public void testWildFlyProcess() throws Exception {

		executeCommand("fabric:create -n");
        Set<ContainerProxy> containers = null;
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            containers= ContainerBuilder.child(fabricProxy, 1).withName("child").assertProvisioningResult().build();
			Container childContainer = containers.iterator().next();
            Assert.assertEquals("Expected to find the child container", "child1", childContainer.getId());

			// Add the WildFly profile and start the process
			executeCommand("container-add-profile child1 controller-wildfly");
			Provision.containerStatus(containers, PROVISION_TIMEOUT);

			// FIXME: [FABRIC-541] process-list broken for remote containers
			// String response = executeCommand("process-list child1");

			SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss:SSS");

			// FIXME: [FABRIC-543] Provide a Process API that allows testing of managed processes
			File childHome = new File(System.getProperty("karaf.home") + "/instances/" + childContainer.getId());
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
		    ContainerBuilder.destroy(containers);
            fabricProxy.close();
		}
	}

	@Configuration
	public Option[] config() {
		return new Option[] { new DefaultCompositeOption(fabricDistributionConfiguration()),
				//new VMOption("-agentlib:jdwp=transport=dt_socket,address=5005,server=y,suspend=y"),
				new VMOption("-D" + SystemProperties.ZOOKEEPER_PASSWORD + "=systempassword") };
	}
}
