package org.fusesource.fabric.itests.basic.camel;


import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.itests.paxexam.support.FabricFeaturesTest;
import org.fusesource.fabric.itests.paxexam.support.ContainerBuilder;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import java.util.Set;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
@Ignore("[FABRIC-667] Fix fabric basic CamelProfileTest")
public class CamelProfileTest extends FabricFeaturesTest {

    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }

    @Test
    public void testFeatures() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        Set<Container> containers = ContainerBuilder.create().withName("feature-camel").withProfiles("feature-camel").assertProvisioningResult().build();
        assertProvisionedFeature(containers, "camel-hazelcast", "feature-camel", "camel-hazelcast");
    }

    @Configuration
    public Option[] config() {
        return fabricDistributionConfiguration();
    }
}
