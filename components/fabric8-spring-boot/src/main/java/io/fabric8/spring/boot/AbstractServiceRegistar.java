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

package io.fabric8.spring.boot;

import io.fabric8.annotations.Alias;
import io.fabric8.annotations.External;
import io.fabric8.annotations.Protocol;
import io.fabric8.annotations.ServiceName;
import io.fabric8.kubernetes.api.model.Service;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.reflect.Field;
import java.net.URLClassLoader;

import static io.fabric8.spring.boot.Constants.ALIAS;
import static io.fabric8.spring.boot.Constants.DEFAULT_PROTOCOL;
import static io.fabric8.spring.boot.Constants.EXTERNAL;
import static io.fabric8.spring.boot.Constants.PROTOCOL;

public abstract class AbstractServiceRegistar implements ImportBeanDefinitionRegistrar {

    private static final Reflections REFLECTIONS = new Reflections(new ConfigurationBuilder()
            .setUrls(((URLClassLoader) AbstractServiceRegistar.class.getClassLoader()).getURLs())
            .setScanners(
                    new FieldAnnotationsScanner()));

    public abstract Service getService(String name);


    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        for (Field field : REFLECTIONS.getFieldsAnnotatedWith(ServiceName.class)) {
            Alias alias = field.getAnnotation(Alias.class);
            ServiceName name = field.getAnnotation(ServiceName.class);
            Protocol protocol = field.getAnnotation(Protocol.class);
            External external = field.getAnnotation(External.class);

            String serviceProtocol = protocol != null ? protocol.value() : DEFAULT_PROTOCOL;
            String serviceAlias = alias != null ? alias.value() : name.value();
            Boolean serviceExternal = external != null && external.value();

            Service serviceInstance = getService(name.value());

            //Add annotation info as additional properties
            serviceInstance.getAdditionalProperties().put(ALIAS, serviceAlias);
            serviceInstance.getAdditionalProperties().put(PROTOCOL, serviceProtocol);
            serviceInstance.getAdditionalProperties().put(EXTERNAL, serviceExternal);

            Class targetClass = field.getType();
            BeanDefinitionHolder holder = createBeanDefinition(serviceInstance, serviceAlias, serviceProtocol, targetClass);
            BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
        }
    }

    private BeanDefinitionHolder createBeanDefinition(Service service, String alias, String protocol, Class type) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder
                .genericBeanDefinition(KubernetesServiceFactoryBean.class);

        builder.addPropertyValue("name", alias);
        builder.addPropertyValue("service", service);
        builder.addPropertyValue("type", type.getCanonicalName());
        //Add protocol qualifier
        builder.getBeanDefinition().addQualifier(new AutowireCandidateQualifier(Protocol.class, protocol));
        return new BeanDefinitionHolder(builder.getBeanDefinition(), alias);
    }
}
