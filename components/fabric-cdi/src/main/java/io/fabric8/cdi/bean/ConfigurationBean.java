/*
 * Copyright 2005-2014 Red Hat, Inc.                                    
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

package io.fabric8.cdi.bean;

import io.fabric8.cdi.annotations.Configuration;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.apache.deltaspike.core.api.config.ConfigResolver;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.util.AnnotationLiteral;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class ConfigurationBean<T> extends BaseBean<T> {

    private final String configurationGroup;

    public ConfigurationBean(Type beanType, String configurationGroup) {
        super(configurationGroup+"."+beanType, beanType, new ConfigurationQualifier(configurationGroup));
        this.configurationGroup = configurationGroup;
    }


    @Override
    public T create(CreationalContext<T> creationalContext) {
        try {
            T bean = (T) getBeanClass().newInstance();
            for (Field f : getBeanClass().getDeclaredFields()) {
                ConfigProperty configProperty = f.getAnnotation(ConfigProperty.class);
                String name = configProperty.name();
                String defaultValue = configProperty.defaultValue();
                String value = ConfigResolver.getPropertyValue(configurationGroup.toUpperCase() + "_" + name, defaultValue);
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
            return bean;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext) {

    }

    private static class ConfigurationQualifier extends AnnotationLiteral<Configuration> implements Configuration {
        private final String value;

        private ConfigurationQualifier(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }
}
