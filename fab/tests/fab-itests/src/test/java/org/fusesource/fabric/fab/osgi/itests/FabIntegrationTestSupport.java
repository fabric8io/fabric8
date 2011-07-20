/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi.itests;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.testing.Helper;
import org.junit.Test;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.osgi.service.blueprint.container.BlueprintContainer;

import static org.apache.karaf.testing.Helper.felixProvisionalApis;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.*;

/**
 */
public abstract class FabIntegrationTestSupport extends IntegrationTestSupport {
    protected CommandProcessor commandProcessor;
    protected CommandSession commandSession;

    @Test
    public void testInstallFabricBundles() throws Exception {
        Thread.sleep(10000);

        // Make sure the command services are available
        assertNotNull(getOsgiService(BlueprintContainer.class, "osgi.blueprint.container.symbolicname=org.apache.karaf.shell.obr", 20000));
        assertNotNull(getOsgiService(BlueprintContainer.class, "osgi.blueprint.container.symbolicname=org.apache.karaf.shell.wrapper", 20000));

        commandProcessor = getOsgiService(CommandProcessor.class);
        commandSession = commandProcessor.createSession(System.in, System.out, System.err);

        commandSession.execute("osgi:list");
        assertStartBundle("org.apache.karaf.shell.log");

        assertStartBundle("org.fusesource.fabric.fab.fabric-fab-osgi");
        Thread.sleep(1000);

        doInstallFabricBundles();

        Thread.sleep(1000);

        commandSession.execute("osgi:list");

        Thread.sleep(2000);

        stopBundles();

        commandSession.close();
    }

    protected abstract void doInstallFabricBundles() throws Exception;


    @Configuration
    public static Option[] configuration() throws Exception {
        Option[] options = combine(
                // Default karaf environment
                Helper.getDefaultOptions(
                        // this is how you set the default log level when using pax logging (logProfile)
                        //Helper.setLogLevel("TRACE")
                        Helper.setLogLevel("INFO")
                ),

                // add karaf features
                Helper.loadKarafStandardFeatures("obr", "wrapper"),

                // add fab features
                scanFeatures(
                        maven().groupId("org.fusesource.fabric").artifactId("fabric-distro").type("xml").classifier("features").versionAsInProject(),
                        "fabric-bundle"
                ),

                mavenBundle("org.apache.felix", "org.apache.felix.configadmin"),
                workingDirectory("target/paxrunner/core/"),

                waitForFrameworkStartup(),

                // TODO Test on both equinox and felix
                // TODO: pax-exam does not support the latest felix version :-(
                // TODO: so we use the higher supported which should be the same
                // TODO: as the one specified in itests/dependencies/pom.xml
                //equinox(), felix().version("3.0.2")
                felix().version("3.0.2"),

                // If you wnat to debug the OSGi modules add the following system property when your run the test
                // -Dpax-runner-vm-options="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"
                System.getProperty("pax-runner-vm-options")!=null ? vmOption(System.getProperty("pax-runner-vm-options")) : null,
                felixProvisionalApis()

        );

        // Stop the shell log bundle
        //Helper.findMaven(options, "org.apache.karaf.shell", "org.apache.karaf.shell.log").noStart();
        return options;
    }

    protected void doInstallFabricBundle(String artifactId) throws Exception {
        commandSession.execute("osgi:install fab:mvn:org.fusesource.fabric.fab.tests/" + artifactId);

        Thread.sleep(1000);

        assertStartBundle("org.fusesource.fabric.fab.tests." + artifactId);
    }
}
