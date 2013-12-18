package io.fabric8.itests.basic;

import org.apache.curator.framework.CuratorFramework;
import io.fabric8.api.Container;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.itests.paxexam.support.Provision;
import io.fabric8.itests.paxexam.support.WaitForConfigurationChange;
import org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.exists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ExtendedUpgradeAndRollbackTest extends FabricTestSupport {

    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }

    /**
     * The purpose of this test is that everything works ok, even if the container is created after the version.
     * This is a test for the issue: http://fusesource.com/issues/browse/FABRIC-363
     */
    @Test
    public void testContainerAfterVersionUpgradeAndDowngrade() throws Exception {
        System.out.println(executeCommand("fabric:create -n"));
        waitForFabricCommands();
        System.out.println(executeCommand("fabric:version-create --parent 1.0 1.1"));
        Set<Container> containers = ContainerBuilder.create().withName("camel").withProfiles("feature-camel").assertProvisioningResult().build();
        //Make sure that the profile change has been applied before changing the version
        CountDownLatch latch = WaitForConfigurationChange.on(getFabricService());
        System.out.println(executeCommand("fabric:profile-edit --features camel-hazelcast feature-camel 1.1"));
        latch.await(5, TimeUnit.SECONDS);
        System.out.println(executeCommand("fabric:container-upgrade --all 1.1"));
        Provision.provisioningSuccess(containers, PROVISION_TIMEOUT);
        System.out.println(executeCommand("fabric:container-list"));

        for (Container container : containers) {
            assertEquals("Container should have version 1.1",   "1.1", container.getVersion().getId());
            String bundles = executeCommand("container-connect -u admin -p admin " + container.getId() + " osgi:list -s | grep camel-hazelcast");
            assertNotNull(bundles);
            System.out.println(bundles);
            assertFalse("Expected camel-hazelcast installed on container:"+container.getId()+".", bundles.isEmpty());
        }
        System.out.println(executeCommand("fabric:container-rollback --all 1.0"));
        Provision.provisioningSuccess(containers, PROVISION_TIMEOUT);
        System.out.println(executeCommand("fabric:container-list"));
        for (Container container : containers) {
            assertEquals("Container should have version 1.0",   "1.0", container.getVersion().getId());
            String bundles = executeCommand("container-connect -u admin -p admin " + container.getId() + " osgi:list -s | grep camel-hazelcast");
            assertNotNull(bundles);
            System.out.println(bundles);
            assertTrue("Expected no camel-hazelcast installed on container:"+container.getId()+".", bundles.isEmpty());
        }
    }


    /**
     * This is a test for http://fusesource.com/issues/browse/FABRIC-367.
     */
    @Test
    public void testContainerAfterVersionDowngrade() throws Exception {
        System.out.println(executeCommand("fabric:create -n"));
        waitForFabricCommands();
        System.out.println(executeCommand("fabric:version-create --parent 1.0 1.1"));
        System.out.println(executeCommand("fabric:container-upgrade --all 1.1"));
        Set<Container> containers = ContainerBuilder.create().withName("camel").withProfiles("feature-camel").assertProvisioningResult().build();

        System.out.println(executeCommand("fabric:container-rollback --all 1.0"));
        Provision.provisioningSuccess(containers, PROVISION_TIMEOUT);
        for (Container container : containers) {
            assertEquals("Container should have version 1.0",   "1.0", container.getVersion().getId());
            assertNotNull(exists(ServiceLocator.getOsgiService(CuratorFramework.class), "/fabric/configs/versions/1.0/containers/" + container.getId()));
        }
    }


    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration())
        };
    }
}
