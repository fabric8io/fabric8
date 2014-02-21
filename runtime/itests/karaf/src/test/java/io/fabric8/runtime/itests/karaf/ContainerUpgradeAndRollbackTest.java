package io.fabric8.runtime.itests.karaf;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.runtime.itests.support.CommandSupport;
import io.fabric8.runtime.itests.support.ContainerBuilder;
import io.fabric8.runtime.itests.support.FabricTestSupport;
import io.fabric8.runtime.itests.support.Provision;
import io.fabric8.runtime.itests.support.ServiceProxy;
import io.fabric8.runtime.itests.support.WaitForConfigurationChange;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("[FABRIC-819] Provide initial set of portable fabric smoke tests")
public class ContainerUpgradeAndRollbackTest {

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
        System.out.println(CommandSupport.executeCommand("fabric:create -n"));
        Set<Container> containers = ContainerBuilder.create().withName("camel").withProfiles("feature-camel").assertProvisioningResult().build();
        try {
            System.out.println(CommandSupport.executeCommand("fabric:version-create --parent 1.0 1.1"));

            ModuleContext moduleContext = RuntimeLocator.getRequiredRuntime().getModuleContext();
            ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
            try {
                //Make sure that the profile change has been applied before changing the version
                CountDownLatch latch = WaitForConfigurationChange.on(fabricProxy.getService());
                System.out.println(CommandSupport.executeCommand("fabric:profile-edit --features camel-hazelcast feature-camel 1.1"));
                Assert.assertTrue(latch.await(5, TimeUnit.SECONDS));
            } finally {
                fabricProxy.close();
            }

            System.out.println(CommandSupport.executeCommand("fabric:profile-display --version 1.1 feature-camel"));
            System.out.println(CommandSupport.executeCommand("fabric:container-upgrade --all 1.1"));
            Provision.provisioningSuccess(containers, FabricTestSupport.PROVISION_TIMEOUT);
            System.out.println(CommandSupport.executeCommand("fabric:container-list"));
            for (Container container : containers) {
                Assert.assertEquals("Container should have version 1.1", "1.1", container.getVersion().getId());
                String bundles = CommandSupport.executeCommand("container-connect -u admin -p admin " + container.getId() + " osgi:list -s | grep camel-hazelcast");
                Assert.assertNotNull(bundles);
                System.out.println(bundles);
                Assert.assertFalse("Expected camel-hazelcast installed on container:"+container.getId()+".", bundles.isEmpty());
            }

            System.out.println(CommandSupport.executeCommand("fabric:container-rollback --all 1.0"));
            Provision.provisioningSuccess(containers, FabricTestSupport.PROVISION_TIMEOUT);
            System.out.println(CommandSupport.executeCommand("fabric:container-list"));

            for (Container container : containers) {
                Assert.assertEquals("Container should have version 1.0",   "1.0", container.getVersion().getId());
                String bundles = CommandSupport.executeCommand("container-connect -u admin -p admin " + container.getId() + " osgi:list -s | grep camel-hazelcast");
                Assert.assertNotNull(bundles);
                System.out.println(bundles);
                Assert.assertTrue("Expected no camel-hazelcast installed on container:"+container.getId()+".", bundles.isEmpty());
            }
        } finally {
            ContainerBuilder.destroy(containers);
        }
    }
}
