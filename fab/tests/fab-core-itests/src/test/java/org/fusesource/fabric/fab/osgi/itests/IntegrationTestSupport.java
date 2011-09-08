/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi.itests;

import org.fusesource.fabric.fab.DependencyTree;
import org.fusesource.fabric.fab.osgi.FabURLHandler;
import org.fusesource.fabric.fab.osgi.internal.Configuration;
import org.fusesource.fabric.fab.osgi.internal.FabClassPathResolver;
import org.fusesource.fabric.fab.osgi.internal.FabConnection;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

import static org.junit.Assert.assertNotNull;

/**
 */
public class IntegrationTestSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(IntegrationTestSupport.class);
    public static final String JAVA_PROTOCOL_HANDLER_PKGS = "java.protocol.handler.pkgs";

    protected DependencyTree doTestFabricBundle(String artifactId) throws Exception {
        String groupId = "org.fusesource.fabric.fab.tests";
        return doTestFabricBundle(groupId, artifactId);
    }

    protected DependencyTree doTestFabricBundle(String groupId, String artifactId) throws Exception {
        installMavenUrlHandler();

        Configuration config = Configuration.newInstance();
        BundleContext bundleContext = new StubBundleContext();

        FabURLHandler handler = new FabURLHandler();
        handler.setBundleContext(bundleContext);
        FabConnection connection = handler.openConnection(new URL("fab:mvn:" + groupId + "/" + artifactId));
        FabClassPathResolver resolve = connection.resolve();
        DependencyTree rootTree = resolve.getRootTree();
        return rootTree;
    }

    public static void installMavenUrlHandler() {
        // lets add pax-maven-url...
        String separator = "|";
        String value = System.getProperty(JAVA_PROTOCOL_HANDLER_PKGS, "");
        String newPackage = "org.ops4j.pax.url" + separator + "org.fusesource.fabric.fab.osgi.url";
        if (value.length() > 0) {
            newPackage += separator;
        }
        value = newPackage + value;
        System.setProperty(JAVA_PROTOCOL_HANDLER_PKGS, value);

        LOG.info("System property " + JAVA_PROTOCOL_HANDLER_PKGS + " =  " + value);
    }

    protected void println(Object value, Throwable e) {
        println(value + ". " + e);
        e.printStackTrace();
    }

    protected void println(Object value) {
        System.out.println("======================== " + value);
    }

}
