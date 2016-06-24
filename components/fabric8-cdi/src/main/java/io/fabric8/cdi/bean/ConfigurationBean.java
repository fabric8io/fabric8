/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.cdi.bean;

import io.fabric8.cdi.Types;
import io.fabric8.cdi.producers.ConfigurationProducer;
import io.fabric8.cdi.qualifiers.ConfigurationQualifier;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationBean<T> extends ProducerBean<T> {

    private static final String SUFFIX = "-config-bean";
    private static final Map<Key, ConfigurationBean> BEANS = new HashMap<>();

    private final String configurationId;

    public static ConfigurationBean getBean(String configurationId, Type type) {
        Key key = new Key(configurationId, type);
        if (BEANS.containsKey(key)) {
            return BEANS.get(key);
        }
        ConfigurationBean bean = new ConfigurationBean(configurationId, type);
        BEANS.put(key, bean);
        return bean;
    }

    public static Collection<ConfigurationBean> getBeans() {
        return BEANS.values();
    }

    private ConfigurationBean(String configurationId, Type type) {
        super(configurationId + SUFFIX,
                type,
                new ConfigurationProducer(configurationId, Types.asClass(type)),
                new ConfigurationQualifier(configurationId));
        this.configurationId = configurationId;
    }

    public String getConfigurationId() {
        return configurationId;
    }

    private static class Key {
        private final String configurationId;
        private final Type type;

        private Key(String configurationId, Type type) {
            this.configurationId = configurationId;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (configurationId != null ? !configurationId.equals(key.configurationId) : key.configurationId != null)
                return false;
            if (type != null ? !type.equals(key.type) : key.type != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = configurationId != null ? configurationId.hashCode() : 0;
            result = 31 * result + (type != null ? type.hashCode() : 0);
            return result;
        }
    }
}
