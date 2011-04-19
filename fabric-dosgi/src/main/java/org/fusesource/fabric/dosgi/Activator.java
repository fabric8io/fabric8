/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.dosgi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.fusesource.fabric.dosgi.impl.Manager;
import org.linkedin.zookeeper.client.IZKClient;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator, ServiceTrackerCustomizer {

    private BundleContext context;
    private ServiceTracker tracker;
    private Map<IZKClient, Manager> managers = new ConcurrentHashMap<IZKClient, Manager>();

    public void start(BundleContext context) throws Exception {
        this.context = context;
        this.tracker = new ServiceTracker(this.context, IZKClient.class.getName(), this);
        this.tracker.open();
    }

    public void stop(BundleContext context) throws Exception {
        tracker.close();
        for (Manager manager : managers.values()) {
            manager.destroy();
        }
    }

    public Object addingService(ServiceReference reference) {
        IZKClient zooKeeper = (IZKClient) this.context.getService(reference);
        try {
            Manager manager = new Manager(this.context, zooKeeper);
            manager.init();
            this.managers.put(zooKeeper, manager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return zooKeeper;
    }

    public void modifiedService(ServiceReference reference, Object service) {
    }

    public void removedService(ServiceReference reference, Object service) {
        Manager manager = this.managers.remove(service);
        if (manager != null) {
            manager.destroy();
        }
        this.context.ungetService(reference);
    }

}
