/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.spring.boot.converters;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

public class FactoryConverter<S, T> implements GenericConverter, BeanFactoryAware {

    private String name;
    private Class<S> sourceType;
    private Class<S> targetType;
    private Class<?> type;

    private BeanFactory beanFactory;


    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(sourceType, targetType));
    }

    @Override
    public Object convert(Object o, TypeDescriptor typeDescriptor, TypeDescriptor typeDescriptor1) {
        try {
            final Object factory = beanFactory.getBean(type);
            final Method method = factory.getClass().getDeclaredMethod(name, sourceType);
            return (T) method.invoke(factory, o);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to convert.", t);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<S> getSourceType() {
        return sourceType;
    }

    public void setSourceType(Class<S> sourceType) {
        this.sourceType = sourceType;
    }

    public Class<S> getTargetType() {
        return targetType;
    }

    public void setTargetType(Class<S> targetType) {
        this.targetType = targetType;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
