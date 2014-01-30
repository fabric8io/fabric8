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
package org.fusesource.gateway.handlers.http.policy;

import org.fusesource.gateway.handlers.http.MappedServices;
import org.fusesource.gateway.handlers.http.ProxyMappingDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Rewrites URL in the <code>Location</code>, <code>Content-Location</code> and <code>URI</code> headers on HTTP
 * redirect responses.
 * <p>
 * Similar to the [ProxyPassReverse setting in mod_proxy](http://httpd.apache.org/docs/current/mod/mod_proxy.html#proxypassreverse)
 */
public class ReverseUriPolicy implements Handler<HttpClientResponse> {
    private static final transient Logger LOG = LoggerFactory.getLogger(ReverseUriPolicy.class);

    private final MappedServices mappedServices;
    private final HttpServerRequest request;
    private final Handler<HttpClientResponse> delegate;
    private final ProxyMappingDetails proxyMappingDetails;
    private final String[] rewriteHeaders = {"Location", "Content-Location", "URI"};

    public ReverseUriPolicy(MappedServices mappedServices, HttpServerRequest request, Handler<HttpClientResponse> delegate, ProxyMappingDetails proxyMappingDetails) {
        this.mappedServices = mappedServices;
        this.request = request;
        this.delegate = delegate;
        this.proxyMappingDetails = proxyMappingDetails;
    }

    @Override
    public void handle(HttpClientResponse clientResponse) {
        delegate.handle(clientResponse);

        MultiMap headers = clientResponse.headers();
        for (String headerName : rewriteHeaders) {
            List<String> headerValues = headers.getAll(headerName);
            int size = headerValues.size();
            if (size > 0) {
                List<String> newHeaders = new ArrayList<String>(size);
                for (String headerValue : headerValues) {
                    String newValue = headerValue;
                    if (headerValue != null && headerValue.length() > 0) {
                        newValue = proxyMappingDetails.rewriteBackendUrl(headerValue);
                    }
                }
                LOG.info("Rewriting header " + headerName + " from: " + headerValues + " to: " + newHeaders);
                headers.set(headerName, newHeaders);
            }
        }
    }
}
