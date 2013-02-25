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
package org.elasticsearch.http;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.jmx.MBean;
import org.elasticsearch.jmx.ManagedAttribute;

@MBean(objectName = "service=http", description = "Http")
public class HttpServerManagement {

    private final HttpServer httpServer;

    @Inject
    public HttpServerManagement(HttpServer httpServer) {
        this.httpServer = httpServer;
    }

    @ManagedAttribute(description = "Transport address published to other nodes")
    public String getPublishAddress() {
        return httpServer.info().address().publishAddress().toString();
    }

    @ManagedAttribute(description = "Transport address bounded on")
    public String getBoundAddress() {
        return httpServer.info().address().boundAddress().toString();
    }

    @ManagedAttribute(description = "Total open")
    public long getTotalOpen() {
        return httpServer.stats().totalOpen();
    }

    @ManagedAttribute(description = "Server open")
    public long getServerOpen() {
        return httpServer.stats().serverOpen();
    }

}
