/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi.itests;

import static org.apache.karaf.testing.Helper.felixProvisionalApis;
import static org.apache.karaf.testing.Helper.mavenBundle;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.scanFeatures;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.workingDirectory;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.testing.Helper;
import org.junit.Test;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public abstract class FabIntegrationTestSupport extends IntegrationTestSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabIntegrationTestSupport.class);

    protected CommandProcessor commandProcessor;
    protected CommandSession commandSession;

    @Test
    public void testInstallFabricBundles() {
        // Capture the exceptions so we can tune how the test reacts
        // to failures.  Start broad dependent and tune from there.
        try {
            LOG.info("Starting Fabric Bundles Test");
            
            // Configure container with custom settings for tuning output
            LOG.debug("Configure logging");
            configurePid("org.ops4j.pax.logging");

            // Configures the PAX Exam container PAX Maven URL
            // instance
            LOG.debug("Configure PAX Maven URL");
            configurePid("org.ops4j.pax.url.mvn");
            
            // Uncomment to tune how fab-osgi is configured
//            LOG.debug("Configure Fabric OSGi");
//            configurePid("org.fusesource.fabric.fab.osgi.url");

            Thread.sleep(15000);

            LOG.debug("Test to ensure the Blueprint OBR is available");
            // Make sure the command services are available
            assertNotNull(getOsgiService(BlueprintContainer.class, "osgi.blueprint.container.symbolicname=org.apache.karaf.shell.obr", 20000));
            
            LOG.debug("Test to ensure the Blueprint Wrapper is available");
            assertNotNull(getOsgiService(BlueprintContainer.class, "osgi.blueprint.container.symbolicname=org.apache.karaf.shell.wrapper", 20000));

            commandProcessor = getOsgiService(CommandProcessor.class, 20000);
            commandSession = commandProcessor.createSession(System.in, System.out, System.err);

            // Print the full list since the default start value is below 
            // the minimum value of 60 required by the list function
            commandSession.execute("osgi:list -t 0");
            assertStartBundle("org.apache.karaf.shell.log");

            assertStartBundle("org.fusesource.fabric.fab.fab-osgi");
            Thread.sleep(5000);

            doInstallFabricBundles();

            Thread.sleep(5000);

            // Print the full list since the default start value is below 
            // the minimum value of 60 required by the list function
            commandSession.execute("osgi:list -t 0");

            Thread.sleep(5000);

            stopBundles();

            commandSession.close();
        } catch (Exception e) {
            LOG.error("Error executing the test probe: " + e.getLocalizedMessage(), e);
        }
    }

    protected abstract void doInstallFabricBundles() throws Exception;


    @Configuration
    public static Option[] configuration() throws Exception {
        return configuration(false);
    }

    public static Option[] configuration(boolean useSpringDm) throws Exception {
        Option[] options = combine(
                // Default karaf environment
                Helper.getDefaultOptions(
                        // this is how you set the default log level when using pax logging (logProfile)
                        //Helper.setLogLevel("TRACE")
                        //Helper.setLogLevel("WARN")
                        Helper.setLogLevel("INFO")
                ),

                // Make sure that configadmin is provisioned early to allow us to tune the container
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin"),
                
                // add karaf features
                Helper.loadKarafStandardFeatures("obr", "wrapper"),

                // add fab features
                scanFeatures(
                        maven().groupId("org.fusesource.fabric").artifactId("fabric-distro").type("xml").classifier("features").versionAsInProject(),
                        "fabric-bundle"
                ),

                //useSpringDm ? profile("spring-dm") : null,
                useSpringDm ? scanFeatures(
                        maven().groupId("org.apache.karaf.assemblies.features").artifactId("standard").type("xml").classifier("features").versionAsInProject(),
                        "spring-dm"
                ) : null,

                // Disable management to avoid the JMX port issues
                mavenBundle("org.apache.karaf", "org.apache.karaf.management").noStart(),
                
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

    protected Bundle assertStartFab(String symbolicName) throws Exception {
        Bundle bundle = assertInstalledBundle(symbolicName);
        LOG.info("installed bundle: " + bundle + " is about to start");

        try {
            commandSession.execute("fab:start " + bundle.getBundleId());

            if (bundle.getState() != Bundle.ACTIVE) {
                bundle.start();
            }
            int state = bundle.getState();
            if (state != Bundle.ACTIVE) {
                fail("Bundle " + symbolicName + " did not start - its status is: " + state);
            }
            /*
            StartCommand start = new StartCommand();
            start.start(bundle);
            */

            startedBundles.addLast(bundle);
        } catch (BundleException e) {
            println("ERROR: " + e, e);
            throw e;
        }
        Thread.sleep(5000);
        return bundle;
    }



    protected void doInstallFabricBundle(String artifactId) throws Exception {
        commandSession.execute("osgi:install fab:mvn:org.fusesource.fabric.fab.tests/" + artifactId);

        Thread.sleep(1000);

        assertStartFab("org.fusesource.fabric.fab.tests." + artifactId);
    }

    protected void doInstallSpringDMFabricBundle(String artifactId) throws Exception {
        Thread.sleep(5000);

        assertStartBundle("org.springframework.osgi.extender");

        doInstallFabricBundle(artifactId);
    }

}
