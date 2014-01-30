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

import org.fusesource.gateway.loadbalancer.ClientRequestFacade;
import org.vertx.java.core.http.HttpServerRequest;

/**
 * A default implementation of {@link ClientRequestFacade} for HTTP requests.
 * <br>
 * Note we may require other implementations such as one using Cookies or request parameter values.
 */
public class HttpClientRequestFacade implements ClientRequestFacade {
    private final HttpServerRequest request;

    public HttpClientRequestFacade(HttpServerRequest request) {
        this.request = request;
    }

    @Override
    public String getClientRequestKey() {
        return request.netSocket().localAddress().toString();
    }
}
