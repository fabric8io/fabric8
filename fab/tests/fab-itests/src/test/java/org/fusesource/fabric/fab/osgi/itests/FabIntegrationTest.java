/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi.itests;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

@RunWith(JUnit4TestRunner.class)
public class FabIntegrationTest extends FabIntegrationTestSupport {

    @Override
    protected void doInstallFabricBundles() throws Exception {
        commandSession.execute("osgi:install fab:mvn:org.fusesource.fabric.fab.tests/fab-sample-camel-noshare");

        Thread.sleep(1000);

        assertStartBundle("org.fusesource.fabric.fab.tests.fab-sample-camel-noshare");
    }

}
