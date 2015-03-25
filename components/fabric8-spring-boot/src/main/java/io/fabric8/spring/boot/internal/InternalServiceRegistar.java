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

package io.fabric8.spring.boot.internal;

import io.fabric8.kubernetes.api.model.Service;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InternalServiceRegistar implements ImportBeanDefinitionRegistrar {

    private static final String SERVICE = "service";
    private static final String SERVICE_HOST_REGEX = "(?<service>[A-Z_]+)_SERVICE_HOST";

    private static final Pattern SERVICE_HOST_PATTERN = Pattern.compile(SERVICE_HOST_REGEX);

    private static final String HOST_SUFFIX = "_SERVICE_HOST";
    private static final String PORT_SUFFIX = "_SERVICE_PORT";
    private static final String PROTO_SUFFIX = "_TCP_PROTO";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Map<String, String> env = System.getenv();

        for (Map.Entry<String, String> entry : env.entrySet()) {
            String key = entry.getKey();
            Matcher matcher = SERVICE_HOST_PATTERN.matcher(key);
            if (matcher.matches()) {
                String service = matcher.group(SERVICE);
                if (areEnvVariablesAvailable(service, env)) {
                    RootBeanDefinition beanDefinition = new RootBeanDefinition(Service.class);

                    String serviceHost = env.get(service + HOST_SUFFIX);
                    String port = env.get(service + PORT_SUFFIX);
                    String protocol = env.get(service + PORT_SUFFIX + "_" + port + PROTO_SUFFIX);

                    beanDefinition.addQualifier(new AutowireCandidateQualifier(io.fabric8.annotations.Service.class, service));
                    beanDefinition.getPropertyValues().addPropertyValue("id", service);
                    beanDefinition.getPropertyValues().addPropertyValue("port", port);
                    beanDefinition.getPropertyValues().addPropertyValue("portalIP", serviceHost);
                    beanDefinition.getPropertyValues().addPropertyValue("protocol", protocol);
                    registry.registerBeanDefinition(service + "-service-bean", beanDefinition);
                }
            }
        }
    }

    private boolean areEnvVariablesAvailable(String service, Map<String, String> env) {
        return env.containsKey(service + HOST_SUFFIX)
                && env.containsKey(service + PORT_SUFFIX)
                && env.containsKey(service + PORT_SUFFIX);
    }
}
