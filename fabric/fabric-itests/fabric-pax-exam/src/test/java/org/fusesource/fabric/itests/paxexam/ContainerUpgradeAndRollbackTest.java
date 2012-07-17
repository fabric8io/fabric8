package org.fusesource.fabric.itests.paxexam;

import org.fusesource.fabric.api.FabricService;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ContainerUpgradeAndRollbackTest extends FabricTestSupport {

    @After
    public void tearDown() throws InterruptedException {
        destroyChildContainer("camel1");
    }

    @Test
    public void testContainerUpgrade() throws Exception {
        FabricService fabricService = getOsgiService(FabricService.class);

        System.out.println(executeCommand("fabric:create -n"));

        addStagingRepoToDefaultProfile();

        createAndAssertChildContainer("camel1", "root", "camel");
        System.out.println(executeCommand("fabric:version-create --parent 1.0 1.1"));
        System.out.println(executeCommand("fabric:profile-edit --features camel-hazelcast camel 1.1"));
        System.out.println(executeCommand("fabric:container-upgrade --all 1.1"));
        waitForProvisionSuccess(fabricService.getContainer("camel1"), PROVISION_TIMEOUT);
        String bundles = executeCommand("container-connect camel1 osgi:list -s | grep camel-hazelcast");
        System.out.println(executeCommand("fabric:container-list"));
        assertNotNull(bundles);
        System.out.println(bundles);
        assertFalse("Expected camel-hazelcast installed.", bundles.isEmpty());
        System.out.println(executeCommand("fabric:container-rollback --all 1.0"));
        waitForProvisionSuccess(fabricService.getContainer("camel1"), PROVISION_TIMEOUT);
        System.out.println(executeCommand("fabric:container-list"));
        bundles = executeCommand("container-connect camel1 osgi:list -s | grep camel-hazelcast");
        assertNotNull(bundles);
        System.out.println(bundles);
        assertTrue("Expected no camel-hazelcast installed.", bundles.isEmpty());
    }

    @Configuration
    public Option[] config() {
        return fabricDistributionConfiguration();
    }
}
