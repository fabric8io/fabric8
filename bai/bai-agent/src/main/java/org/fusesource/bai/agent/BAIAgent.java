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
package org.fusesource.bai.agent;

import org.apache.camel.CamelContext;
import org.apache.camel.core.osgi.OsgiCamelContextPublisher;
import org.apache.camel.spi.EventNotifier;
import org.apache.camel.spi.ManagementStrategy;
import org.apache.camel.util.ServiceHelper;
import org.fusesource.bai.AuditEventNotifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Listens to non-audit CamelContext's and registers an {@link AuditEventNotifier} with them
 * so that the contexts are audited according to the audit rules.
 * <p>
 * Uses an <code>auditEndpoint</code> to write AuditEvent objects to which can then be processed
 * in any regular Camel way or can be bound to a BAI back end service such as bai-mongodb-backend
 */
public class BAIAgent implements ServiceListener {
    private static final transient Logger LOG = LoggerFactory.getLogger(BAIAgent.class);

    private BundleContext bundleContext;
    private Map<String, NotifierRegistration> notifierMap = new HashMap<String, NotifierRegistration>();
    private String auditEndpoint = "vm:audit";

    public void init() throws Exception {
        bundleContext.addServiceListener(this);
    }

    public synchronized void destroy() {
        Set<String> ids = notifierMap.keySet();
        for (String id : ids) {
            removeNotifier(id);
        }
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public String getAuditEndpoint() {
        return auditEndpoint;
    }

    public void setAuditEndpoint(String auditEndpoint) {
        this.auditEndpoint = auditEndpoint;
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        ServiceReference reference = event.getServiceReference();
        if (bundleContext != null && reference != null) {
            int type = event.getType();
            Object service = bundleContext.getService(reference);
            if (service instanceof CamelContext) {
                CamelContext camelContext = (CamelContext) service;
                String name = camelContext.getName();
                String camelContextSymbolicName = getCamelSymbolicName(reference);
                String id = getCamelContextUUID(camelContext, reference, camelContextSymbolicName);
                if (name.startsWith("audit-")) {
                    LOG.debug("Ignoring camel context " + id + " as its an audit context");
                    return;
                }

                if (type == ServiceEvent.UNREGISTERING) {
                    removeNotifier(id);
                } else if (type == ServiceEvent.REGISTERED) {
                    Map<String, Object> properties = new HashMap<String, Object>();
                    String[] keys = reference.getPropertyKeys();
                    if (keys != null) {
                        for (String key : keys) {
                            Object value = reference.getProperty(key);
                            properties.put(key, value);
                        }
                    }

                    ManagementStrategy managementStrategy = camelContext.getManagementStrategy();
                    if (managementStrategy != null) {
                        EventNotifier notifier = createEventNotifier(camelContext, reference, camelContextSymbolicName);
                        if (notifier == null) {
                            LOG.warn("Could not create an EventNotifier for CamelContext " + camelContextSymbolicName);
                        } else {
                            try {
                                addNotifier(id, notifier, managementStrategy);
                            } catch (Exception e) {
                                LOG.error("Failed to start " + notifier + " for CamelContext " + id + ". Reason: " + e, e);
                            }
                        }
                    }
                }
            }
        }
    }

    private String getCamelSymbolicName(ServiceReference reference) {
        String camelContextSymbolicName = null;
        Object value = reference.getProperty(OsgiCamelContextPublisher.CONTEXT_SYMBOLIC_NAME_PROPERTY);
        if (value != null) {
            camelContextSymbolicName = value.toString();
        }
        if (camelContextSymbolicName == null) {
            camelContextSymbolicName = reference.getBundle().getSymbolicName();
        }
        return camelContextSymbolicName;
    }

    protected synchronized NotifierRegistration addNotifier(String id, EventNotifier notifier, ManagementStrategy managementStrategy) throws Exception {
        removeNotifier(id);

        NotifierRegistration registration = new NotifierRegistration(id, notifier, managementStrategy);
        ServiceHelper.startService(registration);
        notifierMap.put(id, registration);
        return registration;
    }

    protected synchronized void removeNotifier(String id) {
        NotifierRegistration registration = notifierMap.remove(id);
        if (registration != null) {
            try {
                ServiceHelper.stopAndShutdownService(registration);
            } catch (Exception e) {
                LOG.error("Failed to stop registration " + registration + " for CamelContext " + id + ". Reason: " + e, e);
            }
        }
    }

    private String getCamelContextUUID(CamelContext camelContext, ServiceReference reference, String camelContextSymbolicName) {
        return camelContextSymbolicName + "." + camelContext.getManagementName();
    }

    protected EventNotifier createEventNotifier(CamelContext camelContext, ServiceReference reference, String camelContextSymbolicName) {
        AuditEventNotifier notifier = new AuditEventNotifier();
        notifier.setCamelContext(camelContext);
        notifier.setEndpointUri(auditEndpoint);
        return notifier;
    }

}
