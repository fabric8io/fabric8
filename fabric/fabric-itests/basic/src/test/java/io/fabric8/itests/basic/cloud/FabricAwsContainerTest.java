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

package io.fabric8.itests.basic.cloud;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.CoreOptions.scanFeatures;
import io.fabric8.api.ServiceLocator;
import io.fabric8.itests.paxexam.support.FabricTestSupport;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import com.google.common.base.Predicate;


@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class FabricAwsContainerTest extends FabricTestSupport {

	private String identity;
	private String credential;
	private String image;
	private String location;
	private String user;
	private String group = "fabricitests";

	/**
	 * Returns true if all the requirements for running this test are meet.
	 *
	 * @return
	 */
	public boolean isReady() {
		return
				identity != null && credential != null && image != null & user != null &&
						!identity.isEmpty() && !credential.isEmpty() && !image.isEmpty() && !user.isEmpty();
	}

	@Before
	public void setUp() {
		identity = System.getProperty("fabricitest.aws.identity");
		credential = System.getProperty("fabricitest.aws.credential");
		image = System.getProperty("fabricitest.aws.image");
		location = System.getProperty("fabricitest.aws.location");
		user = System.getProperty("fabricitest.aws.user");
	}


	@After
	public void tearDown() {
		if (isReady()) {
			System.err.println(executeCommand("group-destroy " + group, 30000L, false));
		}
	}

	/**
	 * Starts an ensemble server on EC2, configures the security groups and join the ensemble.
	 *
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@Test
	public void testAwsAgentCreation() throws InterruptedException, IOException {
		if (!isReady()) {
			System.err.println("Amazon is not setup correctly. This test will not run.");
			System.err.println("To properly run this test, you need to setup with maven the following properties:");
			System.err.println("fabricitest.aws.identity \t The aws access id");
			System.err.println("fabricitest.aws.credential \t The aws access key");
			System.err.println("fabricitest.aws.image  \t The aws (java ready) image");
			System.err.println("fabricitest.aws.user  \t The user under which the agent will run");
			return;
		}

		System.err.println(executeCommand("features:install jclouds-aws-ec2 fabric-jclouds jclouds-commands"));

        /*String fabricVersion = System.getProperty("fabric.version");
		if (fabricVersion != null && fabricVersion.contains("SNAPSHOT")) {
            System.err.println("Switching to snapshot repository");
            executeCommands("config:propset --pid io.fabric8.service defaultRepo https://repo.fusesource.com/nexus/content/groups/public-snapshots/");
        }*/

		//Filtering out regions because there is a temporary connectivity issue with us-west-2.
		executeCommand("fabric:cloud-service-add --provider aws-ec2 --identity " + identity + " --credential " + credential);
		ComputeService computeService = ServiceLocator.awaitService(bundleContext, ComputeService.class, 3, TimeUnit.MINUTES);

		//The compute service needs some time to properly initialize.
		//Thread.sleep(3 * DEFAULT_TIMEOUT);
		System.err.println(executeCommand("fabric:cloud-service-list"));
		System.err.println(executeCommand(String.format("fabric:container-create-cloud --name aws-ec2 --locationId %s --imageId %s --group %s --ensemble-server ensemble1", location, image, group), 10 * 60000L, false));
		String publicIp = getNodePublicIp(computeService);
		assertNotNull(publicIp);
		Thread.sleep(DEFAULT_TIMEOUT);
		System.err.println(executeCommand("fabric:join -n " + publicIp + ":2181", 10 * 60000L, false));
		String agentList = executeCommand("fabric:container-list");
		System.err.println(agentList);
		assertTrue(agentList.contains("root") && agentList.contains("ensemble1"));

	}

	/**
	 * Return the public ip of the generated node.
	 * It assumes that no other node is currently running using the current group.
	 *
	 * @return
	 */
	private String getNodePublicIp(ComputeService computeService) {

		for (ComputeMetadata computeMetadata : computeService.listNodesDetailsMatching(new Predicate<ComputeMetadata>() {
			@Override
			public boolean apply(ComputeMetadata metadata) {
				NodeMetadata nodeMetadata = (NodeMetadata) metadata;
				return nodeMetadata.getGroup().equals(group) && nodeMetadata.getStatus().equals(NodeMetadata.Status.RUNNING);
			}
		})) {
			NodeMetadata nodeMetadata = (NodeMetadata) computeMetadata;
			return nodeMetadata.getPublicAddresses().toArray(new String[0])[0];

		}
		return null;
	}


	@Configuration
	public Option[] config() {
		return new Option[]{
				new DefaultCompositeOption(fabricDistributionConfiguration()),
				//debugConfiguration("5005",true),
				copySystemProperty("fabricitest.aws.identity"),
				copySystemProperty("fabricitest.aws.credential"),
				copySystemProperty("fabricitest.aws.image"),
				copySystemProperty("fabricitest.aws.location"),
				copySystemProperty("fabricitest.aws.user"),
				editConfigurationFilePut("etc/system.properties", "fabric.version", MavenUtils.getArtifactVersion("io.fabric8", "fabric8-karaf")),
				editConfigurationFileExtend("etc/config.properties", "org.osgi.framework.executionenvironment", "JavaSE-1.7,JavaSE-1.6,JavaSE-1.5"),
				scanFeatures("jclouds", "jclouds-compute").start()
		};
	}
}
