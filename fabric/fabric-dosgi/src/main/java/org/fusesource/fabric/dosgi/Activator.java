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
            throw new RuntimeException("Unable to start DOSGi service: " + e.getMessage(), e);
        }
    }

    public void unregisterZooKeeper(ServiceReference reference) {
        destroy();
    }

}
