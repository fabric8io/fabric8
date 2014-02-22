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
package io.fabric8.api;

import java.util.Map;
import java.util.Properties;

/**
 * A runtime properties provider that delegates to system properties
 */
public class DefaultRuntimeProperties implements RuntimeProperties {

    private final Properties properties = new Properties();

    public DefaultRuntimeProperties() {
    }

    public DefaultRuntimeProperties(Properties properties) {
        this.properties.putAll(properties);
    }

    @Override
    public String getProperty(String key) {
        return getPropertyInternal(key, null);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return getPropertyInternal(key, defaultValue);
    }

    @Override
    public void setProperty(String key, String value) {
        synchronized (properties) {
            properties.setProperty(key, value);
        }
    }

    @Override
    public void putProperties(Map<String, String> props) {
        putPropertiesInternal(props);
    }

    private void putPropertiesInternal(Map<String, String> props) {
        synchronized (properties) {
            this.properties.putAll(props);
        }
    }

    private String getPropertyInternal(String key, String defaultValue) {
        synchronized (properties) {
            String value = properties.getProperty(key);
            return value != null ? value : System.getProperty(key, defaultValue);
        }
    }

}
