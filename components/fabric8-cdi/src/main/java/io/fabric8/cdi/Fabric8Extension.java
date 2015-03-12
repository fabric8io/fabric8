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

import io.fabric8.cdi.annotations.Configuration;
import io.fabric8.cdi.bean.ConfigurationBean;
import io.fabric8.cdi.bean.KubernetesClientBean;
import io.fabric8.cdi.bean.KubernetesFactoryBean;
import io.fabric8.cdi.bean.ServiceBean;
import io.fabric8.kubernetes.api.model.Service;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.fabric8.cdi.KubernetesHolder.KUBERNETES;

public class Fabric8Extension implements Extension {

    private final Map<String, io.fabric8.kubernetes.api.model.Service> servicesById;
    private final Set<ConfigurationBean> configurationBeans = new HashSet<>();
    
    

    public Fabric8Extension() {
        Map<String, io.fabric8.kubernetes.api.model.Service> servicesMap = new HashMap<>();
        for (io.fabric8.kubernetes.api.model.Service service : KUBERNETES.getServices().getItems()) {
            servicesMap.put(service.getId(), service);
        }
        servicesById = Collections.unmodifiableMap(servicesMap);
    }

    public void init(final @Observes AfterBeanDiscovery discovery) {
        discovery.addBean(new KubernetesFactoryBean());
        discovery.addBean(new KubernetesClientBean());
        for (Map.Entry<String, Service> entry : servicesById.entrySet()) {
            discovery.addBean(new ServiceBean(entry.getValue()));
        }
        
        for (ConfigurationBean bean : configurationBeans) {
            discovery.addBean(bean);
        }
    }


    public <X,Y> void onInjectionTarget(@Observes ProcessInjectionPoint<X,Y> event, BeanManager beanManager) {
        final InjectionPoint injectionPoint = event.getInjectionPoint();
        if (isConfigurationInjectionPoint(injectionPoint)) {
            Annotated annotated = injectionPoint.getAnnotated();
            Configuration configuration = annotated.getAnnotation(Configuration.class);
            Type type = injectionPoint.getType();
            String configurationGroup = configuration.value();
            configurationBeans.add(new ConfigurationBean(type, configurationGroup));
        }
    }

    /**
     * Checks if the InjectionPoint is annotated with the @Configuration qualifier.
     * @param injectionPoint    The injection point.
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
}
