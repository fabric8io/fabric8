package io.fabric8.itests.basic.esb;

import io.fabric8.api.Container;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
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
@Ignore("[FABRIC-669] Fix fabric basic EsbProfileTest")
public class EsbProfileTest extends FabricTestSupport {

    @After
    public void tearDown() throws InterruptedException {
       ContainerBuilder.destroy();;
    }

    @Test
    public void testLocalChildCreation() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        Set<Container> containers = ContainerBuilder.create().withName("esb").withProfiles("jboss-fuse-minimal").assertProvisioningResult().build();
    }

    @Configuration
    public Option[] config() {
        return fabricDistributionConfiguration();
    }
}
