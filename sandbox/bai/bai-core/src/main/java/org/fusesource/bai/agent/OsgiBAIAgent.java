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
import org.apache.camel.spi.ManagementStrategy;
import org.apache.camel.util.ServiceHelper;
import org.fusesource.bai.AuditEventNotifier;
import org.fusesource.bai.agent.support.ConfigAdminAuditPolicy;
import org.fusesource.bai.agent.support.NotifierRegistration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Listens to non-audit CamelContext's and registers an {@link AuditEventNotifier} with them
 * so that the contexts are audited according to the audit rules.
 * <p/>
 * Uses an <code>auditEndpoint</code> to write AuditEvent objects to which can then be processed
 * in any regular Camel way or can be bound to a BAI back end service such as bai-mongodb-backend
 */
public class OsgiBAIAgent implements ServiceListener, BAIAgent {
    private static final transient Logger LOG = LoggerFactory.getLogger(OsgiBAIAgent.class);

    private BundleContext bundleContext;
    private Map<String, NotifierRegistration> notifierMap = new HashMap<String, NotifierRegistration>();
    private String auditEndpoint = "vm:audit";
    private AuditPolicy auditPolicy = new ConfigAdminAuditPolicy();

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

    public AuditPolicy getAuditPolicy() {
        return auditPolicy;
    }

    public void setAuditPolicy(AuditPolicy auditPolicy) {
        this.auditPolicy = auditPolicy;
        if (auditPolicy != null) {
            auditPolicy.setAgent(this);
        }
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        ServiceReference reference = event.getServiceReference();
        if (bundleContext != null && reference != null) {
            int type = event.getType();
            Object service = bundleContext.getService(reference);
            if (service instanceof CamelContext) {
                CamelContext camelContext = (CamelContext) service;
                String camelContextSymbolicName = getCamelSymbolicName(reference);
                String id = getCamelContextUUID(camelContext, reference, camelContextSymbolicName);

                CamelContextService camelContextService = new CamelContextService(camelContext, reference);

                if (!getAuditPolicy().isAuditEnabled(camelContextService)) {
                    LOG.debug("Ignoring camel context " + id + " as it matches exclude filters in the policy configuration");
                    return;
                }

                if (type == ServiceEvent.UNREGISTERING) {
                    LOG.info("Removing audit notifiers from camel context " + id);
                    removeNotifier(id);
                } else if (type == ServiceEvent.REGISTERED) {
                    LOG.info("Instrumenting camel context " + id + " with audit notifiers");
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
                        AuditEventNotifier notifier = createEventNotifier(camelContextService);
                        if (notifier == null) {
                            LOG.warn("Could not create an EventNotifier for CamelContext " + camelContextSymbolicName);
                        } else {
                            try {
                                addNotifier(id, camelContextService, notifier, managementStrategy);
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

    protected synchronized NotifierRegistration addNotifier(String id, CamelContextService camelContextService, AuditEventNotifier notifier, ManagementStrategy managementStrategy) throws Exception {
        removeNotifier(id);

        NotifierRegistration registration = new NotifierRegistration(id, camelContextService, notifier, managementStrategy);
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


    /**
     * Factory method for creating an AuditEventNotifier using the audit policy
     */
    protected AuditEventNotifier createEventNotifier(CamelContextService camelContextService) {
        AuditEventNotifier notifier = getAuditPolicy().createAuditNotifier(camelContextService);
        getAuditPolicy().configureNotifier(camelContextService, notifier);
        notifier.setCamelContext(camelContextService.getCamelContext());
        notifier.setEndpointUri(auditEndpoint);
        return notifier;
    }

    /**
     * If a policy has been changed this method reconfigures all the active policies
     */
    @Override
    public void reconfigureNotifiers() {
        List<NotifierRegistration> list;
        synchronized (this) {
            list = new ArrayList<NotifierRegistration>(notifierMap.values());
        }

        for (NotifierRegistration registration : list) {
            CamelContextService camelContextService = registration.getCamelContextService();
            AuditEventNotifier notifier = registration.getNotifier();
            getAuditPolicy().configureNotifier(camelContextService, notifier);
        }
    }
}
