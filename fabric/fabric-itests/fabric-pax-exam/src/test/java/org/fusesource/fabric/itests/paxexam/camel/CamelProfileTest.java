package org.fusesource.fabric.itests.paxexam.camel;


import org.fusesource.fabric.itests.paxexam.FabricFeaturesTest;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class CamelProfileTest extends FabricFeaturesTest {

    @After
    public void tearDown() throws InterruptedException {
        destroyChildContainer("camel1");
    }

    @Test
    public void testFeatures() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        createAndAssertChildContainer("camel1", "root", "default");
        addStagingRepoToDefaultProfile();
        assertProvisionedFeature("camel1", "camel-hazelcast", "camel", "camel-hazelcast");
    }

    @Configuration
    public Option[] config() {
        return fabricDistributionConfiguration();
    }
}
