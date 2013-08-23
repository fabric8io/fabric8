package org.fusesource.fabric.itests.paxexam;

import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.itests.paxexam.support.ContainerBuilder;
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

import javax.inject.Inject;
import java.util.Set;

/**
 * A test for making sure that the container registration info such as jmx url and ssh url are updated, if new values
 * are assigned to them via the profile.
 */
@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
@Ignore("[FABRIC-521] Fix fabric-pax-exam tests")
public class ContainerRegistrationTest extends FabricTestSupport {

    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }

    @Test
    public void testSshPortRegistration() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        System.err.println(executeCommand("fabric:profile-create --parents default child-profile"));
        Set<Container> containers = ContainerBuilder.create(1,1).withName("cnt").withProfiles("child-profile").assertProvisioningResult().build();

        Container child1 = containers.iterator().next();
        System.err.println(executeCommand("fabric:profile-edit --i org.apache.karaf.shell child-profile"));
        System.err.println(executeCommand("fabric:profile-edit --pid org.apache.karaf.shell/sshPort=8105 child-profile"));
        Thread.sleep(DEFAULT_TIMEOUT);
        System.err.println(executeCommand("fabric:container-connect "+child1.getId()+" config:proplist --pid org.apache.karaf.shell"));
        String sshUrl = child1.getSshUrl();
        Assert.assertTrue(sshUrl.endsWith("8105"));
    }


    @Test
    public void testJmxPortRegistration() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
		System.err.println(executeCommand("fabric:profile-create --parents default child-profile"));
        Set<Container> containers = ContainerBuilder.create(1,1).withName("cnt").withProfiles("child-profile").assertProvisioningResult().build();

        Container child1 = containers.iterator().next();
        System.err.println(executeCommand("fabric:profile-edit --i org.apache.karaf.management child-profile"));
        System.err.println(executeCommand("fabric:profile-edit --pid org.apache.karaf.management/serviceUrl=service:jmx:rmi://localhost:55555/jndi/rmi://localhost:1099/karaf-"+child1.getId()+" child-profile"));
        System.err.println(executeCommand("fabric:profile-display child-profile"));
        Thread.sleep(DEFAULT_TIMEOUT);
        System.err.println(executeCommand("fabric:container-connect "+child1.getId()+" config:proplist --pid org.apache.karaf.management"));
        String jmxUrl = child1.getJmxUrl();
        Assert.assertTrue(jmxUrl.contains("55555"));
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
       };
    }
}