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
package io.fabric8.runtime.container.karaf;

import io.fabric8.api.EnvironmentVariables;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.gravia.EnvPropertiesProvider;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractRuntimeProperties;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;

@ThreadSafe
@Component(label = "Karaf Runtime Properties Service", immediate = true, metatype = false)
@Service(RuntimeProperties.class)
public class KarafPropertiesService extends AbstractRuntimeProperties {

    private BundleContext bundleContext;

    @Activate
    void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        activateInternal();
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    protected String getPropertyInternal(String key, String defaultValue) {
        String value = bundleContext.getProperty(key);
        return value != null ? value : defaultValue;
    }

    private void activateInternal() {
        if (bundleContext.getProperty(EnvPropertiesProvider.ENV_PREFIX_KEY) == null) {
            System.setProperty(EnvPropertiesProvider.ENV_PREFIX_KEY,  "FABRIC8_");
        }
        if (bundleContext.getProperty(RUNTIME_IDENTITY) == null) {
            System.setProperty(RUNTIME_IDENTITY, System.getProperty("karaf.name", System.getenv(EnvironmentVariables.RUNTIME_ID)));
        }
        if (bundleContext.getProperty(RUNTIME_HOME_DIR) == null) {
            System.setProperty(RUNTIME_HOME_DIR, System.getProperty("karaf.home"));
        }
        if (bundleContext.getProperty(RUNTIME_DATA_DIR) == null) {
            System.setProperty(RUNTIME_DATA_DIR, System.getProperty("karaf.data"));
        }
        if (bundleContext.getProperty(RUNTIME_CONF_DIR) == null) {
            System.setProperty(RUNTIME_CONF_DIR, System.getProperty("karaf.etc"));
        }
    }
}
