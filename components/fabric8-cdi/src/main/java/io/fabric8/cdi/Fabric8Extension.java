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

package io.fabric8.cdi;

import io.fabric8.annotations.Alias;
import io.fabric8.annotations.Configuration;
import io.fabric8.annotations.Factory;
import io.fabric8.annotations.Protocol;
import io.fabric8.annotations.ServiceName;
import io.fabric8.cdi.bean.ConfigurationBean;
import io.fabric8.cdi.bean.KubernetesClientBean;
import io.fabric8.cdi.bean.KubernetesFactoryBean;
import io.fabric8.cdi.bean.ServiceBean;
import io.fabric8.cdi.bean.ServiceUrlBean;
import io.fabric8.cdi.producers.FactoryMethodProducer;
import io.fabric8.cdi.qualifiers.ProtocolQualifier;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessManagedBean;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Fabric8Extension implements Extension {
    
    private static final Set<FactoryMethodContext> factories = new LinkedHashSet<>();

    public void afterDiscovery(final @Observes AfterBeanDiscovery event) {
        event.addBean(new KubernetesFactoryBean());
        event.addBean(new KubernetesClientBean());

        //We need to process factories in reverse order so that we make feasible forwarding for service id etc.
        List<FactoryMethodContext> reverseFactories = new ArrayList<>(FactoryMethodContext.sort(factories));
        Collections.reverse(reverseFactories);
        
        for (final FactoryMethodContext factoryMethodContext : reverseFactories) {
            ServiceBean.doWith(factoryMethodContext.getReturnType(), new ServiceBean.Callback() {
                @Override
                public ServiceBean apply(ServiceBean bean) {
                    String serviceId = bean.getServiceId();
                    String serviceProtocol = bean.getServiceProtocol();
                    
                    //Ensure that there is a factory String -> sourceType before adding producer.
                    if (!String.class.equals(factoryMethodContext.getSourceType())) {
                        ServiceBean.getBean(serviceId, serviceProtocol, null, (Class<Object>) factoryMethodContext.getSourceType());
                    }
                    return bean.withProducer(new FactoryMethodProducer(factoryMethodContext.getBean(), factoryMethodContext.getFactoryMethod(), serviceId, serviceProtocol));
                }
            }); 
        }
        
        for (ServiceUrlBean bean : ServiceUrlBean.getBeans()) {
            event.addBean(bean);
        }
        for (ServiceBean bean : ServiceBean.getBeans()) {
            if (bean.getProducer() != null) {
                event.addBean(bean);
            }
        }
        for (ConfigurationBean b : ConfigurationBean.getBeans()) {
            event.addBean(b);
        }
    }


    public <T, X> void onInjectionPoint(@Observes ProcessInjectionPoint<T, X> event) {
        final InjectionPoint injectionPoint = event.getInjectionPoint();
        if (isServiceInjectionPoint(injectionPoint)) {
            Annotated annotated = injectionPoint.getAnnotated();
            Alias alias = annotated.getAnnotation(Alias.class);
            ServiceName serviceName = annotated.getAnnotation(ServiceName.class);
            Protocol protocol = annotated.getAnnotation(Protocol.class);
            
            String serviceId = serviceName.value();
            String serviceProtocol = protocol != null ? protocol.value() : "tcp";
            String serviceAlias = alias != null ?  alias.value() : null;
            
            Type type = annotated.getBaseType();
            if (type.equals(String.class)) {
                ServiceUrlBean.getBean(serviceId, serviceProtocol, serviceAlias);
            } else {
                ServiceBean.getBean(serviceId, serviceProtocol, serviceAlias, (Class) type);
            }

            if (protocol == null) {
                //if protocol is not specified decorate injection point with "default" protocol.
                event.setInjectionPoint(new DelegatingInjectionPoint(injectionPoint) {
                    @Override
                    public Set<Annotation> getQualifiers() {
                        Set<Annotation> qualifiers = new LinkedHashSet<>(super.getQualifiers());
                        qualifiers.add(new ProtocolQualifier("tcp"));
                        return Collections.unmodifiableSet(qualifiers);
                    }
                });
            }
        } else if (isConfigurationInjectionPoint(injectionPoint)) {
            Annotated annotated = injectionPoint.getAnnotated();
            Configuration configuration = annotated.getAnnotation(Configuration.class);
            Type type = injectionPoint.getType();
            String configurationId = configuration.value();
            ConfigurationBean.getBean(configurationId, (Class) type);
        }
    }

    public <X> void onManagedBean(final @Observes ProcessManagedBean<X> event) {
        for (final AnnotatedMethod<?> method : event.getAnnotatedBeanClass().getMethods()) {
            final Factory factory = method.getAnnotation(Factory.class);
            if (factory != null) {
                final Type sourceType = getSourceType(method);
                final Type returnType = method.getJavaMember().getReturnType();
                factories.add(new FactoryMethodContext(event.getBean(), (Class) sourceType, (Class) returnType, method));
            }
        }
    }

    private static <T> Type getSourceType(AnnotatedMethod<T> method) {
        for (AnnotatedParameter<T> parameter : method.getParameters()) {
            if (parameter.isAnnotationPresent(ServiceName.class)) {
                return parameter.getBaseType();
            }
        }
        return String.class;
    }

    /**
     * Checks if the InjectionPoint is annotated with the @Configuration qualifier.
     *
     * @param injectionPoint The injection point.
     * @return
     */
    private static boolean isConfigurationInjectionPoint(InjectionPoint injectionPoint) {
        Set<Annotation> qualifiers = injectionPoint.getQualifiers();
        for (Annotation annotation : qualifiers) {
            if (annotation.annotationType().isAssignableFrom(Configuration.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the InjectionPoint is annotated with the @Service qualifier.
     *
     * @param injectionPoint The injection point.
     * @return
     */
    public static boolean isServiceInjectionPoint(InjectionPoint injectionPoint) {
        Set<Annotation> qualifiers = injectionPoint.getQualifiers();
        for (Annotation annotation : qualifiers) {
            if (annotation.annotationType().isAssignableFrom(ServiceName.class)) {
                return true;
            }
        }
        return false;
    }
}
