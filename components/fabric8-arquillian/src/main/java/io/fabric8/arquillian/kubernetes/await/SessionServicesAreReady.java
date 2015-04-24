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

package io.fabric8.arquillian.kubernetes.await;

import io.fabric8.arquillian.kubernetes.Configuration;
import io.fabric8.arquillian.kubernetes.Session;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.model.Service;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

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
        List<Service> services = kubernetesClient.getServices(session.getNamespace()).getItems();

        if (services.isEmpty()) {
            result = false;
            session.getLogger().warn("No services are available yet, waiting...");
        } else if (configuration.isWaitForServiceConnection()) {
            for (Service s : filterServices(services, configuration.getWaitForServices())) {
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
        for (String endpoit : kubernetesClient.endpointsForService(s.getId(), s.getNamespace()).getEndpoints()) {
            String addr = endpoit.substring(0, endpoit.indexOf(":"));
            Integer port = Integer.parseInt(endpoit.substring(endpoit.indexOf(":") + 1));
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(addr, port), configuration.getServiceConnectionTimeout());
                serviceStatus = "Service: " + s.getId() + " is ready. Provider:"+ addr+".";
                return true;
            } catch (Exception e) {
                serviceStatus = "Service: " + s.getId() + " is not ready! Error: " + e.getMessage();
            } finally {
                session.getLogger().warn(serviceStatus);
            }
        }
        return result;
    }


    private List<Service> filterServices(List<Service> services, List<String> selectedIds) {
        if (selectedIds != null && !selectedIds.isEmpty()) {
            List<Service> result = new ArrayList<>();
            for (Service s : services) {
                if (selectedIds.contains(s.getId())) {
                    result.add(s);
                }
            }
            return result;
        } else {
            return services;
        }
    }

}
