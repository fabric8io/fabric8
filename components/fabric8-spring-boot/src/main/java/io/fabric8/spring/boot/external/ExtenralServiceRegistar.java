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

package io.fabric8.spring.boot.external;

import io.fabric8.annotations.ServiceName;
import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.api.KubernetesFactory;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.spring.boot.KubernetesProperties;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

public class ExtenralServiceRegistar implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        KubernetesProperties kubernetesProperties = new KubernetesProperties();
        KubernetesFactory kubernetesFactory = new KubernetesFactory(kubernetesProperties.getKubernetesMasterUrl());
        Kubernetes kubernetes = kubernetesFactory.createKubernetes();
        for (final Service service : kubernetes.getServices("default").getItems()) {
            RootBeanDefinition beanDefinition = new RootBeanDefinition(Service.class);
            beanDefinition.addQualifier(new AutowireCandidateQualifier(ServiceName.class, service.getId()));
            beanDefinition.getPropertyValues().addPropertyValue("id", service.getId());
            beanDefinition.getPropertyValues().addPropertyValue("port", service.getPort());
            beanDefinition.getPropertyValues().addPropertyValue("portalIP", service.getPortalIP());
            beanDefinition.getPropertyValues().addPropertyValue("protocol", service.getProtocol());
            beanDefinition.getPropertyValues().addPropertyValue("proxyPort", service.getProxyPort());
            beanDefinition.getPropertyValues().addPropertyValue("containerPort", service.getContainerPort());
            beanDefinition.getPropertyValues().addPropertyValue("kind", service.getKind());
            registry.registerBeanDefinition(service.getId()+"-service-bean", beanDefinition);
        }
    }
}
