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
import io.fabric8.arquillian.utils.Util;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.utils.Filter;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import static io.fabric8.arquillian.kubernetes.Constants.ARQ_KEY;

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
        Map<String, String> labels = Collections.singletonMap(ARQ_KEY, session.getId());
        Filter<Service> serviceFilter = KubernetesHelper.createServiceFilter(labels);
        List<Service> services = Util.findServices(kubernetesClient, serviceFilter);

        if (services.isEmpty()) {
            result = false;
            session.getLogger().warn("No services are available yet, waiting...");
        } else if (configuration.isWaitForServiceConnection()) {
            for (Service s : filterServices(services, configuration.getWaitForServices())) {
                String serviceStatus = null;
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(s.getPortalIP(), s.getPort()), configuration.getServiceConnectionTimeout());
                    serviceStatus = "Service: " + s.getId() + " is ready";
                } catch (Exception e) {
                    result = false;
                    serviceStatus = "Service: " + s.getId() + " is not ready! Error: " + e.getMessage();
                } finally {
                    session.getLogger().warn(serviceStatus);
                }
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
