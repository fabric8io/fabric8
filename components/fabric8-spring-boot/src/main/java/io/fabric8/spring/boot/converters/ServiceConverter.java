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
package io.fabric8.spring.boot.converters;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteList;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.utils.KubernetesServices;
import io.fabric8.utils.Strings;
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
import static io.fabric8.spring.boot.Constants.PORT;
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
        String serviceProtocol = getProtocolOfService(source);
        String servicePort = getPortOfService(source);

        String str = getServiceURL(kubernetesClient, source, serviceProtocol, servicePort);
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


    private String getPortOfService(Service service) {
        String port = null;
        if (service.getAdditionalProperties().containsKey(PORT)) {
            Object portProperty = service.getAdditionalProperties().get(PORT);
            if (portProperty instanceof String) {
                port = (String) portProperty;
            }
        }
        return port;
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

    public String getServiceURL(KubernetesClient client, Service srv, String serviceProtocol, String servicePortName) {
        String serviceName = KubernetesHelper.getName(srv);
        String serviceProto = serviceProtocol != null ? serviceProtocol : KubernetesServices.serviceToProtocol(serviceName, servicePortName);

        if (Strings.isNullOrBlank(servicePortName) && KubernetesHelper.isOpenShift(client)) {
            OpenShiftClient openShiftClient = client.adapt(OpenShiftClient.class);
            RouteList routeList = openShiftClient.routes().list();
            for (Route route : routeList.getItems()) {
                if (route.getSpec().getTo().getName().equals(serviceName)) {
                    return (serviceProto + "://" + route.getSpec().getHost()).toLowerCase();
                }
            }
        }
        ServicePort port = KubernetesHelper.findServicePortByName(srv, servicePortName);
        if (port == null) {
            throw new RuntimeException("Couldn't find port: " + servicePortName + " for service:" + serviceName);
        }

        String clusterIP = srv.getSpec().getClusterIP();
        if ("None".equals(clusterIP)) {
            throw new IllegalStateException("Service " + serviceName + " is head-less. Search for endpoints instead.");
        }

        return (serviceProto + "://" + clusterIP + ":" + port.getPort()).toLowerCase();
    }

    public KubernetesClient getKubernetesClient() {
        return kubernetesClient;
    }

    public void setKubernetesClient(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }
}
