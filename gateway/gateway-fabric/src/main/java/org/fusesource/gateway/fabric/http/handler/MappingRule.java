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
package org.fusesource.gateway.fabric.http.handler;

import org.fusesource.common.util.Strings;
import org.fusesource.gateway.ServiceDetails;
import org.vertx.java.core.http.HttpServerRequest;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * a mapping rule
 */
public class MappingRule {
    private Set<String> serviceUrls = new CopyOnWriteArraySet<String>();

    public MappingRule(List<String> services) {
        serviceUrls.addAll(services);
    }

    public MappingRule(String... services) {
        for (String service : services) {
            serviceUrls.add(service);
        }
    }

    @Override
    public String toString() {
        return "MappingRule{" +
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

    public Set<String> getServiceUrls() {
        return serviceUrls;
    }

    public void setServiceUrls(Set<String> serviceUrls) {
        this.serviceUrls = serviceUrls;
    }
}
