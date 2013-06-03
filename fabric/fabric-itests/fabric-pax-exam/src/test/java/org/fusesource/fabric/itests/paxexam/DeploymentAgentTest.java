package org.fusesource.fabric.itests.paxexam;

import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.itests.paxexam.support.ContainerBuilder;
import org.fusesource.fabric.itests.paxexam.support.Provision;
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

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.debugConfiguration;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.editConfigurationFilePut;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class DeploymentAgentTest extends FabricTestSupport {

	@After
	public void tearDown() throws InterruptedException {
		ContainerBuilder.destroy();
	}

	/**
	 * The purpose of this test is to make sure that everything can be downloaded from the fabric-maven-proxy.
	 * Also we want to make sure that after artifacts have been downloaded can be properlly used, for example:
	 * Feature Repositories can be properly resolved.
	 *
	 * Note: This test makes sense to run using remote containers that have an empty maven repo.
	 *
	 * http://fusesource.com/issues/browse/FABRIC-398
	 *
	 * @throws Exception
	 */
	@Test
	public void testFeatureRepoResolution() throws Exception {
		System.out.println(executeCommand("fabric:create -n"));
		//We are just want to use a feature repository that is not part of the distribution.
		System.out.println(executeCommand("fabric:profile-create --parents camel test-profile"));
		System.out.println(executeCommand("fabric:version-create --parent 1.0 1.1"));
		System.out.println(executeCommand("fabric:profile-edit --repositories mvn:org.fusesource.examples.fabric-camel-cluster/features/" + System.getProperty("fabric.version") + "/xml/features test-profile 1.1"));
		System.out.println(executeCommand("fabric:profile-edit --features camel-server test-profile 1.1"));
		//We remove all repositories from agent config but the maven central to rely on the fabric-maven-proxy.
	    //Also remove local repository
		System.out.println(executeCommand("profile-edit --pid org.fusesource.fabric.agent/org.ops4j.pax.url.mvn.repositories=http://repo1.maven.org/maven2@id=m2central default 1.1"));
		System.out.println(executeCommand("fabric:profile-edit --pid test-profile 1.1"));

		Set<Container> containers = ContainerBuilder.create().withName("cnt").withProfiles("test-profile").assertProvisioningResult().build();

		//We want to remove all repositories from fabric-agent.
		for (Container container : containers) {
			System.out.println(executeCommand("fabric:container-upgrade 1.1 " + container.getId()));
			System.out.flush();
		}
		Provision.assertSuccess(containers, PROVISION_TIMEOUT);
		System.out.println(executeCommand("fabric:container-list"));

		for (Container container : containers) {
			System.out.println(executeCommand("fabric:container-connect -u admin -p admin " + container.getId() + " osgi:list"));
			System.out.println(executeCommand("fabric:container-connect -u admin -p admin " + container.getId() + " config:proplist --pid org.ops4j.pax.url.mvn"));
			System.out.flush();
		}
	}

	/**
	 * The purpose of this test is to make sure that fabs can be downloaded before they are resolved by the fabric-maven-proxy.
	 * http://fusesource.com/issues/browse/FABRIC-397
	 * Note: This test is better run using remote containers that have an empty maven repo.
	 *
	 * @throws Exception
	 */
	@Test
	public void testFab() throws Exception {
		System.out.println(executeCommand("fabric:create -n"));
		//We are just want to use a feature repository that is not part of the distribution.
		System.out.println(executeCommand("fabric:profile-create --parents camel test-profile"));
		System.out.println(executeCommand("fabric:version-create --parent 1.0 1.1"));
		System.out.println(executeCommand("fabric:profile-edit --fabs mvn:org.jboss.fuse.examples/cbr/" + System.getProperty("fabric.version") + " test-profile 1.1"));
		//We remove all repositories from agent config but the maven central to rely on the fabric-maven-proxy.
		//Also remove local repository
		System.out.println(executeCommand("profile-edit --pid org.fusesource.fabric.agent/org.ops4j.pax.url.mvn.repositories=http://repo1.maven.org/maven2@id=m2central default 1.1"));
		System.out.println(executeCommand("profile-edit --pid org.ops4j.pax.url.mvn/org.ops4j.pax.url.mvn.localRepository=file:tmp2@id=tmpRepo default 1.1"));
		System.out.println(executeCommand("fabric:profile-edit --pid test-profile 1.1"));

		Set<Container> containers = ContainerBuilder.create().withName("cnt").withProfiles("test-profile").assertProvisioningResult().build();

		//We want to remove all repositories from fabric-agent.
		for (Container container : containers) {
			System.out.println(executeCommand("fabric:container-upgrade 1.1 " + container.getId()));
			System.out.flush();
		}
		Provision.assertSuccess(containers, PROVISION_TIMEOUT);
		System.out.println(executeCommand("fabric:container-list"));

		for (Container container : containers) {
			System.out.println(executeCommand("fabric:container-connect -u admin -p admin " + container.getId() + " osgi:list"));
			System.out.println(executeCommand("fabric:container-connect -u admin -p admin " + container.getId() + " config:proplist --pid org.ops4j.pax.url.mvn"));
			System.out.flush();
		}
	}

	/**
	 * The purpose of this test is to make sure that fabs can be downloaded before they are resolved by the fabric-maven-proxy.
	 * This test uses fabs defines inside features.
	 * http://fusesource.com/issues/browse/FABRIC-397
	 * Note: This test is better run using remote containers that have an empty maven repo.
	 *
	 * @throws Exception
	 */
	@Test
	public void testFabFromFeature() throws Exception {
		System.out.println(executeCommand("fabric:create -n"));
		//We are just want to use a feature repository that is not part of the distribution.
		System.out.println(executeCommand("fabric:profile-create --parents camel test-profile"));
		System.out.println(executeCommand("fabric:version-create --parent 1.0 1.1"));
		System.out.println(executeCommand("fabric:profile-edit --repositories mvn:org.jboss.fuse.examples/project/" + System.getProperty("fabric.version") + "/xml/features test-profile 1.1"));
		System.out.println(executeCommand("fabric:profile-edit --features example-cbr test-profile 1.1"));

		//We remove all repositories from agent config but the maven central to rely on the fabric-maven-proxy.
		//Also remove local repository
		System.out.println(executeCommand("profile-edit --pid org.fusesource.fabric.agent/org.ops4j.pax.url.mvn.repositories=http://repo1.maven.org/maven2@id=m2central default 1.1"));
		System.out.println(executeCommand("profile-edit --pid org.ops4j.pax.url.mvn/org.ops4j.pax.url.mvn.localRepository=file:tmp2@id=tmpRepo default 1.1"));
		System.out.println(executeCommand("fabric:profile-edit --pid test-profile 1.1"));

		Set<Container> containers = ContainerBuilder.create().withName("cnt").withProfiles("test-profile").assertProvisioningResult().build();

		//We want to remove all repositories from fabric-agent.
		for (Container container : containers) {
			System.out.println(executeCommand("fabric:container-upgrade 1.1 " + container.getId()));
			System.out.flush();
		}
		Provision.assertSuccess(containers, PROVISION_TIMEOUT);
		System.out.println(executeCommand("fabric:container-list"));

		for (Container container : containers) {
			System.out.println(executeCommand("fabric:container-connect -u admin -p admin " + container.getId() + " osgi:list"));
			System.out.println(executeCommand("fabric:container-connect -u admin -p admin " + container.getId() + " config:proplist --pid org.ops4j.pax.url.mvn"));
			System.out.flush();
		}
	}

	@Configuration
	public Option[] config() {
		return new Option[]{
				new DefaultCompositeOption(fabricDistributionConfiguration()),
				editConfigurationFilePut("etc/system.properties", "fabric.version", MavenUtils.asInProject().getVersion(GROUP_ID, ARTIFACT_ID)),
				debugConfiguration("5005", false)
		};
	}
}
