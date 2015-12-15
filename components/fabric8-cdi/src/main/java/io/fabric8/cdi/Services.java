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
package io.fabric8.cdi;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Strings;
import io.fabric8.utils.URLUtils;

import java.util.ArrayList;
import java.util.List;

public class Services {

    public static final String DEFAULT_PROTO = "tcp";

    public static String toServiceUrl(String serviceName, String serviceProtocol, String servicePortName, String servicePath, boolean serviceExternal) {
        KubernetesClient client = KubernetesHolder.getClient();
        String serviceNamespace = client.getNamespace();
        String actualProtocol = Strings.isNullOrBlank(serviceProtocol) ? DEFAULT_PROTO : serviceProtocol;
        return URLUtils.pathJoin(KubernetesHelper.getServiceURL(client, serviceName, serviceNamespace, actualProtocol, servicePortName, serviceExternal), servicePath);
    }

    public static List<String> toServiceEndpointUrl(String serviceId, String serviceProtocol, String servicePort) {
        List<String> endpoints = new ArrayList<>();
        KubernetesClient client = KubernetesHolder.getClient();
        String namespace = client.getNamespace();
        String actualProtocol = serviceProtocol != null ? serviceProtocol : DEFAULT_PROTO;

        Endpoints item = KubernetesHolder.getClient().endpoints().inNamespace(namespace).withName(serviceId).get();
        if (item != null) {
            for (EndpointSubset subset : item.getSubsets()) {
                for (EndpointAddress address : subset.getAddresses()) {
                    for (EndpointPort endpointPort : subset.getPorts()) {
                        if (servicePort == null || servicePort.equals(endpointPort.getName())) {
                            endpoints.add(actualProtocol + "://" + address.getIp() + ":" + endpointPort.getPort());
                        }
                    }
                }
            }
        }
        return endpoints;
    }
}
