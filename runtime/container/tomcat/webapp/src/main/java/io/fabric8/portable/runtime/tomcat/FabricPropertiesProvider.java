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
package io.fabric8.portable.runtime.tomcat;

import io.fabric8.api.RuntimeService;
import org.jboss.gravia.container.tomcat.support.TomcatPropertiesProvider;
import org.jboss.gravia.runtime.spi.CompositePropertiesProvider;
import org.jboss.gravia.runtime.spi.EnvPropertiesProvider;
import org.jboss.gravia.runtime.spi.PropertiesProvider;
import org.jboss.gravia.runtime.spi.SubstitutionPropertiesProvider;
import org.jboss.gravia.runtime.spi.SystemPropertiesProvider;

import javax.servlet.ServletContext;

/**
 * The Fabric {@link PropertiesProvider}
 */
public class FabricPropertiesProvider  implements PropertiesProvider {


    private final PropertiesProvider delegate;

    public FabricPropertiesProvider(ServletContext servletContext) {
        delegate = new SubstitutionPropertiesProvider(
                new CompositePropertiesProvider(
                        new TomcatPropertiesProvider(servletContext),
                        new SystemPropertiesProvider(),
                        new EnvPropertiesProvider(RuntimeService.DEFAULT_ENV_PREFIX)
                )
        );
    }

    @Override
    public Object getProperty(String key) {
        return delegate.getProperty(key);
    }

    @Override
    public Object getProperty(String key, Object defaultValue) {
        return delegate.getProperty(key, defaultValue);
    }

}
