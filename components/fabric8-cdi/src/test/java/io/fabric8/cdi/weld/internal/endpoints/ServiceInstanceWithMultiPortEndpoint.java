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
package io.fabric8.cdi.weld.internal.endpoints;

import io.fabric8.annotations.Endpoint;
import io.fabric8.annotations.PortName;
import io.fabric8.annotations.Protocol;
import io.fabric8.annotations.ServiceName;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
class ServiceInstanceWithMultiPortEndpoint {

    @Inject
    @Endpoint
    @Protocol("http")
    @PortName("port2")
    @ServiceName("multiport")
    private String service;

    public String getService() {
        return service;
    }
}
