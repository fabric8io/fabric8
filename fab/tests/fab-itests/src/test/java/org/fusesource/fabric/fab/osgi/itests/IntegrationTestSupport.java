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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

import static org.junit.Assert.assertNotNull;

/**
 */
public class IntegrationTestSupport extends AbstractIntegrationTest {
    private static final transient Logger LOG = LoggerFactory.getLogger(IntegrationTestSupport.class);

    protected LinkedList<Bundle> startedBundles = new LinkedList<Bundle>();

    protected Bundle assertStartBundle(String symbolicName) throws Exception {
        Bundle bundle = assertInstalledBundle(symbolicName);
        LOG.info("installed bundle: " + bundle + " is about to start");
        try {
            bundle.start();
            startedBundles.addLast(bundle);
        } catch (BundleException e) {
            println("ERROR: " + e, e);
            throw e;
        }
        Thread.sleep(1000);
        return bundle;
    }

    protected Bundle assertInstalledBundle(String symbolicName) {
        Bundle bundle = getInstalledBundle(symbolicName);
        assertNotNull("Should have a bundle for: " + symbolicName, bundle);
        return bundle;
    }

    protected void stopBundles() throws Exception {
        while (!startedBundles.isEmpty()) {
            Bundle bundle = startedBundles.removeFirst();

            assertNotNull("Should have a bundle", bundle);
            LOG.info("stopping bundle: " + bundle + " is about to start");
            bundle.stop();
        }
    }

    protected void println(Object value, BundleException e) {
        println(value);
        e.printStackTrace();
    }

    protected void println(Object value) {
        System.out.println("======================== " + value);
    }
}
