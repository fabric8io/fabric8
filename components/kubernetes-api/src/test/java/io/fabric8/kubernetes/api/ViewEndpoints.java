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
package io.fabric8.kubernetes.api;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.ArrayList;
import java.util.List;

import static io.fabric8.utils.Lists.notNullList;

/**
 * Views the endpoints for all services or the given service id and namespace
 */
public class ViewEndpoints {
    public static void main(String... args) {
        System.out.println("Usage: [serviceId] [namespace]");
        KubernetesClient client = new DefaultKubernetesClient();

        try {
            String service = null;
            String namespace = null;
            if (args.length > 0) {
                service = args[0];
            }
            if (args.length > 1) {
                namespace = args[1];
            }
            listEndpoints(client, service, namespace);
        } catch (Exception e) {
            System.out.println("FAILED: " + e);
            e.printStackTrace();
        }
    }

    protected static void listEndpoints(KubernetesClient client, String service, String namespace)  throws Exception {
        if (service != null) {
            Endpoints endpoints = client.endpoints().inNamespace(namespace).withName(service).get();
            display(endpoints);

        } else {
            EndpointsList endpointsList = client.endpoints().list();
            if (endpointsList != null) {
                List<Endpoints> items = notNullList(endpointsList.getItems());
                for (Endpoints item : items) {
                    display(item);
                }
            }
        }
    }

    protected static void display(Endpoints endpoints) {
        if (endpoints != null) {
            String name = endpoints.getMetadata().getName();
            String namespace = endpoints.getMetadata().getNamespace();
            List<String> urls = new ArrayList<>();
            List<EndpointSubset> endpointsSubsets = endpoints.getSubsets();

            for (EndpointSubset endpointSubset : endpointsSubsets) {
                for (EndpointAddress endpointAddress : endpointSubset.getAddresses()) {
                    for (EndpointPort endpointPort : endpointSubset.getPorts()) {
                        urls.add(endpointAddress.getIp() + ":" + endpointPort.getPort());
                    }
                }
            }

            System.out.println("Service: " + name + " namespace: " + namespace + " urls: " + urls);
        } else {
            System.out.println("null endpoints");
        }
    }

}
