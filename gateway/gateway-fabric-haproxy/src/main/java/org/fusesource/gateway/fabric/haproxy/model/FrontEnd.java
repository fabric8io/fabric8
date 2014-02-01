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
package org.fusesource.gateway.fabric.haproxy.model;

import org.fusesource.gateway.handlers.http.MappedServices;

import java.util.List;

/**
 * Represents the front end POJO for the haproxy configuration
 */
public class FrontEnd {
    private final String id;
    private final String frontUri;
    private final MappedServices services;
    private final List<BackEndServer> servers;

    public FrontEnd(String id, String frontUri, MappedServices services, List<BackEndServer> servers) {
        this.id = id;
        this.frontUri = frontUri;
        this.services = services;
        this.servers = servers;
    }

    public String getId() {
        return id;
    }

    public String getFrontUri() {
        return frontUri;
    }

    public MappedServices getServices() {
        return services;
    }

    public List<BackEndServer> getServers() {
        return servers;
    }

    public String getVersion() {
        return services.getVersion();
    }

    public String getContainer() {
        return services.getContainer();
    }

    public boolean isReverseHeaders() {
        return services.isReverseHeaders();
    }
}
