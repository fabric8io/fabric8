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
import io.fabric8.cdi.Types;
import io.fabric8.cdi.bean.ConfigurationBean;
import io.fabric8.cdi.bean.ServiceBean;
import io.fabric8.cdi.bean.ServiceUrlBean;
import io.fabric8.cdi.bean.ServiceUrlCollectionBean;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.fabric8.cdi.Utils.or;

public class FactoryMethodProducer<T, X> implements Producer<T> {

    private static final String INVOCATION_ERROR_FORMAT = "Failed to invoke @Factory annotated method: %s on bean: %s with arguments: %s";
    private static final String PARAMETER_ERROR_FORMAT = "Failed to process @Factory annotated method: %s on bean: %s. Error processing parameter at position: %d. Check method signature for superfluous arguments or missing annotations.";

    private static final String SERVICE_LOOKUP_ERROR_FORMAT = "Failed to process @Factory annotated method: %s on bean: %s. Failed to lookup service %s. ";
    private static final String BEAN_LOOKUP_ERROR_FORMAT = "Failed to process @Factory annotated method: %s on bean: %s. Failed to lookup bean of type: %s for service: %s. ";
    private static final String CONF_LOOKUP_ERROR_FORMAT = "Failed to process @Factory annotated method: %s on bean: %s. Failed to lookup configuration for service: %s. ";

    private final Bean<T> bean;
    private final AnnotatedMethod<X> factoryMethod;

    // The fields below refer to the injection point properties
    private final String pointName;
    private final String pointProtocol;
    private final String pointPort;
    private final String pointPath;

    // end of injection point properties

    public FactoryMethodProducer(Bean<T> bean, AnnotatedMethod<X> factoryMethod, String pointName, String pointProtocol, String pointPort, String pointPath) {
        this.bean = bean;
        this.factoryMethod = factoryMethod;
        this.pointName = pointName;
        this.pointProtocol = pointProtocol;
        this.pointPort = pointPort;
        this.pointPath = pointPath;
    }

    @Override
    public T produce(CreationalContext<T> ctx) {
        List<Object> arguments = new ArrayList<>();

        for (AnnotatedParameter<X> parameter : factoryMethod.getParameters()) {
                Type type = parameter.getBaseType();
                ServiceName parameterServiceName = parameter.getAnnotation(ServiceName.class);
                Protocol parameterProtocol = parameter.getAnnotation(Protocol.class);
                PortName parameterPortName = parameter.getAnnotation(PortName.class);
                Path parameterPath = parameter.getAnnotation(Path.class);
                Endpoint paramEndpoint = parameter.getAnnotation(Endpoint.class);
                External paramExternal = parameter.getAnnotation(External.class);
                Configuration configuration = parameter.getAnnotation(Configuration.class);

                //A point without @ServiceName is invalid.
                // Even if method defines @ServiceName, the annotation on the injection point takes precedence
                String serviceName = pointName;
                String serviceProtocol = or(pointProtocol, parameterProtocol != null ? parameterProtocol.value() : null);
                String servicePort = or(pointPort, parameterPortName != null ? parameterPortName.value() : null);
                String servicePath = or(pointPath, parameterPath != null ? parameterPath.value() : null);
                Boolean serviceEndpoint = paramEndpoint != null ? paramEndpoint.value() : false;
                Boolean serviceExternal = paramExternal != null ? paramExternal.value() : false;

                //If the @ServiceName exists on the current String property
                if (parameterServiceName != null && String.class.equals(type)) {
                    try {
                        String serviceUrl = getServiceUrl(serviceName, serviceProtocol, servicePort, servicePath, serviceEndpoint, serviceExternal, ctx);
                        arguments.add(serviceUrl);
                    } catch (Throwable t) {
                        throw new RuntimeException(String.format(SERVICE_LOOKUP_ERROR_FORMAT,
                                factoryMethod.getJavaMember().getName(),
                                factoryMethod.getJavaMember().getDeclaringClass().getName(),
                                serviceName), t);
                    }
                }
                //If the @ServiceName exists on the current List property
                else if (parameterServiceName != null && List.class.equals(Types.asClass(type))) {
                    try {
                        List<String> endpointList = getEndpointList(serviceName, serviceProtocol, servicePort, servicePath, serviceExternal, ctx);
                        arguments.add(endpointList);
                    } catch (Throwable t) {
                        throw new RuntimeException(String.format(SERVICE_LOOKUP_ERROR_FORMAT,
                                factoryMethod.getJavaMember().getName(),
                                factoryMethod.getJavaMember().getDeclaringClass().getName(),
                                serviceName), t);
                    }
                }
                //If the @ServiceName exists on the current List property
                else if (parameterServiceName != null && Set.class.equals(Types.asClass(type))) {
                    try {
                        List<String> endpointList = getEndpointList(serviceName, serviceProtocol, servicePort, servicePath, serviceExternal, ctx);
                        arguments.add(new HashSet<>(endpointList));
                    } catch (Throwable t) {
                        throw new RuntimeException(String.format(SERVICE_LOOKUP_ERROR_FORMAT,
                                factoryMethod.getJavaMember().getName(),
                                factoryMethod.getJavaMember().getDeclaringClass().getName(),
                                serviceName), t);
                    }
                }

                // If the @ServiceName exists on the current property which is a non-String
                else if (parameterServiceName != null && !String.class.equals(type)) {
                    try {
                        Object serviceBean = getServiceBean(serviceName, serviceProtocol, servicePort, servicePath, serviceEndpoint, serviceExternal,  type, ctx);
                        arguments.add(serviceBean);
                    } catch (Throwable t) {
                        throw new RuntimeException(String.format(BEAN_LOOKUP_ERROR_FORMAT,
                                factoryMethod.getJavaMember().getName(),
                                factoryMethod.getJavaMember().getDeclaringClass().getName(),
                                type,
                                serviceName), t);
                    }
                }
                //If the current parameter is annotated with @Configuration
                else if (configuration != null) {
                    try {
                        Object config = getConfiguration(serviceName, (Class<Object>) type, ctx);
                        arguments.add(config);
                    } catch (Throwable t) {
                        throw new RuntimeException(String.format(CONF_LOOKUP_ERROR_FORMAT,
                                factoryMethod.getJavaMember().getName(),
                                factoryMethod.getJavaMember().getDeclaringClass().getName(),
                                serviceName), t);
                    }
                } else {
                    try {
                        Object other = BeanProvider.getContextualReference(Types.asClass(type), true);
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
                    arguments), t);
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
            return BeanProvider.getContextualReference(String.class, Qualifiers.create(serviceId, serviceProtocol, servicePort, servicePath, serviceEndpoint, serviceExternal));
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
     * Get Endpoint URLs as List from the context or create a producer.
     * @param serviceId
     * @param serviceProtocol
     * @param context
     * @return
     */
    private static List<String> getEndpointList(String serviceId, String serviceProtocol, String servicePort, String servicePath, Boolean serviceExternal, CreationalContext context) {
        final Boolean serviceEndpoint = true;
        try {
            return BeanProvider.getContextualReference(List.class, Qualifiers.create(serviceId, serviceProtocol, servicePort, servicePath, serviceEndpoint, serviceExternal));
        } catch (IllegalStateException e) {
            //Contextual Refernece not found, let's fallback to Configuration Producer.
            Producer<String> producer = ServiceUrlBean.anyBean(serviceId, serviceProtocol, servicePort, servicePath, serviceEndpoint, serviceExternal).getProducer();
            if (producer != null) {
                return ServiceUrlCollectionBean.anyBean(serviceId, serviceProtocol, servicePort, servicePath, serviceEndpoint, serviceExternal, Types.LIST_OF_STRINGS).getProducer().produce(context);
            } else {
                throw new IllegalStateException("Could not find producer for endpoints of service:" + serviceId + " protocol:" + serviceProtocol);
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
    private static Object getServiceBean(String serviceId, String serviceProtocol, String servicePort, String servicePath, Boolean serviceExternal, Boolean serviceEndpoint, Type serviceType, CreationalContext context) {
        try {
            return  BeanProvider.getContextualReference(Types.asClass(serviceType), Qualifiers.create(serviceId, serviceProtocol, servicePort, servicePath, serviceEndpoint, serviceExternal));
        } catch (IllegalStateException e) {

            Producer producer = ServiceBean.anyBean(serviceId, serviceProtocol, servicePort, servicePath, serviceEndpoint, serviceExternal, serviceType).getProducer();
            if (producer != null) {
                return  producer.produce(context);
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
     * @return
     */
    private static Object getConfiguration(String serviceId, Type type, CreationalContext context) {
        try {
            return BeanProvider.getContextualReference(Types.asClass(type), new ConfigurationQualifier(serviceId));
        } catch (IllegalStateException e) {
            //Contextual Refernece not found, let's fallback to Configuration Producer.
            return ConfigurationBean.getBean(serviceId, type).getProducer().produce(context);
        }
    }
}
