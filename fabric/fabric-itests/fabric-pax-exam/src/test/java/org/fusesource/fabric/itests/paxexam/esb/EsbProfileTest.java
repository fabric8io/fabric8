package org.fusesource.fabric.itests.paxexam.esb;

import org.fusesource.fabric.itests.paxexam.FabricTestSupport;
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
public class EsbProfileTest extends FabricTestSupport {

    @After
    public void tearDown() throws InterruptedException {
       destroyChildContainer("esb1");
    }

    @Test
    public void testLocalChildCreation() throws Exception {
         System.err.println(executeCommand("fabric:create -n"));
         addStagingRepoToDefaultProfile();
         createAndAssertChildContainer("esb1", "root", "jboss-fuse-medium");
    }

    @Configuration
    public Option[] config() {
        return fabricDistributionConfiguration();
    }
}
