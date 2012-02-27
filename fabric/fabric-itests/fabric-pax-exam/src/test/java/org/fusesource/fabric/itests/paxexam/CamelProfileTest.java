package org.fusesource.fabric.itests.paxexam;


import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openengsb.labs.paxexam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import static org.junit.Assert.*;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.logLevel;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class CamelProfileTest extends FabricCommandsTestSupport {

    @After
    public void tearDown() throws InterruptedException {
       destroyChildContainer("camel1");
    }

    @Test
    public void testLocalChildCreation() throws Exception {
         System.err.println(executeCommand("fabric:create"));
         createAndAssetChildContainer("camel1","root", "camel");
         String bundles = executeCommand("container-connect camel1 osgi:list -s | grep org.apache.camel.camel-core");
         assertNotNull(bundles);
         System.out.println(bundles);
         assertFalse("Expected camel core installed.", bundles.isEmpty());
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                fabricDistributionConfiguration(), keepRuntimeFolder(),
                logLevel(LogLevelOption.LogLevel.ERROR)};
    }
}
