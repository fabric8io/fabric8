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
package io.fabric8.cdi.producers;

import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.apache.deltaspike.core.api.config.ConfigResolver;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Producer;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;

public class ConfigurationProducer<T> implements Producer<T> {

    private final String configurationId;
    private final Class<T> type;

    public ConfigurationProducer(String configurationId, Class<T> type) {
        this.type = type;
        this.configurationId = configurationId;
    }

    @Override
    public T produce(CreationalContext<T> ctx) {
        if (configurationId == null) {
            throw new IllegalArgumentException("No service id has been specified.");
        }
        try {
            T bean = type.newInstance();
            for (Field f : type.getDeclaredFields()) {
                ConfigProperty configProperty = f.getAnnotation(ConfigProperty.class);
                if (configProperty != null) {
                    String name = configProperty.name();
                    String defaultValue = ConfigProperty.NULL.equals(configProperty.defaultValue()) ? null : configProperty.defaultValue();

                    String value = ConfigResolver.getPropertyValue((configurationId + "_" + name)
                            .replaceAll("-", "_")
                            .toUpperCase(), defaultValue);
                    if (f.getType().isAssignableFrom(String.class)) {
                        f.setAccessible(true);
                        f.set(bean, value);
                    } else if (f.getType().isAssignableFrom(Boolean.class)) {
                        f.setAccessible(true);
                        f.set(bean, Boolean.parseBoolean(value));
                    } else if (f.getType().isAssignableFrom(Short.class)) {
                        f.setAccessible(true);
                        f.set(bean, Short.parseShort(value));
                    } else if (f.getType().isAssignableFrom(Integer.class)) {
                        f.setAccessible(true);
                        f.set(bean, Integer.parseInt(value));
                    } else if (f.getType().isAssignableFrom(Long.class)) {
                        f.setAccessible(true);
                        f.set(bean, Long.parseLong(value));
                    } else if (f.getType().isAssignableFrom(Double.class)) {
                        f.setAccessible(true);
                        f.set(bean, Double.parseDouble(value));
                    } else if (f.getType().isAssignableFrom(Float.class)) {
                        f.setAccessible(true);
                        f.set(bean, Float.parseFloat(value));
                    }
                }
            }
            return bean;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void dispose(T instance) {
        //do nothing
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    private static String toEnv(String str) {
        return str.toUpperCase().replaceAll("-", "_");
    }
}
