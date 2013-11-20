package org.fusesource.fabric.zookeeper.bootstrap;

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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.fabric.api.RuntimeProperties;
import org.fusesource.fabric.api.jcip.ThreadSafe;
import org.fusesource.fabric.api.scr.AbstractComponent;
import org.fusesource.fabric.utils.SystemProperties;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

@ThreadSafe
@Component(name = RuntimePropertiesService.COMPONENT_NAME, immediate = true)
@Service(RuntimeProperties.class)
public class RuntimePropertiesService extends AbstractComponent implements RuntimeProperties {

    public static final String COMPONENT_NAME = "org.fusesource.fabric.runtime.properties";

    private final Map<String, String> systemProperties = new ConcurrentHashMap<String, String>();

    private ComponentContext componentContext;

    @Activate
    void activate(ComponentContext componentContext) throws Exception {
        this.componentContext = componentContext;

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

    @Override
    public void removeProperty(String key) {
        assertValid();
        systemProperties.remove(key);
    }

    private String getPropertyInternal(String key, String defaultValue) {
        String result = systemProperties.get(key);
        if (result == null) {
            BundleContext syscontext = componentContext.getBundleContext();
            result = syscontext.getProperty(key);
        }
        return result != null ? result : defaultValue;
    }

    private void setPropertyInternal(String key, String value) {
        if (value != null) {
            systemProperties.put(key, value);
        }
    }

    private void assertPropertyNotNull(String propName) {
        if (getPropertyInternal(propName, null) == null)
            throw new IllegalStateException("Cannot obtain required property: " + propName);
    }
}
