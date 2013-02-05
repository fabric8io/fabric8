package org.fusesource.fabric.itests.paxexam;

import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.zookeeper.IZKClient;
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

    /**
     * This tests the simple scenario of
     * 1. create a child container
     * 2. create a new version
     * 3. modify the profile of the new version
     * 4. upgrade all contaienrs
     * 5. verify that child is provisioned according to the new version
     * 6. rollback containers.
     * 7. verify that the child is provisioned according to the old version.
     * @throws Exception
     */
    @Test
    public void testContainerUpgradeAndRollback() throws Exception {
        FabricService fabricService = getOsgiService(FabricService.class);
        System.out.println(executeCommand("fabric:create -n"));
        createAndAssertChildContainer("camel1", "root", "camel");
        System.out.println(executeCommand("fabric:version-create --parent 1.0 1.1"));
        System.out.println(executeCommand("fabric:profile-edit --features camel-hazelcast camel 1.1"));
        System.out.println(executeCommand("fabric:container-upgrade --all 1.1"));
        waitForProvisionSuccess(fabricService.getContainer("camel1"), PROVISION_TIMEOUT);
        String bundles = executeCommand("container-connect -u admin -p admin camel1 osgi:list -s | grep camel-hazelcast");
        System.out.println(executeCommand("fabric:container-list"));
        assertNotNull(bundles);
        System.out.println(bundles);
        assertFalse("Expected camel-hazelcast installed.", bundles.isEmpty());
        System.out.println(executeCommand("fabric:container-rollback --all 1.0"));
        waitForProvisionSuccess(fabricService.getContainer("camel1"), PROVISION_TIMEOUT);
        System.out.println(executeCommand("fabric:container-list"));
        bundles = executeCommand("container-connect -u admin -p admin camel1 osgi:list -s | grep camel-hazelcast");
        assertNotNull(bundles);
        System.out.println(bundles);
        assertTrue("Expected no camel-hazelcast installed.", bundles.isEmpty());
    }

    /**
     * The purpose of this test is that everything works ok, even if the container is created after the version.
     * This is a test for the issue: http://fusesource.com/issues/browse/FABRIC-363
     * @throws Exception
     */
    @Test
    public void testContainerAfterVersionUpgradeAndDowngrade() throws Exception {
        FabricService fabricService = getOsgiService(FabricService.class);
        System.out.println(executeCommand("fabric:create -n"));
        System.out.println(executeCommand("fabric:version-create --parent 1.0 1.1"));
        createAndAssertChildContainer("camel1", "root", "camel");
        System.out.println(executeCommand("fabric:profile-edit --features camel-hazelcast camel 1.1"));
        System.out.println(executeCommand("fabric:container-upgrade --all 1.1"));
        waitForProvisionSuccess(fabricService.getContainer("camel1"), PROVISION_TIMEOUT);
        String bundles = executeCommand("container-connect -u admin -p admin camel1 osgi:list -s | grep camel-hazelcast");
        System.out.println(executeCommand("fabric:container-list"));
        assertNotNull(bundles);
        System.out.println(bundles);
        assertFalse("Expected camel-hazelcast installed.", bundles.isEmpty());
        System.out.println(executeCommand("fabric:container-rollback --all 1.0"));
        waitForProvisionSuccess(fabricService.getContainer("camel1"), PROVISION_TIMEOUT);
        System.out.println(executeCommand("fabric:container-list"));
        bundles = executeCommand("container-connect -u admin -p admin camel1 osgi:list -s | grep camel-hazelcast");
        assertNotNull(bundles);
        System.out.println(bundles);
        assertTrue("Expected no camel-hazelcast installed.", bundles.isEmpty());
    }


    /**
     * This is a test for http://fusesource.com/issues/browse/FABRIC-367.
     * @throws Exception
     */
    @Test
    public void testContainerAfterVersionDowngrade() throws Exception {
        FabricService fabricService = getOsgiService(FabricService.class);
        System.out.println(executeCommand("fabric:create -n"));
        System.out.println(executeCommand("fabric:version-create --parent 1.0 1.1"));
        System.out.println(executeCommand("fabric:container-upgrade --all 1.1"));
        createAndAssertChildContainer("camel1", "root", "camel");
        System.out.println(executeCommand("fabric:container-rollback --all 1.0"));
        waitForProvisionSuccess(fabricService.getContainer("camel1"), PROVISION_TIMEOUT);
        IZKClient zookeeper = getOsgiService(IZKClient.class);
        assertNotNull(zookeeper.exists("/fabric/configs/versions/1.0/containers/camel1"));
    }


    @Configuration
    public Option[] config() {
        return fabricDistributionConfiguration();
    }
}
