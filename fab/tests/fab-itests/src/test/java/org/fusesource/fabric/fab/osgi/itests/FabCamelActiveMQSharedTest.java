package org.fusesource.fabric.fab.osgi.itests;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

@RunWith(JUnit4TestRunner.class)
public class FabCamelActiveMQSharedTest extends SpringDmTestSupport {

    @Override
    protected void doInstallFabricBundles() throws Exception {
        doInstallSpringDMFabricBundle("fab-sample-camel-activemq-share");
    }
}
