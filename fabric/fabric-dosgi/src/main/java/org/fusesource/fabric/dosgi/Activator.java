/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.dosgi;

import org.fusesource.fabric.dosgi.impl.Manager;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class Activator implements LifecycleListener {

    private BundleContext bundleContext;
    private Manager manager;
    private String uri;
    private String exportedAddress;
    private ServiceReference reference;
    private IZKClient zookeeper;

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setExportedAddress(String exportedAddress) {
        this.exportedAddress = exportedAddress;
    }

    public void destroy() {
        destroyManager();
        if (reference != null) {
            ServiceReference ref = reference;
            reference = null;
            zookeeper = null;
            this.bundleContext.ungetService(ref);
        }
    }

    protected void destroyManager() {
        if (manager != null) {
            Manager mgr = manager;
            manager = null;
            mgr.destroy();
        }
    }

    public void registerZooKeeper(ServiceReference ref) {
        destroy();
        try {
            reference = ref;
            zookeeper = (IZKClient) this.bundleContext.getService(reference);
            zookeeper.registerListener(this);
        } catch (Exception e) {
            throw new RuntimeException("Unable to start DOSGi service: " + e.getMessage(), e);
        }
    }

    public void unregisterZooKeeper(ServiceReference reference) {
        destroy();
    }

    @Override
    public void onConnected() {
        destroyManager();
        try {
            manager = new Manager(this.bundleContext, zookeeper, uri, exportedAddress);
            manager.init();
        } catch (Exception e) {
            throw new RuntimeException("Unable to start DOSGi service: " + e.getMessage(), e);
        }
    }

    @Override
    public void onDisconnected() {
        destroyManager();
    }
}
