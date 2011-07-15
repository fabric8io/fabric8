package org.fusesource.fabric.fab.osgi.itests;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.testing.Helper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.service.blueprint.container.BlueprintContainer;

import static org.apache.karaf.testing.Helper.felixProvisionalApis;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.scanFeatures;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.workingDirectory;

@RunWith(JUnit4TestRunner.class)
public class FabIntegrationTest extends FabIntegrationTestSupport {

    @Override
    protected void doInstallFabricBundles() throws Exception {
        commandSession.execute("osgi:install fab:mvn:org.fusesource.fabric.fab.tests/fab-sample-camel-noshare");

        Thread.sleep(1000);

        assertStartBundle("org.fusesource.fabric.fab.tests.fab-sample-camel-noshare");
    }




}
