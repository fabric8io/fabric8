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

package io.fabric8.cdi.producers;


import io.fabric8.cdi.annotations.Configuration;
import io.fabric8.cdi.annotations.Service;
import io.fabric8.cdi.bean.ConfigurationBean;
import io.fabric8.cdi.bean.ServiceUrlBean;
import io.fabric8.cdi.qualifiers.ConfigurationQualifier;
import io.fabric8.cdi.qualifiers.ServiceQualifier;
import org.apache.deltaspike.core.api.provider.BeanProvider;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Producer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class FactoryMethodProducer<T, X> implements Producer<T> {

    private final Bean<T> bean;
    private final AnnotatedMethod<X> factoryMethod;
    private final String serviceId;

    public FactoryMethodProducer(Bean<T> bean, AnnotatedMethod<X> factoryMethod, String serviceId) {
        this.bean = bean;
        this.factoryMethod = factoryMethod;
        this.serviceId = serviceId;
    }

    public FactoryMethodProducer<T, X> withServiceId(String serviceId) {
        return new FactoryMethodProducer<>(bean, factoryMethod, serviceId);
    }

    @Override
    public T produce(CreationalContext<T> ctx) {
        List<Object> arguments = new ArrayList<>();

        for (AnnotatedParameter<X> parameter : factoryMethod.getParameters()) {
            Type type = parameter.getBaseType();
            Service service = parameter.getAnnotation(Service.class);
            Configuration configuration = parameter.getAnnotation(Configuration.class);
            if (service != null) {
                String serviceUrl = getServiceUrl(serviceId, ctx);
                arguments.add(serviceUrl);
            } else if (configuration != null) {
                Object config = getConfiguration(serviceId, (Class<Object>) type, ctx);
                arguments.add(config);
            } else {
                Object other = BeanProvider.getContextualReferences((Class) type, false);
                arguments.add(other);
            }
        }
        try {
            return (T) factoryMethod.getJavaMember().invoke(bean.create(ctx), arguments.toArray());
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public void dispose(T instance) {

    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    /**
     * Get Service URL from the context or create a producer. 
     * @param serviceId
     * @param context
     * @return
     */
    private String getServiceUrl(String serviceId, CreationalContext context) {
        try {
            return (String) BeanProvider.getContextualReference((Class) String.class, new ServiceQualifier(serviceId));
        } catch (IllegalStateException e) {
            //Contextual Refernece not found, let's fallback to Configuration Producer.
            return ServiceUrlBean.getBean(serviceId).getProducer().produce(context);
        }
    }

    /**
     * Get Configuration from context or create a producer.
     * @param serviceId
     * @param type
     * @param context
     * @param <C>
     * @return
     */
    private <C> C getConfiguration(String serviceId, Class<C> type, CreationalContext context) {
        try {
            return (C) BeanProvider.getContextualReference((Class) type, new ConfigurationQualifier(serviceId));
        } catch (IllegalStateException e) {
            //Contextual Refernece not found, let's fallback to Configuration Producer.
            return (C) ConfigurationBean.getBean(serviceId, type).getProducer().produce(context);
        }
    }
}
