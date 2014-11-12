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
package io.fabric8.api.scr;

import io.fabric8.api.RuntimeProperties;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import io.fabric8.api.gravia.IllegalArgumentAssertion;
import io.fabric8.api.gravia.IllegalStateAssertion;

public abstract class AbstractRuntimeProperties extends AbstractComponent implements RuntimeProperties {

    private final Map<String, Object> attributes = new HashMap<>();
    private String identity;
    private Path homePath;
    private Path dataPath;
    private Path confPath;

    @Override
    public void activateComponent() {
        identity = getRequiredProperty(RUNTIME_IDENTITY);
        homePath = Paths.get(getRequiredProperty(RUNTIME_HOME_DIR));
        dataPath = Paths.get(getRequiredProperty(RUNTIME_DATA_DIR));
        confPath = Paths.get(getRequiredProperty(RUNTIME_CONF_DIR));
        super.activateComponent();
    }

    @Override
    public void deactivateComponent() {
        super.deactivateComponent();
    }

    @Override
    public String getRuntimeIdentity() {
        return identity;
    }

    @Override
    public Path getHomePath() {
        return homePath;
    }

    @Override
    public Path getConfPath() {
        return confPath;
    }

    @Override
    public Path getDataPath() {
        return dataPath;
    }

    @Override
    public String getProperty(String key) {
        return getPropertyInternal(key, null);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return getPropertyInternal(key, defaultValue);
    }

    abstract protected String getPropertyInternal(String key, String defaultValue);

    private String getRequiredProperty(String propName) {
        String result = getPropertyInternal(propName, null);
        IllegalStateAssertion.assertNotNull(result, "Cannot obtain required property: " + propName);
        return result;
    }

    @Override
    public <T> void putRuntimeAttribute(Class<T> key, T value) {
        IllegalArgumentAssertion.assertNotNull(key, "key");
        IllegalArgumentAssertion.assertNotNull(value, "value");
        synchronized (attributes) {
            // Use string to normalize keys from different class loaders
            Object exist = attributes.get(key.getName());
            IllegalStateAssertion.assertNull(exist, "Runtime already contains attribute: " + exist);
            attributes.put(key.getName(), value);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getRuntimeAttribute(Class<T> key) {
        IllegalArgumentAssertion.assertNotNull(key, "key");
        synchronized (attributes) {
            return (T) attributes.get(key.getName());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T removeRuntimeAttribute(Class<T> key) {
        IllegalArgumentAssertion.assertNotNull(key, "key");
        synchronized (attributes) {
            return (T) attributes.remove(key.getName());
        }
    }

    @Override
    public <T> void clearRuntimeAttributes() {
        synchronized (attributes) {
            attributes.clear();
        }
    }
}
