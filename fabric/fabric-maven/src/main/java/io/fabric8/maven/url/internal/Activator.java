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
package io.fabric8.maven.url.internal;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;

import io.fabric8.maven.MavenResolver;
import io.fabric8.maven.util.MavenConfiguration;
import io.fabric8.maven.util.MavenConfigurationImpl;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.ops4j.util.property.PropertyResolver;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bundle activator for protocol handlers.
 */
public class Activator extends AbstractURLStreamHandlerService
        implements BundleActivator, ManagedService
{

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger( Activator.class );

    /**
     * The PID for this service configuration
     */
    private static final String PID = "org.ops4j.pax.url.mvn";

    private static final String PROTOCOL = "mvn";

    /**
     * Bundle context in use.
     */
    private BundleContext m_bundleContext;
    /**
     * Protocol handler specific configuration.
     */
    private volatile MavenResolver m_resolver;
    /**
     * Handler service registration. Used for cleanup.
     */
    private ServiceRegistration m_handlerReg;
    /**
     * Managed service registration. Used for cleanup.
     */
    private ServiceRegistration m_managedServiceReg;
    /**
     * Managed service registration. Used for cleanup.
     */
    private final AtomicReference<ServiceRegistration> m_resolverReg = new AtomicReference<>();

    /**
     * Registers Handler as a wrap: protocol stream handler service and as a configuration managed service if
     * possible.
     *
     * @param bundleContext the bundle context.
     *
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start( final BundleContext bundleContext )
    {
        m_bundleContext = bundleContext;
        updated(null);
        registerManagedService();
        registerHandler();
    }

    /**
     * Performs cleanup:<br/>
     * * Unregister handler;<br/>
     * * Unregister managed service;<br/>
     * * Release bundle context.
     *
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop( final BundleContext bundleContext )
    {
        if ( m_handlerReg != null )
        {
            m_handlerReg.unregister();
            m_handlerReg = null;
        }
        if ( m_managedServiceReg != null )
        {
            m_managedServiceReg.unregister();
            m_managedServiceReg = null;
        }
        ServiceRegistration registration = m_resolverReg.getAndSet(null);
        if ( registration != null )
        {
            registration.unregister();
        }
        m_bundleContext = null;
        LOG.debug( "Handler for protocols " + PROTOCOL + " stopped" );
    }

    /**
     * Register the handler service.
     */
    private void registerHandler()
    {
        final Dictionary<String, Object> props = new Hashtable<>();
        props.put( URLConstants.URL_HANDLER_PROTOCOL, PROTOCOL );
        m_handlerReg = m_bundleContext.registerService(
                URLStreamHandlerService.class.getName(),
                this,
                props
        );

    }

    /**
     * Registers a managed service to listen on configuration updates.
     */
    private void registerManagedService()
    {
        final Dictionary<String, String> props = new Hashtable<>();
        props.put(Constants.SERVICE_PID, PID);
        m_managedServiceReg = m_bundleContext.registerService(
                ManagedService.class.getName(),
                this,
                props
        );
    }

    public void updated(Dictionary<String, ?> config) {
        PropertyResolver propertyResolver;
        if (config == null) {
            propertyResolver = new PropertyResolver() {
                @Override
                public String get(String propertyName) {
                    return m_bundleContext.getProperty(propertyName);
                }
            };
        } else {
            propertyResolver = new DictionaryPropertyResolver(config);
        }
        MavenConfiguration mavenConfig = new MavenConfigurationImpl(propertyResolver, PID);
        MavenResolver resolver = new AetherBasedResolver(mavenConfig);
        m_resolver = resolver;
        ServiceRegistration registration = m_bundleContext.registerService(
                MavenResolver.class.getName(),
                resolver,
                null
        );
        registration = m_resolverReg.getAndSet(registration);
        if (registration != null) {
            registration.unregister();
        }
    }

    @Override
    public URLConnection openConnection( final URL url )
            throws IOException
    {
        return new Connection( url, m_resolver );
    }

}