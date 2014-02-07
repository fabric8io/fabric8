package io.fabric8.zookeeper.bootstrap;

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

import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.utils.SystemProperties;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

@ThreadSafe
@Component(name = RuntimePropertiesService.COMPONENT_NAME, label = "Fabric8 Runtime Properties Service", immediate = true, metatype = false)
@Service(RuntimeProperties.class)
public class RuntimePropertiesService extends AbstractComponent implements RuntimeProperties {

    public static final String COMPONENT_NAME = "io.fabric8.runtime.properties";
    static final String ENV_PREFIX = "env.prefix";
    static final String DEFAULT_ENV_PREFIX = "FABRIC8_";
    static final String REPLACE_PATTERN = "-|\\.";

    private final Map<String, String> runtimeProperties = new ConcurrentHashMap<String, String>();

    @Property(name = RuntimePropertiesService.ENV_PREFIX, label = "Environment Variable Prefix", value = DEFAULT_ENV_PREFIX)
    private String envPrefix = DEFAULT_ENV_PREFIX;

    private ComponentContext componentContext;

    @Activate
    void activate(ComponentContext componentContext) throws Exception {
        this.componentContext = componentContext;
        this.envPrefix = (String) componentContext.getProperties().get(ENV_PREFIX);

        // Assert some required properties
        assertPropertyNotNull(SystemProperties.KARAF_HOME);
        assertPropertyNotNull(SystemProperties.KARAF_BASE);
        assertPropertyNotNull(SystemProperties.KARAF_NAME);
        assertPropertyNotNull(SystemProperties.KARAF_DATA);

        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public String getProperty(String key) {
        assertValid();
        return getPropertyInternal(key, null);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        assertValid();
        return getPropertyInternal(key, defaultValue);
    }

    @Override
    public void setProperty(String key, String value) {
        assertValid();
        setPropertyInternal(key, value);
    }

    private String getPropertyInternal(String key, String defaultValue) {
        String result = runtimeProperties.get(key);
        if (result == null) {
            BundleContext syscontext = componentContext.getBundleContext();
            result = syscontext.getProperty(key);
        }
        if (result == null) {
            result =  System.getenv(toEnvVariable(envPrefix, key));
        }
        return result != null ? result : defaultValue;
    }

    private void setPropertyInternal(String key, String value) {
        if (value != null) {
            runtimeProperties.put(key, value);
        }
    }

    @Override
    public void putProperties(Map<String, String> properties) {
        runtimeProperties.putAll(properties);
    }

    private void assertPropertyNotNull(String propName) {
        if (getPropertyInternal(propName, null) == null)
            throw new IllegalStateException("Cannot obtain required property: " + propName);
    }

    static String toEnvVariable(String prefix, String name) {
        if (name == null || name.isEmpty()) {
            return name;
        } else {
            return prefix + name.replaceAll(REPLACE_PATTERN,"_").toUpperCase();
        }
    }
}
