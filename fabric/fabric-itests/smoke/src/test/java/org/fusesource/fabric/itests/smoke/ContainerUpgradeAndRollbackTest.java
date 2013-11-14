package org.fusesource.fabric.itests.smoke;

import org.apache.curator.framework.CuratorFramework;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.itests.paxexam.support.ContainerBuilder;
import org.fusesource.fabric.itests.paxexam.support.FabricTestSupport;
import org.fusesource.fabric.itests.paxexam.support.Provision;
import org.fusesource.fabric.itests.paxexam.support.WaitForConfigurationChange;
import org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator;
import org.junit.After;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.exists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ContainerUpgradeAndRollbackTest extends FabricTestSupport {

    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }

    /**
     * This tests the simple scenario of
     * 1. create a child container
     * 2. create a new version
     * 3. modify the profile of the new version
     * 4. upgrade all containers
     * 5. verify that child is provisioned according to the new version
     * 6. rollback containers.
     * 7. verify that the child is provisioned according to the old version.
     */
    @Test
    public void testContainerUpgradeAndRollback() throws Exception {
        System.out.println(executeCommand("fabric:create -n"));
        Set<Container> containers = ContainerBuilder.create().withName("camel").withProfiles("feature-camel").assertProvisioningResult().build();
        System.out.println(executeCommand("fabric:version-create --parent 1.0 1.1"));

        //Make sure that the profile change has been applied before changing the version
        CountDownLatch latch = WaitForConfigurationChange.on(getFabricService());
        System.out.println(executeCommand("fabric:profile-edit --features camel-hazelcast feature-camel 1.1"));
        latch.await(5, TimeUnit.SECONDS);

        System.out.println(executeCommand("fabric:profile-display --version 1.1 feature-camel"));
        System.out.println(executeCommand("fabric:container-upgrade --all 1.1"));
        Provision.provisioningSuccess(containers, PROVISION_TIMEOUT);
        System.out.println(executeCommand("fabric:container-list"));
        for (Container container : containers) {
            assertEquals("Container should have version 1.1", "1.1", container.getVersion().getId());
            String bundles = executeCommand("container-connect -u admin -p admin " + container.getId() + " osgi:list -s | grep camel-hazelcast");
            System.out.println(executeCommand("fabric:container-list"));
            assertNotNull(bundles);
            System.out.println(bundles);
            assertFalse("Expected camel-hazelcast installed.", bundles.isEmpty());
        }

        System.out.println(executeCommand("fabric:container-rollback --all 1.0"));
        Provision.provisioningSuccess(containers, PROVISION_TIMEOUT);
        System.out.println(executeCommand("fabric:container-list"));

        for (Container container : containers) {
            assertEquals("Container should have version 1.0",   "1.0", container.getVersion().getId());
            String bundles = executeCommand("container-connect -u admin -p admin " + container.getId() + " osgi:list -s | grep camel-hazelcast");
            assertNotNull(bundles);
            System.out.println(bundles);
            assertTrue("Expected no camel-hazelcast installed.", bundles.isEmpty());
        }
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
            System.out.println(executeCommand("fabric:container-list"));
            assertNotNull(bundles);
            System.out.println(bundles);
            assertFalse("Expected camel-hazelcast installed.", bundles.isEmpty());
        }
        System.out.println(executeCommand("fabric:container-rollback --all 1.0"));
        Provision.provisioningSuccess(containers, PROVISION_TIMEOUT);
        System.out.println(executeCommand("fabric:container-list"));
        for (Container container : containers) {
            assertEquals("Container should have version 1.0",   "1.0", container.getVersion().getId());
            String bundles = executeCommand("container-connect -u admin -p admin " + container.getId() + " osgi:list -s | grep camel-hazelcast");
            assertNotNull(bundles);
            System.out.println(bundles);
            assertTrue("Expected no camel-hazelcast installed.", bundles.isEmpty());
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
