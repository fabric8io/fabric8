/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi.itests;

import org.apache.karaf.testing.AbstractIntegrationTest;
import org.apache.karaf.testing.Helper;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.scanFeatures;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.workingDirectory;

/**
 */
public class IntegrationTestSupport extends AbstractIntegrationTest {

    /*
    @Test
    public void testInstallCommand() throws Exception {
        Thread.sleep(12000);

        CommandProcessor cp = getOsgiService(CommandProcessor.class);
        CommandSession cs = cp.createSession(System.in, System.out, System.err);

        try {
            cs.execute("log:display");
            fail("command should not exist");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().indexOf("Command not found") >= 0);
        }

        Bundle b = getInstalledBundle("org.apache.karaf.shell.log");
        b.start();

        Thread.sleep(1000);

        cs.execute("log:display");

        b.stop();

        Thread.sleep(1000);

        try {
            cs.execute("log:display");
            fail("command should not exist");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().indexOf("Command not found") >= 0);
        }

        cs.close();
    }
    */

    protected Bundle assertStartBundle(String symbolicName) throws Exception {
        Bundle b = getInstalledBundle(symbolicName);

        println("got bundle: " + b);
        try {
            b.start();
        } catch (BundleException e) {
            println("ERROR: " + e, e);
            throw e;
        }
        Thread.sleep(1000);
        return b;
    }


    protected void println(Object value, BundleException e) {
        println(value);
        e.printStackTrace();
    }

    protected void println(Object value) {
        System.out.println("======================== " + value);
    }
}
