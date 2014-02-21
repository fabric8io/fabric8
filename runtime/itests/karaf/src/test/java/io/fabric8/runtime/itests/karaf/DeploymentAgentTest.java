package io.fabric8.runtime.itests.karaf;

import io.fabric8.api.Container;
import io.fabric8.runtime.itests.support.CommandSupport;
import io.fabric8.runtime.itests.support.ContainerBuilder;
import io.fabric8.runtime.itests.support.FabricTestSupport;
import io.fabric8.runtime.itests.support.Provision;

import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

@Ignore("[FABRIC-819] Provide initial set of portable fabric smoke tests")
public class DeploymentAgentTest {

	/**
	 * The purpose of this test is to make sure that everything can be downloaded from the fabric-maven-proxy.
	 * Also we want to make sure that after artifacts have been downloaded can be properlly used, for example:
	 * Feature Repositories can be properly resolved.
	 *
	 * Note: This test makes sense to run using remote containers that have an empty maven repo.
	 *
	 * http://fusesource.com/issues/browse/FABRIC-398
	 */
	@Test
	public void testFeatureRepoResolution() throws Exception {
		System.out.println(CommandSupport.executeCommand("fabric:create -n"));

		//We are just want to use a feature repository that is not part of the distribution.
		System.out.println(CommandSupport.executeCommand("fabric:profile-create --parents feature-camel test-profile"));
		System.out.println(CommandSupport.executeCommand("fabric:version-create --parent 1.0 1.1"));
		System.out.println(CommandSupport.executeCommand("fabric:profile-edit --repositories mvn:org.fusesource.examples.fabric-camel-dosgi/features/" + System.getProperty("fabric.version") + "/xml/features test-profile 1.1"));
		System.out.println(CommandSupport.executeCommand("fabric:profile-edit --features fabric-dosgi test-profile 1.1"));
		//We remove all repositories from agent config but the maven central to rely on the fabric-maven-proxy.
	    //Also remove local repository
		System.out.println(CommandSupport.executeCommand("profile-edit --pid io.fabric8.agent/org.ops4j.pax.url.mvn.repositories=http://repo1.maven.org/maven2@id=m2central default 1.1"));
		System.out.println(CommandSupport.executeCommand("fabric:profile-edit --pid test-profile 1.1"));

        Set<Container> containers = ContainerBuilder.create().withName("cnt").withProfiles("test-profile").assertProvisioningResult().build();
		try {
	        //We want to remove all repositories from fabric-agent.
	        for (Container container : containers) {
	            System.out.println(CommandSupport.executeCommand("fabric:container-upgrade 1.1 " + container.getId()));
	            System.out.flush();
	        }
	        Provision.provisioningSuccess(containers, FabricTestSupport.PROVISION_TIMEOUT);
	        System.out.println(CommandSupport.executeCommand("fabric:container-list"));

	        for (Container container : containers) {
	            System.out.println(CommandSupport.executeCommand("fabric:container-connect -u admin -p admin " + container.getId() + " osgi:list"));
	            System.out.println(CommandSupport.executeCommand("fabric:container-connect -u admin -p admin " + container.getId() + " config:proplist --pid org.ops4j.pax.url.mvn"));
	            System.out.flush();
	        }
		} finally {
            ContainerBuilder.destroy(containers);
		}
	}
}
