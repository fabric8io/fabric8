/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.spring.boot;

import io.fabric8.annotations.Alias;
import io.fabric8.annotations.External;
import io.fabric8.annotations.Factory;
import io.fabric8.annotations.PortName;
import io.fabric8.annotations.Protocol;
import io.fabric8.annotations.ServiceName;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.spring.boot.converters.FactoryConverter;
import io.fabric8.utils.Strings;
import javassist.ClassPool;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLClassLoader;

import static io.fabric8.spring.boot.Constants.ALIAS;
import static io.fabric8.spring.boot.Constants.DEFAULT_PROTOCOL;
import static io.fabric8.spring.boot.Constants.EXTERNAL;
import static io.fabric8.spring.boot.Constants.PORT;
import static io.fabric8.spring.boot.Constants.PROTOCOL;

public abstract class AbstractServiceRegistar implements ImportBeanDefinitionRegistrar {

    private final ClassPool classPool = ClassPool.getDefault();

    private static final Reflections REFLECTIONS = new Reflections(new ConfigurationBuilder()
            .setUrls(((URLClassLoader) AbstractServiceRegistar.class.getClassLoader()).getURLs())
            .setScanners(
                    new FieldAnnotationsScanner(),
                    new MethodAnnotationsScanner()
            )
    );

    public abstract Service getService(String name);


    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {

        for (Method method : REFLECTIONS.getMethodsAnnotatedWith(Factory.class)) {
            String methodName = method.getName();
            Class sourceType = getSourceType(method);
            Class targetType = method.getReturnType();
            Class beanType = method.getDeclaringClass();
            BeanDefinitionHolder holder = createConverterBean(beanType, methodName, sourceType, targetType);
            BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
        }

        for (Field field : REFLECTIONS.getFieldsAnnotatedWith(ServiceName.class)) {
            Class targetClass = field.getType();
            Alias alias = field.getAnnotation(Alias.class);
            ServiceName name = field.getAnnotation(ServiceName.class);
            PortName port = field.getAnnotation(PortName.class);
            Protocol protocol = field.getAnnotation(Protocol.class);
            External external = field.getAnnotation(External.class);

            String serviceName = name != null ? name.value() : null;

            //We copy the service since we are going to add properties to it.
            Service serviceInstance = new ServiceBuilder(getService(serviceName)).build();
            String servicePort = port != null ? port.value() : null;
            String serviceProtocol = protocol != null ? protocol.value() : DEFAULT_PROTOCOL;
            Boolean serviceExternal = external != null && external.value();
            String serviceAlias = alias != null ? alias.value() : createAlias(serviceName, targetClass, serviceProtocol, servicePort, serviceExternal);

            //Add annotation info as additional properties
            serviceInstance.getAdditionalProperties().put(ALIAS, serviceAlias);
            serviceInstance.getAdditionalProperties().put(PROTOCOL, serviceProtocol);
            serviceInstance.getAdditionalProperties().put(EXTERNAL, serviceExternal);

            //We don't want to add a fallback value to the attributes.
            if (port != null) {
                serviceInstance.getAdditionalProperties().put(PORT, servicePort);
            }

            BeanDefinitionHolder holder = createServiceDefinition(serviceInstance, serviceAlias, serviceProtocol, servicePort, targetClass);
            BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
        }
    }

    private static Class getSourceType(Method method) {
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < annotations.length; i++) {
            for (int j = 0; j < annotations[i].length; j++) {
                if (ServiceName.class.equals(annotations[i][j].annotationType())) {
                    return method.getParameterTypes()[i];
                }
            }
        }
        throw new IllegalStateException("No source type found for @Factory:" + method.getName());
    }

    private <S, T> BeanDefinitionHolder createConverterBean(Class type, String methodName, Class<S> sourceType, Class<T> targetType) {

        BeanDefinitionBuilder builder = BeanDefinitionBuilder
                .genericBeanDefinition(FactoryConverter.class);

        String beanName = type.getName() + "." + methodName;
        builder.addPropertyValue("name", methodName);
        builder.addPropertyValue("type", type.getCanonicalName());
        builder.addPropertyValue("sourceType", sourceType.getCanonicalName());
        builder.addPropertyValue("targetType", targetType.getCanonicalName());

        builder.setAutowireMode(Autowire.BY_TYPE.value());
        return new BeanDefinitionHolder(builder.getBeanDefinition(), beanName);
    }

    private BeanDefinitionHolder createServiceDefinition(Service service, String alias, String protocol, String port, Class type) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder
                .genericBeanDefinition(KubernetesServiceFactoryBean.class);

        builder.addPropertyValue("name", alias);
        builder.addPropertyValue("service", service);
        builder.addPropertyValue("port", port);
        builder.addPropertyValue("type", type.getCanonicalName());
        builder.setAutowireMode(Autowire.BY_TYPE.value());
        //Add protocol qualifier
        builder.getBeanDefinition().addQualifier(new AutowireCandidateQualifier(ServiceName.class, KubernetesHelper.getName(service)));
        builder.getBeanDefinition().addQualifier(new AutowireCandidateQualifier(Protocol.class, protocol));
        builder.getBeanDefinition().addQualifier(new AutowireCandidateQualifier(PortName.class, port != null ? port : ""));
        return new BeanDefinitionHolder(builder.getBeanDefinition(), alias);
    }


    private static String createAlias(String name, Class type, String protocol, String port, Boolean external) {
        StringBuilder sb = new StringBuilder();
        sb.append(type.getName()).append("-").append(name);

        if (Strings.isNotBlank(protocol)) {
            sb.append("-").append(protocol);
        }

        if (Strings.isNotBlank(port)) {
            sb.append("-").append(port);
        }
        if (external) {
            sb.append("-external");
        }
        return sb.toString();
    }
}
