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
package org.fusesource.fabric.itests.paxexam;

import junit.framework.Assert;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.ZooKeeperClusterService;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import javax.inject.Inject;
import java.util.List;

import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.debugConfiguration;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class JoinTest extends FabricTestSupport {

	@After
	public void tearDown() throws InterruptedException {
	}

	@Test
	public void testJoin() throws Exception {
        FabricService fabricService = getFabricService();
		String version = System.getProperty("fabric.version");
        System.err.println(executeCommand("fabric:create -n"));
		System.err.println(executeCommand("admin:create --java-opts \"-Dzookeeper.url=localhost:2181 -Dzookeeper.password=admin\" --featureURL mvn:org.fusesource.fabric/fuse-fabric/" + version + "/xml/features --feature fabric-agent child1"));
		try {
			System.err.println(executeCommand("admin:start child1"));
			Container child1 = fabricService.getContainer("child1");
			waitForProvisionSuccess(child1, PROVISION_TIMEOUT);
			System.err.println(executeCommand("fabric:container-list"));
		} finally {
			System.err.println(executeCommand("admin:stop child1"));
		}
	}

	/**
	 * This is a test for FABRIC-353.
	 *
	 * @throws Exception
	 */
	@Test
	public void testJoinAndAddToEnsemble() throws Exception {
        FabricService fabricService = getFabricService();
        System.err.println(executeCommand("fabric:create -n"));
        IZKClient zookeeper = getZookeeper();
		String version = System.getProperty("fabric.version");
		System.err.println(executeCommand("admin:create --java-opts \"-Dzookeeper.url=localhost:2181 -Dzookeeper.password=admin\" --featureURL mvn:org.fusesource.fabric/fuse-fabric/" + version + "/xml/features --feature fabric-agent child1"));
		System.err.println(executeCommand("admin:create --java-opts \"-Dzookeeper.url=localhost:2181 -Dzookeeper.password=admin\" --featureURL mvn:org.fusesource.fabric/fuse-fabric/" + version + "/xml/features --feature fabric-agent child2"));
		try {
			System.err.println(executeCommand("admin:start child1"));
			System.err.println(executeCommand("admin:start child2"));

			Container child1 = fabricService.getContainer("child1");
			Container child2 = fabricService.getContainer("child2");
			waitForProvisionSuccess(child1, PROVISION_TIMEOUT);
			waitForProvisionSuccess(child2, PROVISION_TIMEOUT);
			System.err.println(executeCommand("fabric:ensemble-add child1 child2"));
			Thread.sleep(5000);
			zookeeper.waitForConnected();
			System.err.println(executeCommand("fabric:container-list"));
			System.err.println(executeCommand("fabric:ensemble-remove child1 child2"));
			Thread.sleep(5000);
			zookeeper.waitForConnected();
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
