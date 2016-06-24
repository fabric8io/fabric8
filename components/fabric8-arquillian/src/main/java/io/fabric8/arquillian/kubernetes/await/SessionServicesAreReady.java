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
package io.fabric8.arquillian.kubernetes.await;

import io.fabric8.arquillian.kubernetes.Configuration;
import io.fabric8.arquillian.kubernetes.Session;
import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static io.fabric8.kubernetes.api.KubernetesHelper.getName;

public class SessionServicesAreReady implements Callable<Boolean> {

    private final Session session;
    private final KubernetesClient kubernetesClient;
    private final Configuration configuration;

    public SessionServicesAreReady(KubernetesClient kubernetesClient, Session session, Configuration configuration) {
        this.session = session;
        this.kubernetesClient = kubernetesClient;
        this.configuration = configuration;
    }

    @Override
    public Boolean call() throws Exception {
        boolean result = true;
        List<Service> services = kubernetesClient.services().inNamespace(session.getNamespace()).list().getItems();

        if (services.isEmpty()) {
            result = false;
            session.getLogger().warn("No services are available yet, waiting...");
        } else if (configuration.isWaitForServiceConnectionEnabled()) {
            for (Service s : filterServices(services, configuration.getWaitForServiceList())) {
                if (!isEndpointAvailable(s)) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Checks if there is an endpoint for the service available.
     * @param s The target service.
     * @return  Returns true if a connection to at least one of the endpoints is possible.
     */
    private boolean isEndpointAvailable(Service s) {
        String serviceStatus = null;
        boolean result = false;
        String sid = getName(s);
        String namespace = session.getNamespace();
        Endpoints endpoints = kubernetesClient.endpoints().inNamespace(namespace).withName(sid).get();
        ServiceSpec spec = s.getSpec();
        if (endpoints != null && spec != null) {
            List<EndpointSubset> subsets = endpoints.getSubsets();
            if (subsets != null) {
                for (EndpointSubset subset : subsets) {
                    List<EndpointAddress> addresses = subset.getAddresses();
                    if (addresses != null) {
                        for (EndpointAddress address : addresses) {
                            String ip = address.getIp();
                            String addr = ip;
/*
    TODO v1beta2...
                            String addr = endpoit.substring(0, endpoit.indexOf(":"));
                            Integer port = Integer.parseInt(endpoit.substring(endpoit.indexOf(":") + 1));
*/
                            List<ServicePort> ports = spec.getPorts();
                            for (ServicePort port : ports) {
                                Integer portNumber = port.getPort();
                                if (portNumber != null && portNumber > 0) {
                                    if (configuration.isWaitForServiceConnectionEnabled()) {
                                        try (Socket socket = new Socket()) {
                                            socket.connect(new InetSocketAddress(ip, portNumber), (int) configuration.getWaitForServiceConnectionTimeout());
                                            serviceStatus = "Service: " + sid + " is ready. Provider:" + addr + ".";
                                            return true;
                                        } catch (Exception e) {
                                            serviceStatus = "Service: " + sid + " is not ready! in namespace " + namespace + ". Error: " + e.getMessage();
                                        } finally {
                                            session.getLogger().warn(serviceStatus);
                                        }
                                    } else {
                                        serviceStatus = "Service: " + sid + " is ready. Not testing connecting to it!. Provider:" + addr + ".";
                                        session.getLogger().warn(serviceStatus);
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        session.getLogger().warn("Service: " + sid + " has no valid endpoints");
        return result;
    }


    private List<Service> filterServices(List<Service> services, List<String> selectedIds) {
        if (selectedIds != null && !selectedIds.isEmpty()) {
            List<Service> result = new ArrayList<>();
            for (Service s : services) {
                String sid = getName(s);
                if (selectedIds.contains(sid)) {
                    result.add(s);
                }
            }
            return result;
        } else {
            return services;
        }
    }

}
