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
package io.fabric8.cdi.producers;

import io.fabric8.cdi.Services;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Producer;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class FirstEndpointProducer implements Producer<String> {

    private final String serviceId;
    private final String serviceProtocol;
    private final String servicePort;

    public FirstEndpointProducer(String serviceId) {
        this(serviceId, Services.DEFAULT_PROTO, null);
    }

    public FirstEndpointProducer(String serviceId, String serviceProtocol, String servicePort) {
        this.serviceId = serviceId;
        this.serviceProtocol = serviceProtocol;
        this.servicePort = servicePort;
    }

    @Override
    public String produce(CreationalContext<String> ctx) {
        if (serviceId == null) {
            throw new IllegalArgumentException("No service id has been specified.");
        }

        List<String> endpoints = Services.toServiceEndpointUrl(serviceId, serviceProtocol, servicePort);
        if (endpoints == null || endpoints.isEmpty()) {
            return null;
        } else {
            return endpoints.get(0);
        }
    }

    @Override
    public void dispose(String instance) {
        //do nothing
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }
}
