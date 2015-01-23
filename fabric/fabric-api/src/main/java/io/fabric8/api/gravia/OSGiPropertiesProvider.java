/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.api.gravia;

import org.osgi.framework.BundleContext;

/**
 * The default {@link PropertiesProvider} for OSGi runtimes.
 */
public class OSGiPropertiesProvider extends AbstractPropertiesProvider {

    private final PropertiesProvider delegate;

    public OSGiPropertiesProvider(BundleContext bundleContext) {
        this(bundleContext, null);
    }

    public OSGiPropertiesProvider(BundleContext bundleContext, String environmentVariablePrefix) {
        PropertiesProvider system = new SystemPropertiesProvider();
        PropertiesProvider env =  environmentVariablePrefix != null ? new EnvPropertiesProvider(environmentVariablePrefix) : new EnvPropertiesProvider(system);

        this.delegate = new SubstitutionPropertiesProvider(
                new CompositePropertiesProvider(
                        new BundleContextPropertiesProvider(bundleContext),
                        system,
                        env
                )
        );
    }

    @Override
    public Object getProperty(String key, Object defaultValue) {
        return delegate.getProperty(key, defaultValue);
    }
}