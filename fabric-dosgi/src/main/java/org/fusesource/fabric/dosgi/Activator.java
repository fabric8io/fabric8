/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.dosgi;

import org.fusesource.fabric.dosgi.impl.Manager;
import org.linkedin.zookeeper.client.IZKClient;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class Activator {

    private BundleContext bundleContext;
    private Manager manager;
    private String uri;
    private ServiceReference reference;

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void destroy() {
        if (manager != null) {
            Manager mgr = manager;
            ServiceReference ref = reference;
            reference = null;
            manager = null;
            this.bundleContext.ungetService(ref);
            mgr.destroy();
        }
    }

    public void registerZooKeeper(ServiceReference ref) {
        try {
            destroy();
            reference = ref;
            manager = new Manager(this.bundleContext, (IZKClient) this.bundleContext.getService(reference), uri);
            manager.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unregisterZooKeeper(ServiceReference reference) {
        destroy();
    }

}
