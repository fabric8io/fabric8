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
package io.fabric8.service;

import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.api.scr.support.ConfigInjection;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.spi.CompositePropertiesProvider;
import org.jboss.gravia.runtime.spi.EnvPropertiesProvider;
import org.jboss.gravia.runtime.spi.MapPropertiesProvider;
import org.jboss.gravia.runtime.spi.PropertiesProvider;
import org.jboss.gravia.runtime.spi.SubstitutionPropertiesProvider;
import org.jboss.gravia.runtime.spi.SystemPropertiesProvider;
import org.jboss.gravia.utils.IllegalStateAssertion;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Component(immediate = true)
@Service(Configurer.class)
public class ComponentConfigurer extends AbstractComponent implements Configurer {

    private static final String BOX_FORMAT = "${%s}";

    @Reference(referenceInterface = Runtime.class)
    private ValidatingReference<Runtime> runtime = new ValidatingReference<>();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public <T> Map<String, ?> configure(final Dictionary<String, ?> configuration, T target, String... ignorePrefix) throws Exception {
        assertValid();
        Map<String, Object> mapConfiguration = new HashMap<>();
        for (Enumeration<String> keys = configuration.keys(); keys.hasMoreElements();) {
            String key = keys.nextElement();
            mapConfiguration.put(key, configuration.get(key));
        }
        return configure(mapConfiguration, target, ignorePrefix);
    }

    @Override
    public <T> Map<String, ?> configure(final Map<String, ?> configuration, T target, String... ignorePrefix) throws Exception {
        assertValid();
        Map<String, Object> result = new HashMap<>();
        PropertiesProvider envPropertiesProvider = new EnvPropertiesProvider("FABRIC8_");
        PropertiesProvider systemPropertiesProvider = new SystemPropertiesProvider();
        final Runtime runtime = this.runtime.get();
        final PropertiesProvider configurationProvider = new MapPropertiesProvider((Map<String, Object>) configuration);
        final PropertiesProvider fallbackPropertiesProvider = new CompositePropertiesProvider(systemPropertiesProvider, envPropertiesProvider);
        final PropertiesProvider[] propertiesProviders = new PropertiesProvider[]{configurationProvider, fallbackPropertiesProvider};

        PropertiesProvider provider = new SubstitutionPropertiesProvider(new PropertiesProvider() {
            @Override
            public Object getProperty(String key) {
                return runtime.getProperty(key);
            }

            @Override
        	public Object getRequiredProperty(String key) {
                Object value = getProperty(key, null);
                IllegalStateAssertion.assertNotNull(value, "Cannot obtain property: " + key);
        		return value;
        	}
            
            @Override
            public Object getProperty(String key, Object defaultValue) {
                Object value = null;
                for (PropertiesProvider p : propertiesProviders) {
                    value = p.getProperty(key);
                    if (value != null && !isCyclicReference(key, value)) {
                        return value;
                    }
                }
                return defaultValue;
            }
        });

        for (Map.Entry<String, ?> entry : configuration.entrySet()) {
            String key = entry.getKey();
            Object value = provider.getProperty(key);
            result.put(key, value);
        }
        ConfigInjection.applyConfiguration(result, target, ignorePrefix);
        return result;
    }

    private static boolean isCyclicReference(String key, Object value) {
        if (!(value instanceof String)) {
            return false;
        } else return String.format(BOX_FORMAT, key).equals(value);
    }

    void bindRuntime(Runtime service) {
        runtime.bind(service);
    }

    void unbindRuntime(Runtime service) {
        runtime.unbind(service);
    }
}
