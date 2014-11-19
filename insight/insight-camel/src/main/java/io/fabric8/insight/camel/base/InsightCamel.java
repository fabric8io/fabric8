/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.insight.camel.base;

import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.insight.camel.audit.Auditor;
import io.fabric8.insight.camel.breadcrumb.Breadcrumbs;
import io.fabric8.insight.camel.commands.AuditorCommand;
import io.fabric8.insight.camel.commands.BreadcrumbsCommand;
import io.fabric8.insight.camel.commands.ProfilerCommand;
import io.fabric8.insight.camel.commands.TracerCommand;
import io.fabric8.insight.camel.profiler.Profiler;
import io.fabric8.insight.camel.trace.Tracer;
import io.fabric8.insight.storage.StorageService;
import org.apache.camel.CamelContext;
import org.apache.camel.spi.Container;
import org.apache.felix.gogo.commands.basic.SimpleCommand;
import org.apache.felix.scr.annotations.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
@Component(name = InsightCamel.INSIGHT_CAMEL_PID)
public class InsightCamel implements Container {

    public static final String INSIGHT_CAMEL_PID = "io.fabric8.insight.camel";

    public static final String BREADCRUMBS = "breadcrumbs";
    public static final String PROFILER = "profiler";
    public static final String TRACER = "tracer";
    public static final String AUDITOR = "auditor";

    private static final transient Logger LOG = LoggerFactory.getLogger(InsightCamel.class);

    private final Map<String, ContainerStrategy> strategies = new HashMap<String, ContainerStrategy>();

    @Reference(referenceInterface = StorageService.class)
    private ValidatingReference<StorageService> storage = new ValidatingReference<>();

    @Reference
    private MBeanServer mbeanServer;

    private List<ServiceRegistration> commandRegistrations;

    private BundleContext bundleContext;

    public InsightCamel() {
        strategies.put(BREADCRUMBS, new Breadcrumbs());
        strategies.put(PROFILER, new Profiler());
        strategies.put(TRACER, new Tracer());
        strategies.put(AUDITOR, new Auditor(storage));
    }

    @Activate
    public void activate(BundleContext context, Map<String, ?> configuration) throws Exception {
        this.bundleContext = context;
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

        try {

            commandRegistrations = Arrays.asList(
                    SimpleCommand.export(bundleContext, AuditorCommand.class),
                    SimpleCommand.export(bundleContext, BreadcrumbsCommand.class),
                    SimpleCommand.export(bundleContext, ProfilerCommand.class),
                    SimpleCommand.export(bundleContext, TracerCommand.class));
        } catch (Exception e) {
            LOG.debug("Not registering commands - probably not running in Karaf runtime");
        }

        modified(configuration);
    }

    @Deactivate
    public void deactivate() throws Exception {
        for (ServiceRegistration sr : commandRegistrations) {
            sr.unregister();
        }
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
    }

    @Modified
    public void modified(Map<String, ?> configuration) throws ConfigurationException {
        Map<String, Map<String, String>> props = new HashMap<String, Map<String, String>>();
        for (String strategy : strategies.keySet()) {
            props.put(strategy, new HashMap<String, String>());
        }
        if (configuration != null) {
            for (Map.Entry<String, ?> entry : configuration.entrySet()) {
                for (String strategy : strategies.keySet()) {
                    if (entry.getKey().startsWith(strategy + ".")) {
                        String key = entry.getKey().substring((strategy + ".").length());
                        props.get(strategy).put(key, entry.getValue() != null ? entry.getValue().toString() : null);
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
        return new ObjectName("io.fabric8.insight:type=Camel" + strategy.getClass().getSimpleName());
    }

    public void bindStorage(StorageService storage) {
        this.storage.bind(storage);
    }

    public void unbindStorage(StorageService storage) {
        this.storage.unbind(storage);
    }
}
