package io.fabric8.itests.smoke;

import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.editConfigurationFilePut;

import java.util.Set;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.ContainerProxy;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.itests.paxexam.support.Provision;
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

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class DeploymentAgentTest extends FabricTestSupport {

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
		System.out.println(executeCommand("fabric:create -n"));
        waitForFabricCommands();
		//We are just want to use a feature repository that is not part of the distribution.
		System.out.println(executeCommand("fabric:profile-create --parents feature-camel test-profile"));
		System.out.println(executeCommand("fabric:version-create --parent 1.0 1.1"));
		System.out.println(executeCommand("fabric:profile-edit --repositories mvn:org.fusesource.examples.fabric-camel-dosgi/features/" + System.getProperty("fabric.version") + "/xml/features test-profile 1.1"));
		System.out.println(executeCommand("fabric:profile-edit --features fabric-dosgi test-profile 1.1"));
		//We remove all repositories from agent config but the maven central to rely on the fabric-maven-proxy.
	    //Also remove local repository
		System.out.println(executeCommand("profile-edit --pid io.fabric8.agent/org.ops4j.pax.url.mvn.repositories=http://repo1.maven.org/maven2@id=m2central default 1.1"));
		System.out.println(executeCommand("fabric:profile-edit --pid test-profile 1.1"));

        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            Set<ContainerProxy> containers = ContainerBuilder.create(fabricProxy).withName("cnt").withProfiles("test-profile").assertProvisioningResult().build();
            try {
                //We want to remove all repositories from fabric-agent.
                for (Container container : containers) {
                    System.out.println(executeCommand("fabric:container-upgrade 1.1 " + container.getId()));
                    System.out.flush();
                }
                Provision.provisioningSuccess(containers, PROVISION_TIMEOUT);
                System.out.println(executeCommand("fabric:container-list"));

                for (Container container : containers) {
                    System.out.println(executeCommand("fabric:container-connect -u admin -p admin " + container.getId() + " osgi:list"));
                    System.out.println(executeCommand("fabric:container-connect -u admin -p admin " + container.getId() + " config:proplist --pid org.ops4j.pax.url.mvn"));
                    System.out.flush();
                }
            } finally {
                ContainerBuilder.destroy(containers);
            }
		} finally {
            fabricProxy.close();
		}
	}

	@Configuration
	public Option[] config() {
		return new Option[]{
				new DefaultCompositeOption(fabricDistributionConfiguration()),
				editConfigurationFilePut("etc/system.properties", "fabric.version", MavenUtils.asInProject().getVersion(GROUP_ID, ARTIFACT_ID))
		};
	}
}
