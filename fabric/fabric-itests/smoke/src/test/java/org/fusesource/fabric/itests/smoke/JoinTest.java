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
package org.fusesource.fabric.itests.smoke;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.karaf.admin.AdminService;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.itests.paxexam.support.FabricTestSupport;
import org.fusesource.fabric.itests.paxexam.support.Provision;
import org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class JoinTest extends FabricTestSupport {

    private static final String WAIT_FOR_JOIN_SERVICE = "wait-for-service org.fusesource.fabric.boot.commands.service.Join";

	@After
	public void tearDown() throws InterruptedException {
	}

	@Test
    @Ignore("[FABRIC-521] Fix fabric/fabric-itests/fabric-itests-smoke")
	public void testJoin() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        FabricService fabricService = getFabricService();
        AdminService adminService = ServiceLocator.getOsgiService(AdminService.class);
        String version = System.getProperty("fabric.version");
        System.err.println(executeCommand("admin:create --featureURL mvn:org.fusesource.fabric/fuse-fabric/" + version + "/xml/features --feature fabric-boot-commands child1"));
		try {
			System.err.println(executeCommand("admin:start child1"));
            Provision.instanceStarted(Arrays.asList("child1"), PROVISION_TIMEOUT);
            System.err.println(executeCommand("admin:list"));
            String joinCommand = "fabric:join -f --zookeeper-password "+ fabricService.getZookeeperPassword() +" " + fabricService.getZookeeperUrl();
            System.err.println(executeCommand("ssh -l karaf -P karaf -p " + adminService.getInstance("child1").getSshPort() + " localhost " + WAIT_FOR_JOIN_SERVICE));
            System.err.println(executeCommand("ssh -l karaf -P karaf -p " + adminService.getInstance("child1").getSshPort() + " localhost " + joinCommand));
            Provision.containersExist(Arrays.asList("child1"), PROVISION_TIMEOUT);
            Container child1 = fabricService.getContainer("child1");
			waitForProvisionSuccess(child1, PROVISION_TIMEOUT, TimeUnit.MILLISECONDS);
			System.err.println(executeCommand("fabric:container-list"));
		} finally {
			System.err.println(executeCommand("admin:stop child1"));
		}
	}

	/**
	 * This is a test for FABRIC-353.
	 */
	@Test
    @Ignore("[FABRIC-521] Fix fabric/fabric-itests/fabric-itests-smoke")
	public void testJoinAndAddToEnsemble() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        FabricService fabricService = getFabricService();
        AdminService adminService = ServiceLocator.getOsgiService(AdminService.class);

		String version = System.getProperty("fabric.version");
        System.err.println(executeCommand("admin:create --featureURL mvn:org.fusesource.fabric/fuse-fabric/" + version + "/xml/features --feature fabric-boot-commands child1"));
        System.err.println(executeCommand("admin:create --featureURL mvn:org.fusesource.fabric/fuse-fabric/" + version + "/xml/features --feature fabric-boot-commands child2"));
		try {
			System.err.println(executeCommand("admin:start child1"));
			System.err.println(executeCommand("admin:start child2"));
            Provision.instanceStarted(Arrays.asList("child1", "child2"), PROVISION_TIMEOUT);
            System.err.println(executeCommand("admin:list"));
            String joinCommand = "fabric:join -f --zookeeper-password "+ fabricService.getZookeeperPassword() +" " + fabricService.getZookeeperUrl();

            System.err.println(executeCommand("ssh -l karaf -P karaf -p " + adminService.getInstance("child1").getSshPort() + " localhost " + WAIT_FOR_JOIN_SERVICE));
            System.err.println(executeCommand("ssh -l karaf -P karaf -p " + adminService.getInstance("child1").getSshPort() + " localhost " + joinCommand));
            System.err.println(executeCommand("ssh -l karaf -P karaf -p " + adminService.getInstance("child2").getSshPort() + " localhost " + WAIT_FOR_JOIN_SERVICE));
            System.err.println(executeCommand("ssh -l karaf -P karaf -p " + adminService.getInstance("child2").getSshPort() + " localhost " + joinCommand));
            Provision.containersExist(Arrays.asList("child1", "child2"), PROVISION_TIMEOUT);
			Container child1 = fabricService.getContainer("child1");
			Container child2 = fabricService.getContainer("child2");
			waitForProvisionSuccess(child1, PROVISION_TIMEOUT, TimeUnit.MILLISECONDS);
			waitForProvisionSuccess(child2, PROVISION_TIMEOUT, TimeUnit.MILLISECONDS);
			System.err.println(executeCommand("fabric:ensemble-add --force child1 child2"));
			Thread.sleep(5000);
            getCurator().getZookeeperClient().blockUntilConnectedOrTimedOut();
			System.err.println(executeCommand("fabric:container-list"));
			System.err.println(executeCommand("fabric:ensemble-remove --force child1 child2"));
			Thread.sleep(5000);
            getCurator().getZookeeperClient().blockUntilConnectedOrTimedOut();
			System.err.println(executeCommand("fabric:container-list"));
		} finally {
			System.err.println(executeCommand("admin:stop child1"));
			System.err.println(executeCommand("admin:stop child2"));
		}
	}


	@Configuration
	public Option[] config() {
		return new Option[]{
				new DefaultCompositeOption(fabricDistributionConfiguration()),
				//debugConfiguration("5005", false),
				editConfigurationFilePut("etc/system.properties", "karaf.name", "myroot"),
				editConfigurationFilePut("etc/system.properties", "fabric.version", MavenUtils.getArtifactVersion("org.fusesource.fabric", "fuse-fabric"))
		};
	}
}
