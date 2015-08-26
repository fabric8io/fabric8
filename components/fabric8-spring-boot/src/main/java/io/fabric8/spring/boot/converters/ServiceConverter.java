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
package io.fabric8.spring.boot.converters;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static io.fabric8.spring.boot.Constants.DEFAULT_PROTOCOL;
import static io.fabric8.spring.boot.Constants.EXTERNAL;
import static io.fabric8.spring.boot.Constants.PROTOCOL;

@Component
public class ServiceConverter implements GenericConverter {

    @Autowired
    private KubernetesClient kubernetesClient;

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return new LinkedHashSet<>(Arrays.asList(
                new ConvertiblePair(Service.class, String.class),
                new ConvertiblePair(Service.class, URL.class)
        ));
    }

    @Override
    public Object convert(Object o, TypeDescriptor sourceType, TypeDescriptor targetType) {
        Service source = (Service) o;
        String serviceName = KubernetesHelper.getName(source);
        String serviceNamespace = KubernetesHelper.getNamespace(source);
        String serviceProtocol = getProtocolOfService(source);
        Boolean serviceExternal = isServiceExternal(source);
        serviceNamespace = serviceNamespace != null ? serviceNamespace : KubernetesHelper.defaultNamespace();
        String str = KubernetesHelper.getServiceURL(kubernetesClient, serviceName, serviceNamespace, serviceProtocol, serviceExternal);
        try {
            if (String.class.equals(targetType.getObjectType())) {
                return str;
            } else if (URL.class.equals(targetType.getObjectType())) {
                return new URL(str);
            }
        } catch (Throwable t) {
            throw new RuntimeException("Failed to convert from: " + sourceType.getObjectType() + " to: " + targetType.getObjectType());
        }
        throw new IllegalStateException("Invalid target type: " + targetType.getObjectType());
    }


    private String getProtocolOfService(Service service) {
        String protocol = DEFAULT_PROTOCOL;
        if (service.getAdditionalProperties().containsKey(PROTOCOL)) {
            Object protocolProperty = service.getAdditionalProperties().get(PROTOCOL);
            if (protocolProperty instanceof String) {
                protocol = (String) protocolProperty;
            }
        }
        return protocol;
    }

    private Boolean isServiceExternal(Service service) {
        Boolean external = false;
        if (service.getAdditionalProperties().containsKey(EXTERNAL)) {
            Object externalProperty = service.getAdditionalProperties().get(EXTERNAL);
            if (externalProperty instanceof Boolean) {
                external = (Boolean) externalProperty;
            }
        }
        return external;
    }

    public KubernetesClient getKubernetesClient() {
        return kubernetesClient;
    }

    public void setKubernetesClient(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }
}
