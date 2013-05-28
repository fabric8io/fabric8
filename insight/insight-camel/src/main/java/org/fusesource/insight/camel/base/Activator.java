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
package org.fusesource.insight.camel.base;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.Container;
import org.fusesource.insight.camel.audit.Auditor;
import org.fusesource.insight.camel.audit.StorageProxy;
import org.fusesource.insight.camel.breadcrumb.Breadcrumbs;
import org.fusesource.insight.camel.profiler.Profiler;
import org.fusesource.insight.camel.trace.Tracer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class Activator implements BundleActivator, Container {

    private static final transient Logger LOG = LoggerFactory.getLogger(Activator.class);

    private final List<ContainerStrategy> strategies = new ArrayList<ContainerStrategy>();
    private StorageProxy storageProxy = new StorageProxy();
    private BundleContext bundleContext;
    private MBeanServer mbeanServer;

    public Activator() {
        strategies.add(new Breadcrumbs());
        strategies.add(new Profiler());
        strategies.add(new Tracer());
        strategies.add(new Auditor(storageProxy));
    }

    @Override
    public void start(BundleContext context) throws Exception {
        this.bundleContext = context;
        storageProxy.setContext(context);
        storageProxy.init();
        mbeanServer = lookupMBeanServer();
        if (mbeanServer != null) {
            for (ContainerStrategy strategy : strategies) {
                try {
                    mbeanServer.registerMBean(strategy, getObjectName(strategy));
                } catch (Exception e) {
                    LOG.warn("An error occured during mbean server unregistration: " + e, e);
                }
            }
        }
        Container.Instance.set(this);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        Container.Instance.set(null);
        if (mbeanServer != null) {
            for (ContainerStrategy strategy : strategies) {
                try {
                    mbeanServer.unregisterMBean(getObjectName(strategy));
                } catch (Exception e) {
                    LOG.warn("An error occured during mbean server unregistration: " + e, e);
                }
            }
        }
        storageProxy.destroy();
    }

    @Override
    public void manage(CamelContext camelContext) {
        for (ContainerStrategy strategy : strategies) {
            try {
                strategy.manage(camelContext);
            } catch (Exception e) {
                LOG.error("Error managing CamelContext " + camelContext, e);
            }
        }
    }

    protected ObjectName getObjectName(ContainerStrategy strategy) throws MalformedObjectNameException {
        return new ObjectName("org.fusesource.insight:type=Camel" + strategy.getClass().getSimpleName());
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
