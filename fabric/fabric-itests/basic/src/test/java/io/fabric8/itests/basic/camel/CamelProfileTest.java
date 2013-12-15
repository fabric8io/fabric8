package io.fabric8.itests.basic.camel;


import io.fabric8.api.Container;
import io.fabric8.itests.paxexam.support.FabricFeaturesTest;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import org.junit.After;
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
public class CamelProfileTest extends FabricFeaturesTest {

    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }

    @Test
    public void testFeatures() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        Set<Container> containers = ContainerBuilder.create().withName("feature-camel").withProfiles("feature-camel").assertProvisioningResult().build();
        assertProvisionedFeature(containers, "camel-http", "feature-camel", "camel-http");
        //assertProvisionedFeature(containers, "camel-jetty", "feature-camel", "camel-jetty");
        assertProvisionedFeature(containers, "camel-jms", "feature-camel", "camel-jms");
        assertProvisionedFeature(containers, "camel-ftp", "feature-camel", "camel-ftp");
        assertProvisionedFeature(containers, "camel-quartz", "feature-camel", "camel-quartz");
    }

    @Configuration
    public Option[] config() {
        return fabricDistributionConfiguration();
    }
}
