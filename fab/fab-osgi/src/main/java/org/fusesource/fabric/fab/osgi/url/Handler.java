/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.fab.osgi.url;

import org.fusesource.fabric.fab.osgi.url.internal.Configuration;
import org.fusesource.fabric.fab.osgi.url.internal.FabConnection;
import org.ops4j.util.property.PropertiesPropertyResolver;
import org.osgi.framework.BundleContext;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Implements the "fab:" protocol
 */
public class Handler extends URLStreamHandler {

    private BundleContext bundleContext;

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        PropertiesPropertyResolver resolver = new PropertiesPropertyResolver(System.getProperties());
        Configuration config = new Configuration(resolver);
        return new FabConnection(url, config, bundleContext);
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}