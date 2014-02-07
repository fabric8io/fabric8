package io.fabric8.itests.basic;

import io.fabric8.api.Container;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.itests.paxexam.support.Provision;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

/**
 * A test for making sure that the container registration info such as jmx url and ssh url are updated, if new values
 * are assigned to them via the profile.
 */
@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ContainerRegistrationTest extends FabricTestSupport {

    @Test
    public void testContainerRegistration() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        waitForFabricCommands();
        System.err.println(executeCommand("fabric:profile-create --parents default child-profile"));
        Assert.assertTrue(Provision.profileAvailable("child-profile", "1.0", DEFAULT_TIMEOUT));
        Set<Container> containers = ContainerBuilder.create(1,1).withName("cnt").withProfiles("child-profile").assertProvisioningResult().build();
        try {
            Container child1 = containers.iterator().next();
            System.err.println(executeCommand("fabric:profile-edit --import-pid --pid org.apache.karaf.shell child-profile"));
            System.err.println(executeCommand("fabric:profile-edit --pid org.apache.karaf.shell/sshPort=8105 child-profile"));

            System.err.println(executeCommand("fabric:profile-edit --import-pid --pid org.apache.karaf.management child-profile"));
            System.err.println(executeCommand("fabric:profile-edit --pid org.apache.karaf.management/rmiServerPort=55555 child-profile"));
            System.err.println(executeCommand("fabric:profile-edit --pid org.apache.karaf.management/rmiRegistryPort=1100 child-profile"));
            System.err.println(executeCommand("fabric:profile-edit --pid org.apache.karaf.management/serviceUrl=service:jmx:rmi://localhost:55555/jndi/rmi://localhost:1099/karaf-"+child1.getId()+" child-profile"));

            Thread.sleep(DEFAULT_TIMEOUT);

            String sshUrl = child1.getSshUrl();
            String jmxUrl = child1.getJmxUrl();
            Assert.assertTrue(sshUrl.endsWith("8105"));
            Assert.assertTrue(jmxUrl.contains("55555"));
        } finally {
            ContainerBuilder.destroy(containers);
        }
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                //KarafDistributionOption.debugConfiguration("5005", true)
       };
    }
}