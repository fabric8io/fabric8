/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.fab.osgi.itests;

import org.apache.karaf.testing.Helper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.scanFeatures;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.workingDirectory;

@RunWith(JUnit4TestRunner.class)
public class AgentIntegrationTest extends IntegrationTestSupport {

    @Test
    public void testRun() throws Exception {
        Thread.sleep(10000);

        assertStartBundle("org.fusesource.fabric.fabric-agent");
        Thread.sleep(10000);
        stopBundles();
    }

    @Configuration
    public static Option[] configuration() throws Exception {
        Option[] options = combine(
                // Default karaf environment
                Helper.getDefaultOptions(
                        // this is how you set the default log level when using pax logging (logProfile)
                        //Helper.setLogLevel("TRACE")
                        Helper.setLogLevel("INFO")
                ),

                // add fab features
                scanFeatures(
                        maven().groupId("org.fusesource.fabric").artifactId("fabric-distro").type("xml").classifier("features").versionAsInProject(),
                        "fabric-agent"
                ),

                workingDirectory("target/paxrunner/core/"),

                waitForFrameworkStartup(),

                // TODO Test on both equinox and felix
                // TODO: pax-exam does not support the latest felix version :-(
                // TODO: so we use the higher supported which should be the same
                // TODO: as the one specified in itests/dependencies/pom.xml
                //equinox(), felix().version("3.0.2")
                felix().version("3.0.2")
        );
        // Stop the shell log bundle
        //Helper.findMaven(options, "org.apache.karaf.shell", "org.apache.karaf.shell.log").noStart();
        return options;
    }


}
