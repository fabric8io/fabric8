/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.fab.osgi.url.fab;

import org.fusesource.fabric.fab.osgi.internal.Configuration;
import org.fusesource.fabric.fab.osgi.internal.FabConnection;
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
    protected Object clone() throws CloneNotSupportedException {
        // TODO
        return super.clone();

    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        Configuration config = Configuration.newInstance();
        return new FabConnection(url, config, bundleContext);
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}