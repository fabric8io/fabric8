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
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessManagedBean;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class Fabric8Extension implements Extension {

    public void afterDiscovery(final @Observes AfterBeanDiscovery event) {
        event.addBean(new KubernetesFactoryBean());
        event.addBean(new KubernetesClientBean());

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
            ServiceName serviceName = annotated.getAnnotation(ServiceName.class);
            Protocol protocol = annotated.getAnnotation(Protocol.class);
            String serviceId = serviceName.value();
            String serviceProtocol = protocol != null ? protocol.value() : "tcp";
            Type type = annotated.getBaseType();
            if (type.equals(String.class)) {
                ServiceUrlBean.getBean(serviceId, serviceProtocol);
            } else {
                ServiceBean.getBean(serviceId, serviceProtocol, (Class) type);
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
                Type returnType = method.getJavaMember().getReturnType();
                ServiceBean.doWith(returnType, new ServiceBean.Callback() {
                    @Override
                    public ServiceBean apply(ServiceBean bean) {
                        String serviceId = bean.getServiceId();
                        String serviceProtocol = bean.getServiceProtocol();
                        return bean.withProducer(new FactoryMethodProducer(event.getBean(), method, serviceId, serviceProtocol));
                    }
                });
            }
        }
    }

    /**
     * Checks if the InjectionPoint is annotated with the @Configuration qualifier.
     *
     * @param injectionPoint The injection point.
     * @return
     */
    public boolean isConfigurationInjectionPoint(InjectionPoint injectionPoint) {
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
    public boolean isServiceInjectionPoint(InjectionPoint injectionPoint) {
        Set<Annotation> qualifiers = injectionPoint.getQualifiers();
        for (Annotation annotation : qualifiers) {
            if (annotation.annotationType().isAssignableFrom(ServiceName.class)) {
                return true;
            }
        }
        return false;
    }
}
