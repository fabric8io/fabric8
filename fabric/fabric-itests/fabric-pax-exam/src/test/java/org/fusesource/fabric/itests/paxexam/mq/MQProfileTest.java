package org.fusesource.fabric.itests.paxexam.mq;

import org.fusesource.fabric.itests.paxexam.FabricCommandsTestSupport;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openengsb.labs.paxexam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;


import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.logLevel;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class MQProfileTest extends FabricCommandsTestSupport {

    @After
    public void tearDown() throws InterruptedException {
       destroyChildContainer("mq1");
    }

    @Test
    public void testLocalChildCreation() throws Exception {
         System.err.println(executeCommand("fabric:create"));
         createAndAssetChildContainer("camel1","root", "mq");
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                fabricDistributionConfiguration(), keepRuntimeFolder(),
                logLevel(LogLevelOption.LogLevel.ERROR)};
    }
}
