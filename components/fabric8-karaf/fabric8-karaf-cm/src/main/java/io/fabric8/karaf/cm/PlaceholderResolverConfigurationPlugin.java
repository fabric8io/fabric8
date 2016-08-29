/**
 * Copyright 2005-2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.karaf.cm;

import java.util.Dictionary;
import java.util.concurrent.atomic.AtomicReference;

import io.fabric8.karaf.core.properties.PlaceholderResolver;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.fabric8.karaf.cm.KubernetesConstants.FABRIC8_CONFIG_PLUGIN_ENABLED;
import static io.fabric8.karaf.cm.KubernetesConstants.FABRIC8_CONFIG_PLUGIN_ENABLED_DEFAULT;
import static io.fabric8.kubernetes.client.utils.Utils.getSystemPropertyOrEnvVar;

@Component(
    immediate = true,
    policy = ConfigurationPolicy.IGNORE,
    createPid = false
)
@Reference(
    name = "resolver",
    cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
    policy = ReferencePolicy.DYNAMIC,
    referenceInterface = PlaceholderResolver.class
)
@Properties({
    @Property(name = ConfigurationPlugin.CM_RANKING, value = "10", classValue = Integer.class)
})
@Service(ConfigurationPlugin.class)
public class PlaceholderResolverConfigurationPlugin implements ConfigurationPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlaceholderResolverConfigurationPlugin.class);

    private final AtomicReference<PlaceholderResolver> resolver;
    private boolean enabled;

    public PlaceholderResolverConfigurationPlugin() {
        this.resolver = new AtomicReference<>();
        this.enabled = FABRIC8_CONFIG_PLUGIN_ENABLED_DEFAULT;
    }

    // ***********************
    // Lifecycle
    // ***********************

    @Activate
    void activate() {
        enabled = getSystemPropertyOrEnvVar(FABRIC8_CONFIG_PLUGIN_ENABLED, enabled);
        LOGGER.debug("Configuration update is {}", enabled ? "ENABLED" : "DISABLED");
    }

    // ****************************
    // Binding
    // ****************************

    protected void bindResolver(PlaceholderResolver resolver) {
        this.resolver.set(resolver);
    }

    protected void unbindResolver(PlaceholderResolver resolver) {
        this.resolver.compareAndSet(resolver, null);
    }

    // ***********************
    // ConfigurationPlugin
    // ***********************

    @Override
    public void modifyConfiguration(ServiceReference<?> reference, Dictionary<String, Object> dictionary) {
        PlaceholderResolver res = resolver.get();
        if (enabled && res != null) {
            res.replaceAll(dictionary);
        }
    }
}
