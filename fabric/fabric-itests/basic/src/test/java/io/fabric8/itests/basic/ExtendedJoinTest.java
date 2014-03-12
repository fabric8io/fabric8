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
package io.fabric8.itests.basic;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceLocator;
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.ContainerProxy;
import io.fabric8.itests.paxexam.support.FabricEnsembleTest;
import io.fabric8.itests.paxexam.support.Provision;

import java.util.Arrays;

import javax.inject.Inject;

import org.apache.karaf.admin.AdminService;
import org.apache.karaf.tooling.exam.options.KarafDistributionOption;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.osgi.framework.BundleContext;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ExtendedJoinTest extends FabricEnsembleTest {

    private static final String WAIT_FOR_JOIN_SERVICE = "wait-for-service io.fabric8.boot.commands.service.JoinAvailable";

    @Inject
    BundleContext bundleContext;

	@After
	public void tearDown() throws InterruptedException {
	}

	/**
	 * This is a test for FABRIC-353.
	 */
	@Test
	public void testJoinAndAddToEnsemble() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            AdminService adminService = ServiceLocator.awaitService(bundleContext, AdminService.class);
            String version = System.getProperty("fabric.version");
            System.err.println(executeCommand("admin:create --featureURL mvn:io.fabric8/fabric8-karaf/" + version + "/xml/features --feature fabric-git --feature fabric-agent --feature fabric-boot-commands child1"));
            System.err.println(executeCommand("admin:create --featureURL mvn:io.fabric8/fabric8-karaf/" + version + "/xml/features --feature fabric-git --feature fabric-agent --feature fabric-boot-commands child2"));
            try {
                System.err.println(executeCommand("admin:start child1"));
                System.err.println(executeCommand("admin:start child2"));
                Provision.instanceStarted(bundleContext, Arrays.asList("child1", "child2"), PROVISION_TIMEOUT);
                System.err.println(executeCommand("admin:list"));
                String joinCommand = "fabric:join -f --zookeeper-password "+ fabricService.getZookeeperPassword() +" " + fabricService.getZookeeperUrl();

                String response = "";
                for (int i = 0; i < 10 && !response.contains("true"); i++) {
                    response = executeCommand("ssh -l admin -P admin -p " + adminService.getInstance("child1").getSshPort() + " localhost " + WAIT_FOR_JOIN_SERVICE);
                    Thread.sleep(1000);
                }
                response = "";
                for (int i = 0; i < 10 && !response.contains("true"); i++) {
                    response = executeCommand("ssh -l admin -P admin -p " + adminService.getInstance("child2").getSshPort() + " localhost " + WAIT_FOR_JOIN_SERVICE);
                    Thread.sleep(1000);
                }

                System.err.println(executeCommand("ssh -l admin -P admin -p " + adminService.getInstance("child1").getSshPort() + " localhost " + joinCommand));
                System.err.println(executeCommand("ssh -l admin -P admin -p " + adminService.getInstance("child2").getSshPort() + " localhost " + joinCommand));
                Provision.containersExist(bundleContext, Arrays.asList("child1", "child2"), PROVISION_TIMEOUT);
                Container child1 = ContainerProxy.wrap(fabricService.getContainer("child1"), fabricProxy);
                Container child2 = ContainerProxy.wrap(fabricService.getContainer("child2"), fabricProxy);
                Provision.containersStatus(Arrays.asList(child1, child2), "success", PROVISION_TIMEOUT);
                addToEnsemble(fabricService, child1, child2);
                System.err.println(executeCommand("fabric:container-list"));
                removeFromEnsemble(fabricService, child1, child2);
                System.err.println(executeCommand("fabric:container-list"));
            } finally {
                System.err.println(executeCommand("admin:stop child1"));
                System.err.println(executeCommand("admin:stop child2"));
            }
        } finally {
            fabricProxy.close();
        }
	}


	@Configuration
	public Option[] config() {
		return new Option[]{
				new DefaultCompositeOption(fabricDistributionConfiguration()),
				KarafDistributionOption.editConfigurationFilePut("etc/system.properties", "karaf.name", "myroot"),
				KarafDistributionOption.editConfigurationFilePut("etc/system.properties", "fabric.version", MavenUtils.getArtifactVersion("io.fabric8", "fabric8-karaf"))
		};
	}
}
