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
package io.fabric8.cdi.producers;

import io.fabric8.cdi.Services;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Producer;
import java.util.Collections;
import java.util.Set;

public class ServiceUrlProducer implements Producer<String> {

    private final String serviceId;
    private final String serviceProtocol;
    private final String servicePortName;
    private final String servicePath;
    private final Boolean serviceExternal;

    public ServiceUrlProducer(String serviceId, String serviceProtocol, String servicePortName, String servicePath, Boolean serviceExternal) {
        this.serviceId = serviceId;
        this.serviceProtocol = serviceProtocol;
        this.servicePortName = servicePortName;
        this.servicePath = servicePath;
        this.serviceExternal = serviceExternal;
    }

    @Override
    public String produce(CreationalContext<String> ctx) {
        if (serviceId == null) {
            throw new IllegalArgumentException("No service id has been specified.");
        }
        return Services.toServiceUrl(serviceId, serviceProtocol, servicePortName, servicePath, serviceExternal);
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
