/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.fab.osgi.url.internal;

import org.fusesource.fabric.fab.osgi.url.ServiceConstants;
import org.ops4j.pax.url.commons.handler.ConnectionFactory;
import org.ops4j.util.property.PropertyResolver;
import org.osgi.framework.BundleContext;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * {@link ConnectionFactory} for the "fab" protocol
 */
public class FabConnectionFactory implements ConnectionFactory<Configuration> {

    public URLConnection createConection(BundleContext bundleContext, URL url, Configuration config) throws MalformedURLException {
        String protocol = url.getProtocol();
        if (ServiceConstants.PROTOCOL_FAB.equals(protocol)) {
            return new FabConnection(url, config);
        }
        throw new MalformedURLException("Unsupported protocol: " + protocol);
    }

    public Configuration createConfiguration(PropertyResolver propertyResolver) {
        return new Configuration(propertyResolver);
    }
}
