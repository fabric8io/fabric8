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

package io.fabric8.virt.service;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibvirtServiceFactory implements ManagedServiceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(LibvirtServiceFactory.class);

    public static final String URL = "url";

    private final Map<String, ServiceRegistration> registrations = new ConcurrentHashMap<String, ServiceRegistration>();
    private final BundleContext bundleContext;

    /**
     * Constructor
     *
     * @param bundleContext
     */
    public LibvirtServiceFactory(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * Return a descriptive name of this factory.
     *
     * @return the name for the factory, which might be localized
     */
    @Override
    public String getName() {
        return "LibVirt Service Factory";
    }

    /**
     * Create a new instance, or update the configuration of an existing
     * instance.
     * <p/>
     * If the PID of the <code>Configuration</code> object is new for the
     * Managed Service Factory, then create a new factory instance, using the
     * configuration <code>properties</code> provided. Else, update the
     * service instance with the provided <code>properties</code>.
     * <p/>
     * <p/>
     * If the factory instance is registered with the Framework, then the
     * configuration <code>properties</code> should be copied to its registry
     * properties. This is not mandatory and security sensitive properties
     * should obviously not be copied.
     * <p/>
     * <p/>
     * If this method throws any <code>Exception</code>, the Configuration
     * Admin service must catch it and should log it.
     * <p/>
     * <p/>
     * When the implementation of updated detects any kind of error in the
     * configuration properties, it should create a new
     * {@link org.osgi.service.cm.ConfigurationException} which describes the problem.
     * <p/>
     * <p/>
     * The Configuration Admin service must call this method asynchronously.
     * This implies that implementors of the <code>ManagedServiceFactory</code>
     * class can be assured that the callback will not take place during
     * registration when they execute the registration in a synchronized method.
     *
     * @param pid        The PID for this configuration.
     * @param properties A copy of the configuration properties. This argument
     *                   must not contain the service.bundleLocation" property. The value
     *                   of this property may be obtained from the
     *                   <code>Configuration.getBundleLocation</code> method.
     * @throws org.osgi.service.cm.ConfigurationException
     *          when the configuration properties are
     *          invalid.
     */
    @Override
    public void updated(String pid, Dictionary properties) throws ConfigurationException {
        ServiceRegistration newRegistration = null;
        try {
            if (properties != null) {
                Properties props = new Properties();
                for (Enumeration e = properties.keys(); e.hasMoreElements(); ) {
                    Object key = e.nextElement();
                    Object val = properties.get(key);
                    props.put(key, val);
                }
                String url = (String) properties.get(URL);

                try {
                    Connect connect = new Connect(url, false);
                    newRegistration = bundleContext.registerService(
                            Connect.class.getName(), connect, properties);
                } catch (LibvirtException e) {
                    LOG.error("Error creating libvirt connection", e);
                }
            }
        } finally {
            ServiceRegistration oldRegistration = (newRegistration == null)
                    ? registrations.remove(pid)
                    : registrations.put(pid, newRegistration);
            if (oldRegistration != null) {
                System.out.println("Unregistering libvirt connection " + pid);
                oldRegistration.unregister();
            }
        }
    }

    /**
     * Remove a factory instance.
     * <p/>
     * Remove the factory instance associated with the PID. If the instance was
     * registered with the service registry, it should be unregistered.
     * <p/>
     * If this method throws any <code>Exception</code>, the Configuration
     * Admin service must catch it and should log it.
     * <p/>
     * The Configuration Admin service must call this method asynchronously.
     *
     * @param pid the PID of the service to be removed
     */
    @Override
    public void deleted(String pid) {
        ServiceRegistration oldRegistration = registrations.remove(pid);
        if (bundleContext != null) {
            Connect connect = (Connect) bundleContext.getService(oldRegistration.getReference());
            try {
                connect.close();
            } catch (LibvirtException e) {
                LOG.error("Error closing libvirt connection", e);
            }
        }
        if (oldRegistration != null) {
            oldRegistration.unregister();
        }
    }
}

