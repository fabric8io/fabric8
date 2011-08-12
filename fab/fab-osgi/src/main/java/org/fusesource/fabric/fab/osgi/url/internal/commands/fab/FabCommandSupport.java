/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi.url.internal.commands.fab;

import org.apache.karaf.shell.dev.AbstractBundleCommand;
import org.fusesource.fabric.fab.osgi.url.internal.BundleFabFacade;
import org.fusesource.fabric.fab.osgi.url.internal.FabClassPathResolver;
import org.fusesource.fabric.fab.osgi.url.internal.FabFacade;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 */
public abstract class FabCommandSupport extends AbstractBundleCommand {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabCommandSupport.class);

    protected void doExecute(Bundle bundle) throws Exception {
        Properties instructions = new Properties();
        Dictionary headers = bundle.getHeaders();
        Enumeration e = headers.keys();
        while (e.hasMoreElements()) {
            Object key = e.nextElement();
            Object value = headers.get(key);
            if (key instanceof String && value instanceof String) {
                instructions.setProperty((String) key, (String) value);
            }
        }

        FabFacade facade = new BundleFabFacade(bundle);
        Map<String, Object> embeddedResources = new HashMap<String, Object>();
        FabClassPathResolver resolver = new FabClassPathResolver(facade, instructions, embeddedResources);
        resolver.resolve();

        doExecute(bundle, resolver);
    }

    protected abstract void doExecute(Bundle bundle, FabClassPathResolver resolver) throws Exception;

    protected void stopBundle(Bundle bundle) {
        if (bundle.getState() == Bundle.ACTIVE) {
            LOG.debug("Stopping bundle %s version %s", bundle.getSymbolicName(), bundle.getVersion());
            try {
                bundle.stop();
            } catch (BundleException e) {
                System.out.println("Failed to start " + bundle.getSymbolicName() + " " + bundle.getVersion() + ". " + e);
                e.printStackTrace();
            }
        }
    }
}
