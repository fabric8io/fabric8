package io.fabric8.itests.basic;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceLocator;
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.itests.paxexam.support.Provision;
import io.fabric8.itests.paxexam.support.WaitForConfigurationChange;
import io.fabric8.zookeeper.utils.ZooKeeperUtils;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ExtendedUpgradeAndRollbackTest extends FabricTestSupport {

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
        try {
            //Make sure that the profile change has been applied before changing the version
            ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
            try {
                FabricService fabricService = fabricProxy.getService();
                CountDownLatch latch = WaitForConfigurationChange.on(fabricService);
                System.out.println(executeCommand("fabric:profile-edit --features camel-hazelcast feature-camel 1.1"));
                Assert.assertTrue(latch.await(5, TimeUnit.SECONDS));
            } finally {
                fabricProxy.close();
            }

            System.out.println(executeCommand("fabric:container-upgrade --all 1.1"));
            Provision.provisioningSuccess(containers, PROVISION_TIMEOUT);
            System.out.println(executeCommand("fabric:container-list"));

            for (Container container : containers) {
                Assert.assertEquals("Container should have version 1.1",   "1.1", container.getVersion().getId());
                String bundles = executeCommand("container-connect -u admin -p admin " + container.getId() + " osgi:list -s | grep camel-hazelcast");
                Assert.assertNotNull(bundles);
                System.out.println(bundles);
                Assert.assertFalse("Expected camel-hazelcast installed on container:"+container.getId()+".", bundles.isEmpty());
            }
            System.out.println(executeCommand("fabric:container-rollback --all 1.0"));
            Provision.provisioningSuccess(containers, PROVISION_TIMEOUT);
            System.out.println(executeCommand("fabric:container-list"));
            for (Container container : containers) {
                Assert.assertEquals("Container should have version 1.0",   "1.0", container.getVersion().getId());
                String bundles = executeCommand("container-connect -u admin -p admin " + container.getId() + " osgi:list -s | grep camel-hazelcast");
                Assert.assertNotNull(bundles);
                System.out.println(bundles);
                Assert.assertTrue("Expected no camel-hazelcast installed on container:"+container.getId()+".", bundles.isEmpty());
            }
        } finally {
            ContainerBuilder.destroy(containers);
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
        try {
            System.out.println(executeCommand("fabric:container-rollback --all 1.0"));
            Provision.provisioningSuccess(containers, PROVISION_TIMEOUT);
            for (Container container : containers) {
                Assert.assertEquals("Container should have version 1.0", "1.0", container.getVersion().getId());
                Assert.assertNotNull(ZooKeeperUtils.exists(ServiceLocator.awaitService(CuratorFramework.class), "/fabric/configs/versions/1.0/containers/" + container.getId()));
            }
        } finally {
            ContainerBuilder.destroy(containers);
        }
    }


    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration())
        };
    }
}
