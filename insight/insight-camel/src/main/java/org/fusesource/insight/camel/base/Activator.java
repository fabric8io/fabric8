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
import org.apache.felix.gogo.commands.basic.SimpleCommand;
import org.fusesource.insight.camel.audit.Auditor;
import org.fusesource.insight.camel.audit.StorageProxy;
import org.fusesource.insight.camel.breadcrumb.Breadcrumbs;
import org.fusesource.insight.camel.commands.AuditorCommand;
import org.fusesource.insight.camel.commands.BreadcrumbsCommand;
import org.fusesource.insight.camel.commands.ProfilerCommand;
import org.fusesource.insight.camel.commands.TracerCommand;
import org.fusesource.insight.camel.profiler.Profiler;
import org.fusesource.insight.camel.trace.Tracer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class Activator implements BundleActivator, Container, ManagedService {

    public static final String INSIGHT_CAMEL_PID = "org.fusesource.insight.camel";

    public static final String BREADCRUMBS = "breadcrumbs";
    public static final String PROFILER = "profiler";
    public static final String TRACER = "tracer";
    public static final String AUDITOR = "auditor";

    private static final transient Logger LOG = LoggerFactory.getLogger(Activator.class);

    private final Map<String, ContainerStrategy> strategies = new HashMap<String, ContainerStrategy>();
    private StorageProxy storageProxy = new StorageProxy();
    private BundleContext bundleContext;
    private MBeanServer mbeanServer;
    private ServiceRegistration<ManagedService> registration;
    private List<ServiceRegistration> commandRegistrations;

    public Activator() {
        strategies.put(BREADCRUMBS, new Breadcrumbs());
        strategies.put(PROFILER, new Profiler());
        strategies.put(TRACER, new Tracer());
        strategies.put(AUDITOR, new Auditor(storageProxy));
    }

    @Override
    public void start(BundleContext context) throws Exception {
        this.bundleContext = context;
        storageProxy.setContext(context);
        storageProxy.init();
        mbeanServer = lookupMBeanServer();
        if (mbeanServer != null) {
            for (ContainerStrategy strategy : strategies.values()) {
                try {
                    mbeanServer.registerMBean(strategy, getObjectName(strategy));
                } catch (Exception e) {
                    LOG.warn("An error occured during mbean server unregistration: " + e, e);
                }
            }
        }
        Container.Instance.set(this);
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, INSIGHT_CAMEL_PID);
        registration = bundleContext.registerService(ManagedService.class, this, props);
        commandRegistrations = Arrays.asList(
                SimpleCommand.export(bundleContext, AuditorCommand.class),
                SimpleCommand.export(bundleContext, BreadcrumbsCommand.class),
                SimpleCommand.export(bundleContext, ProfilerCommand.class),
                SimpleCommand.export(bundleContext, TracerCommand.class));
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        for (ServiceRegistration sr : commandRegistrations) {
            sr.unregister();
        }
        registration.unregister();
        Container.Instance.set(null);
        if (mbeanServer != null) {
            for (ContainerStrategy strategy : strategies.values()) {
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
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        Map<String, Map<String, String>> props = new HashMap<String, Map<String, String>>();
        for (String strategy : strategies.keySet()) {
            props.put(strategy, new HashMap<String, String>());
        }
        if (properties != null) {
            for (Enumeration<String> es = properties.keys(); es.hasMoreElements();) {
                String key = es.nextElement();
                Object val = properties.get(key);
                for (String strategy : strategies.keySet()) {
                    if (key.startsWith(strategy + ".")) {
                        key = key.substring((strategy + ".").length());
                        props.get(strategy).put(key, val != null ? val.toString() : null);
                    }
                }
            }
        }
        for (String key : strategies.keySet()) {
            Map<String, String> p = props.get(key);
            strategies.get(key).update(p);
        }
    }

    @Override
    public void manage(CamelContext camelContext) {
        for (ContainerStrategy strategy : strategies.values()) {
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
