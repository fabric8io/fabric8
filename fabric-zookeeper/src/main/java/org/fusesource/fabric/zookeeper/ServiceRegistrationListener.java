/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.zookeeper;

import java.util.Properties;

import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class ServiceRegistrationListener implements LifecycleListener, ZooKeeperAware {

    private IZKClient zooKeeper;
    private String zooKeeperUrl;
    private BundleContext bundleContext;
    private ServiceRegistration registration;

    public IZKClient getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(IZKClient zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    public String getZooKeeperUrl() {
        return zooKeeperUrl;
    }

    public void setZooKeeperUrl(String zooKeeperUrl) {
        this.zooKeeperUrl = zooKeeperUrl;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void onConnected() {
        Properties props = new Properties();
        props.put("url", zooKeeperUrl);
        registration = bundleContext.registerService(IZKClient.class.getName(), zooKeeper, props);
    }

    public void onDisconnected() {
        if (registration != null) {
            registration.unregister();
        }
    }
}
