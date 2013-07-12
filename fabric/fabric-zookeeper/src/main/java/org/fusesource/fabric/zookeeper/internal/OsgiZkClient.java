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

package org.fusesource.fabric.zookeeper.internal;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.fusesource.fabric.zookeeper.IZKClient;
import org.linkedin.util.clock.Timespan;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.linkedin.zookeeper.client.ZooKeeperFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;

@Deprecated
public class OsgiZkClient extends AbstractZKClient implements ManagedService {

    public static final String PID = "org.fusesource.fabric.zookeeper";
    
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(OsgiZkClient.class.getName());

    private ConfigurationAdmin configurationAdmin;
    private BundleContext bundleContext;
    private ServiceRegistration managedServiceRegistration;
    private ServiceRegistration zkClientRegistration;

    public OsgiZkClient() {
        super(null);
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    protected void doStart() throws InvalidSyntaxException, ConfigurationException {
        bundleContext.addServiceListener(new ServiceListener() {
            @Override
            public void serviceChanged(ServiceEvent event) {
                if (isConnected() && event.getType() == ServiceEvent.REGISTERED) {
                    LifecycleListener listener = (LifecycleListener) bundleContext.getService(event.getServiceReference());
                    try {
                        listener.onConnected();
                    } catch (Throwable e) {
                        LOG.warn("Exception while executing listener (ignored)", e);
                    } finally {
                        bundleContext.ungetService(event.getServiceReference());
                    }
                }
            }
        }, "(" + Constants.OBJECTCLASS + "=" + LifecycleListener.class.getName() + ")");

        Hashtable ht = new Hashtable();
        zkClientRegistration = bundleContext.registerService(
                new String[] { IZKClient.class.getName(), org.linkedin.zookeeper.client.IZKClient.class.getName() },
                this, ht);
        ht = new Hashtable();
        ht.put(Constants.SERVICE_PID, PID);
        managedServiceRegistration = bundleContext.registerService(ManagedService.class.getName(), this, ht);

        updated(getDefaultProperties());
    }

    private Dictionary getDefaultProperties() {
        try {
            Configuration c = configurationAdmin != null ? configurationAdmin.getConfiguration(PID, null) : null;
            return c != null ? c.getProperties() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    public void close() {
        ServiceRegistration srMs = managedServiceRegistration;
        ServiceRegistration srZk = zkClientRegistration;
        managedServiceRegistration = null;
        zkClientRegistration = null;
        if (srMs != null) {
            srMs.unregister();
        }
        if (srZk != null) {
            srZk.unregister();
        }
        super.close();
    }

    public void updated(Dictionary properties) throws ConfigurationException {
        synchronized (_lock) {
            String url = System.getProperty("zookeeper.url");
            String password = System.getProperty("zookeeper.password");
            Map<String, String> acls = new HashMap<String, String>();
            if (properties != null) {
                if (properties.get("zookeeper.url") != null) {
                    url = (String) properties.get("zookeeper.url");
                }
                if (properties.get("zookeeper.timeout") != null) {
                    sessionTimeout = Timespan.parse((String) properties.get("zookeeper.timeout"));
                }
                if (properties.get("zookeeper.password") != null) {
                    password = (String) properties.get("zookeeper.password");
                }
                for (Enumeration e = properties.keys(); e.hasMoreElements();) {
                    String key = e.nextElement().toString();
                    if (key.startsWith("acls.")) {
                        String val = properties.get(key).toString();
                        acls.put(key.substring("acls.".length()), val);
                    }
                }
            }
            if (!acls.containsKey("/")) {
                acls.put("/", "world:anyone:acdrw");
                acls.put("/fabric", "auth::acdrw,world:anyone:");
            }
            setACLs(acls);
            setPassword(password);
            if (_factory == null && url == null
                    || _factory != null && url != null && getConnectString().equals(url)) {
                // No configuration changes at all
                return;
            }
            if (isConfigured()) {
                changeState(url != null ? State.RECONNECTING : State.NONE);
                try {
                    _zk.close();
                } catch (Throwable t) {
                }
                _zk = null;
                _factory = null;
            }
            if (url != null) {
                _factory = new ZooKeeperFactory(url, sessionTimeout, this);
                tryConnect();
            }
            if (zkClientRegistration != null) {
                zkClientRegistration.setProperties(properties);
            }
        }
    }

    protected Map<Object, Boolean> callListeners(Map<Object, Boolean> history, Boolean connectedEvent) {
        Map<Object, Boolean> newHistory = super.callListeners(history, connectedEvent);
        try {
            ServiceReference[] references = bundleContext.getServiceReferences(LifecycleListener.class.getName(), null);
            if (references != null) {
                for (ServiceReference reference : references) {
                    LifecycleListener listener = (LifecycleListener) bundleContext.getService(reference);
                    Boolean previousEvent = history.get(reference);
                    // we propagate the event only if it was not already sent
                    if (previousEvent == null || previousEvent != connectedEvent) {
                        try {
                            if (connectedEvent) {
                                listener.onConnected();
                            } else {
                                listener.onDisconnected();
                            }
                        } catch (Throwable e) {
                            LOG.warn("Exception while executing listener (ignored)", e);
                        }
                    }
                    newHistory.put(reference, connectedEvent);
                    bundleContext.ungetService(reference);
                }
            }
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException(e);
        }
        return newHistory;
    }

}
