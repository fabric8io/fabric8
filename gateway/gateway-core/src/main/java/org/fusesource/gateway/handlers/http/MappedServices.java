/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.gateway.handlers.http;

import org.fusesource.gateway.ServiceDetails;
import org.fusesource.gateway.loadbalancer.LoadBalancer;
import org.vertx.java.core.http.HttpServerRequest;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The set of mapped services
 */
public class MappedServices {
    private final ServiceDetails serviceDetails;
    private final LoadBalancer<String> loadBalancer;
    private List<String> serviceUrls = new CopyOnWriteArrayList<String>();

    public MappedServices(String service, ServiceDetails serviceDetails, LoadBalancer<String> loadBalancer) {
        this.serviceDetails = serviceDetails;
        this.loadBalancer = loadBalancer;
        serviceUrls.add(service);
    }

    @Override
    public String toString() {
        return "MappedServices{" +
                "serviceUrls=" + serviceUrls +
                '}';
    }

    /**
     * Chooses a request to use
     */
    public String chooseService(HttpServerRequest request) {
        return loadBalancer.choose(serviceUrls, new HttpClientRequestFacade(request));
    }

    public String getContainer() {
        return serviceDetails.getContainer();
    }

    public String getVersion() {
        return serviceDetails.getVersion();
    }

    public String getId() {
        return serviceDetails.getId();
    }

    public List<String> getServiceUrls() {
        return serviceUrls;
    }
}
