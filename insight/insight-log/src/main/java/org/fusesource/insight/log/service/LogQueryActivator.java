/*
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
package org.fusesource.insight.log.service;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;

/**
 * Activator for creating a {@link LogQuery}
 */
public class LogQueryActivator implements BundleActivator {
    private static final transient Logger LOG = LoggerFactory.getLogger(LogQueryActivator.class);

    private BundleContext bundleContext;
    private MBeanServer mbeanServer;
    private LogQuery logQuery;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        this.bundleContext = bundleContext;
        mbeanServer = lookupMBeanServer();
        logQuery = new LogQuery();
        logQuery.setBundleContext(bundleContext);
        logQuery.registerMBeanServer(mbeanServer);
        logQuery.init();
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        if (logQuery != null) {
            if (mbeanServer != null) {
                logQuery.unregisterMBeanServer(mbeanServer);
            }
            logQuery.destroy();
        }
    }

    protected MBeanServer lookupMBeanServer() {
        ServiceReference ref = bundleContext.getServiceReference(MBeanServer.class.getName());
        if (ref != null) {
            return (MBeanServer) bundleContext.getService(ref);
        } else {
            LOG.warn("Could not find MBeanServer in the OSGi registry so using the platform MBeanServer instead");
            return ManagementFactory.getPlatformMBeanServer();
        }
    }
}
