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
package io.fabric8.cdi.producers;


import io.fabric8.annotations.Configuration;
import io.fabric8.annotations.Endpoint;
import io.fabric8.annotations.External;
import io.fabric8.annotations.Path;
import io.fabric8.annotations.PortName;
import io.fabric8.annotations.Protocol;
import io.fabric8.annotations.ServiceName;
import io.fabric8.cdi.bean.ConfigurationBean;
import io.fabric8.cdi.bean.ServiceBean;
import io.fabric8.cdi.bean.ServiceUrlBean;
import io.fabric8.cdi.qualifiers.ConfigurationQualifier;
import io.fabric8.cdi.qualifiers.Qualifiers;
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

    private static final String INVOCATION_ERROR_FORMAT = "Failed to invoke @Factory annotated method: %s on bean: %s with arguments: %s";
    private static final String PARAMETER_ERROR_FORMAT = "Failed to process @Factory annotated method: %s on bean: %s. Error processing parameter at position: %d. Check method signature for superfluous arguments or missing annotations.";

    private static final String SERVICE_LOOKUP_ERROR_FORMAT = "Failed to process @Factory annotated method: %s on bean: %s. Failed to lookup service %s. ";
    private static final String BEAN_LOOKUP_ERROR_FORMAT = "Failed to process @Factory annotated method: %s on bean: %s. Failed to lookup bean of type: %s for service: %s. ";
    private static final String CONF_LOOKUP_ERROR_FORMAT = "Failed to process @Factory annotated method: %s on bean: %s. Failed to lookup configuration for service: %s. ";

    private final Bean<T> bean;
    private final AnnotatedMethod<X> factoryMethod;

    // The fields below refer to the injection point properties
    private final String serviceId;
    // end of injection point properties

    public FactoryMethodProducer(Bean<T> bean, AnnotatedMethod<X> factoryMethod, String serviceId) {
        this.bean = bean;
        this.factoryMethod = factoryMethod;
        this.serviceId = serviceId;
    }

    @Override
    public T produce(CreationalContext<T> ctx) {
        List<Object> arguments = new ArrayList<>();

        for (AnnotatedParameter<X> parameter : factoryMethod.getParameters()) {

                Type type = parameter.getBaseType();
                ServiceName parameterServiceName = parameter.getAnnotation(ServiceName.class);
                Protocol paramterProtocol = parameter.getAnnotation(Protocol.class);
                PortName parameterPortName = parameter.getAnnotation(PortName.class);
                Path parameterPath = parameter.getAnnotation(Path.class);
                Endpoint paramEndpoint = parameter.getAnnotation(Endpoint.class);
                External paramExternal = parameter.getAnnotation(External.class);
                Configuration configuration = parameter.getAnnotation(Configuration.class);

                String serviceProtocol = paramterProtocol != null ? paramterProtocol.value() : null;
                String servicePort = parameterPortName != null ? parameterPortName.value() : null;
                String servicePath = parameterPath != null ? parameterPath.value() : null;
                Boolean serviceEndpoint = paramEndpoint != null ? paramEndpoint.value() : false;
                Boolean serviceExternal = paramExternal != null ? paramExternal.value() : false;

                //If the @ServiceName exists on the current String property
                if (parameterServiceName != null && String.class.equals(type)) {
                    try {
                        String serviceUrl = getServiceUrl(serviceId, serviceProtocol, servicePort, servicePath, serviceEndpoint, serviceExternal, ctx);
                        arguments.add(serviceUrl);
                    } catch (Throwable t) {
                        throw new RuntimeException(String.format(SERVICE_LOOKUP_ERROR_FORMAT,
                                factoryMethod.getJavaMember().getName(),
                                factoryMethod.getJavaMember().getDeclaringClass().getName(),
                                serviceId), t);
                    }
                }
                // If the @ServiceName exists on the current property which is a non-String
                else if (parameterServiceName != null && !String.class.equals(type)) {
                    try {
                        Object serviceBean = getServiceBean(serviceId, serviceProtocol, servicePort, servicePath, serviceEndpoint, serviceExternal, (Class<Object>) type, ctx);
                        arguments.add(serviceBean);
                    } catch (Throwable t) {
                        throw new RuntimeException(String.format(BEAN_LOOKUP_ERROR_FORMAT,
                                factoryMethod.getJavaMember().getName(),
                                factoryMethod.getJavaMember().getDeclaringClass().getName(),
                                type,
                                serviceId), t);
                    }
                }
                //If the current parameter is annotated with @Configuration
                else if (configuration != null) {
                    try {
                        Object config = getConfiguration(serviceId, (Class<Object>) type, ctx);
                        arguments.add(config);
                    } catch (Throwable t) {
                        throw new RuntimeException(String.format(CONF_LOOKUP_ERROR_FORMAT,
                                factoryMethod.getJavaMember().getName(),
                                factoryMethod.getJavaMember().getDeclaringClass().getName(),
                                serviceId), t);
                    }
                } else {
                    try {
                        Object other = BeanProvider.getContextualReferences((Class) type, true);
                        arguments.add(other);
                    } catch (Throwable t) {
                        throw new RuntimeException(String.format(PARAMETER_ERROR_FORMAT,
                                factoryMethod.getJavaMember().getName(),
                                factoryMethod.getJavaMember().getDeclaringClass().getName(),
                                parameter.getPosition()), t);
                    }
                }
        }

        try {
            return (T) factoryMethod.getJavaMember().invoke(bean.create(ctx), arguments.toArray());
        } catch (Throwable t) {

            throw new RuntimeException(String.format(INVOCATION_ERROR_FORMAT,
                    factoryMethod.getJavaMember().getName(),
                    factoryMethod.getJavaMember().getDeclaringClass().getName(),
                    arguments));
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
     * @param serviceProtocol 
     * @param context
     * @return
     */
    private static String getServiceUrl(String serviceId, String serviceProtocol, String servicePort, String servicePath, Boolean serviceEndpoint, Boolean serviceExternal, CreationalContext context) {
        try {
            return (String) BeanProvider.getContextualReference((Class) String.class, Qualifiers.create(serviceId, serviceProtocol, servicePort, servicePath, serviceEndpoint, serviceExternal));
        } catch (IllegalStateException e) {
            //Contextual Refernece not found, let's fallback to Configuration Producer.
            Producer<String> producer = ServiceUrlBean.anyBean(serviceId, serviceProtocol, servicePort, servicePath, serviceEndpoint, serviceExternal).getProducer();
            if (producer != null) {
                return ServiceUrlBean.anyBean(serviceId, serviceProtocol, servicePort, servicePath, serviceEndpoint, serviceExternal).getProducer().produce(context);
            } else {
                throw new IllegalStateException("Could not find producer for service:" + serviceId + " protocol:" + serviceProtocol);
            }
        }
    }

    /**
     * Get Service Bean from the context or create a producer.
     * @param serviceId
     * @param serviceProtocol
     * @param context
     * @return
     */
    private static <S> S getServiceBean(String serviceId, String serviceProtocol, String servicePort, String servicePath, Boolean serviceExternal, Boolean serviceEndpoint, Class<S> serviceType, CreationalContext context) {
        try {
            return  BeanProvider.getContextualReference(serviceType, Qualifiers.create(serviceId, serviceProtocol, servicePort, servicePath, serviceEndpoint, serviceExternal));
        } catch (IllegalStateException e) {

            Producer<S> producer = ServiceBean.anyBean(serviceId, serviceProtocol, servicePort, servicePath, serviceEndpoint, serviceExternal, serviceType).getProducer();
            if (producer != null) {
                return (S) producer.produce(context);
            } else {
                throw new IllegalStateException("Could not find producer for service:" + serviceId + " type:" + serviceType + " protocol:" + serviceProtocol);
            }
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
    private static <C> C getConfiguration(String serviceId, Class<C> type, CreationalContext context) {
        try {
            return (C) BeanProvider.getContextualReference((Class) type, new ConfigurationQualifier(serviceId));
        } catch (IllegalStateException e) {
            //Contextual Refernece not found, let's fallback to Configuration Producer.
            return (C) ConfigurationBean.getBean(serviceId, type).getProducer().produce(context);
        }
    }
}
