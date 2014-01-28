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

import org.fusesource.common.util.Strings;
import org.fusesource.gateway.ServiceDetails;
import org.vertx.java.core.http.HttpServerRequest;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * The set of mapped services
 */
public class MappedServices {
    private final ServiceDetails serviceDetails;
    private Set<String> serviceUrls = new CopyOnWriteArraySet<String>();

    public MappedServices(String service, ServiceDetails serviceDetails) {
        this.serviceDetails = serviceDetails;
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
        // TODO should this be random or sticky etc?
        for (String serviceUrl : serviceUrls) {
            if (Strings.isNotBlank(serviceUrl)) {
                return serviceUrl;
            }
        }
        return null;
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

    public Set<String> getServiceUrls() {
        return serviceUrls;
    }

    public void setServiceUrls(Set<String> serviceUrls) {
        this.serviceUrls = serviceUrls;
    }
}
