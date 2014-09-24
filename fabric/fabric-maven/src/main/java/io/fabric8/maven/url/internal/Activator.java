/*
 * Copyright 2007 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.maven.url.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import io.fabric8.maven.url.ServiceConstants;
import io.fabric8.maven.util.MavenConfiguration;
import io.fabric8.maven.util.MavenConfigurationImpl;
import org.ops4j.pax.url.commons.handler.ConnectionFactory;
import org.ops4j.pax.url.commons.handler.HandlerActivator;
import org.ops4j.util.property.PropertyResolver;
import org.osgi.framework.BundleContext;

/**
 * Bundle activator for mvn: protocol handler
 */
public final class Activator
    extends HandlerActivator<MavenConfiguration>
{

    /**
     * @see HandlerActivator#HandlerActivator(String[], String, org.ops4j.pax.url.commons.handler.ConnectionFactory)
     */
    public Activator()
    {
        super(
            new String[]{ ServiceConstants.PROTOCOL },
            ServiceConstants.PID,
            new ConnectionFactory<MavenConfiguration>()
            {

                /**
                 * @see ConnectionFactory#createConection(org.osgi.framework.BundleContext, java.net.URL, Object)
                 */
                public URLConnection createConection( final BundleContext bundleContext,
                                                      final URL url,
                                                      final MavenConfiguration config )
                    throws MalformedURLException
                {
                    return new Connection( url, config );
                }

                /**
                 * @see ConnectionFactory#createConfiguration(org.ops4j.util.property.PropertyResolver)
                 */
                public MavenConfiguration createConfiguration( final PropertyResolver propertyResolver )
                {
                    final MavenConfigurationImpl config =
                        new MavenConfigurationImpl( propertyResolver, ServiceConstants.PID );
                    if (!config.isValid())
                    {
                        return null;
                    }
                    return config;
                }
            }
        );
    }

}