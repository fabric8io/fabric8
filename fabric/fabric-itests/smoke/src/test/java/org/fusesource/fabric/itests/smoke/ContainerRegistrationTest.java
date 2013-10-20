package org.fusesource.fabric.itests.smoke;

import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.itests.paxexam.support.ContainerBuilder;
import org.fusesource.fabric.itests.paxexam.support.Provision;
import org.fusesource.fabric.itests.paxexam.support.FabricTestSupport;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import java.util.Set;

/**
 * A test for making sure that the container registration info such as jmx url and ssh url are updated, if new values
 * are assigned to them via the profile.
 */
@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
@Ignore("[FABRIC-521] Fix fabric/fabric-itests/fabric-itests-smoke")
public class ContainerRegistrationTest extends FabricTestSupport {

    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }

    @Test
    public void testSshPortRegistration() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        Thread.sleep(3000);
        System.err.println(executeCommand("fabric:profile-create --parents default child-profile"));
        Assert.assertTrue(Provision.profileAvailable("child-profile", "1.0", DEFAULT_TIMEOUT));
        Set<Container> containers = ContainerBuilder.create(1,1).withName("cnt").withProfiles("child-profile").assertProvisioningResult().build();

        Container child1 = containers.iterator().next();
        System.err.println(executeCommand("fabric:profile-edit --import-pid --pid org.apache.karaf.shell child-profile"));
        System.err.println(executeCommand("fabric:profile-edit --pid org.apache.karaf.shell/sshPort=8105 child-profile"));
        Thread.sleep(DEFAULT_TIMEOUT);
        System.err.println(executeCommand("fabric:container-connect -u admin -p admin "+child1.getId()+" config:proplist --pid org.apache.karaf.shell | grep sshPort"));
        String sshUrl = child1.getSshUrl();
        Assert.assertTrue(sshUrl.endsWith("8105"));
    }


    @Test
    public void testJmxPortRegistration() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        Thread.sleep(3000);
		System.err.println(executeCommand("fabric:profile-create --parents default child-profile"));
        Set<Container> containers = ContainerBuilder.create(1,1).withName("cnt").withProfiles("child-profile").assertProvisioningResult().build();

        Container child1 = containers.iterator().next();
        System.err.println(executeCommand("fabric:profile-edit --import-pid --pid org.apache.karaf.management child-profile"));
        System.err.println(executeCommand("fabric:profile-edit --pid org.apache.karaf.management/rmiServerPort=55555 child-profile"));
        System.err.println(executeCommand("fabric:profile-edit --pid org.apache.karaf.management/rmiRegistryPort=1100 child-profile"));
        System.err.println(executeCommand("fabric:profile-edit --pid org.apache.karaf.management/serviceUrl=service:jmx:rmi://localhost:55555/jndi/rmi://localhost:1099/karaf-"+child1.getId()+" child-profile"));
        System.err.println(executeCommand("fabric:profile-display child-profile"));
        Thread.sleep(DEFAULT_TIMEOUT);
        System.err.println(executeCommand("fabric:container-connect -u admin -p admin "+child1.getId()+" config:proplist --pid org.apache.karaf.management | grep rmiServerPort"));
        String jmxUrl = child1.getJmxUrl();
        System.out.println(jmxUrl);
        Assert.assertTrue(jmxUrl.contains("55555"));
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
       };
    }
}