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
package org.fusesource.mq.fabric;

import org.apache.curator.framework.CuratorFramework;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsgiFabricDiscoveryAgent extends FabricDiscoveryAgent implements ServiceTrackerCustomizer {
    private static final Logger LOG = LoggerFactory.getLogger(OsgiFabricDiscoveryAgent.class);

    ServiceTracker tracker;
    BundleContext context;

    public OsgiFabricDiscoveryAgent() {
        if (FrameworkUtil.getBundle(getClass()) != null) {
            context = FrameworkUtil.getBundle(getClass()).getBundleContext();
            tracker = new ServiceTracker(context, CuratorFramework.class.getName(), this);
            tracker.open();
        }
    }

    @Override
    public Object addingService(ServiceReference serviceReference) {
        curator = (CuratorFramework) context.getService(serviceReference);
        return curator;
    }

    @Override
    public void modifiedService(ServiceReference serviceReference, Object o) {
    }

    @Override
    public void removedService(ServiceReference serviceReference, Object o) {
    }

    @Override
    public synchronized void stop() throws Exception {
        super.stop();

        if (tracker != null) {
            LOG.info("closing tracker");
            tracker.close();
        }
    }
}
