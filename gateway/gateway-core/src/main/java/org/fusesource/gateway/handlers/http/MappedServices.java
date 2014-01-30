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
import org.fusesource.gateway.handlers.http.policy.ReverseUriPolicy;
import org.fusesource.gateway.loadbalancer.LoadBalancer;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents the mapped services and the relevant mapping information so that a service implementation can be
 * chosen using a load balancer together with wrapping the client in whatever policies are required.
 */
public class MappedServices {
    private final ServiceDetails serviceDetails;
    private final LoadBalancer<String> loadBalancer;
    private final boolean reverseHeaders;
    private List<String> serviceUrls = new CopyOnWriteArrayList<String>();

    public MappedServices(String service, ServiceDetails serviceDetails, LoadBalancer<String> loadBalancer, boolean reverseHeaders) {
        this.serviceDetails = serviceDetails;
        this.loadBalancer = loadBalancer;
        this.reverseHeaders = reverseHeaders;
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

    /**
     * Provides a hook so we can wrap a client response handler in a policy such
     * as to reverse the URIs {@link org.fusesource.gateway.handlers.http.policy.ReverseUriPolicy} or
     * add metering, limits, security or contract checks etc.
     */
    public Handler<HttpClientResponse> wrapResponseHandlerInPolicies(HttpServerRequest request, Handler<HttpClientResponse> responseHandler, ProxyMappingDetails proxyMappingDetails) {
        if (reverseHeaders) {
            responseHandler = new ReverseUriPolicy(this, request, responseHandler, proxyMappingDetails);
        }
        return responseHandler;
    }

    /**
     * Rewrites the URI response from a request to a URI in the gateway namespace
     */
    public String rewriteUrl(String proxiedUrl) {
        return proxiedUrl;
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

    public boolean isReverseHeaders() {
        return reverseHeaders;
    }

    public ServiceDetails getServiceDetails() {
        return serviceDetails;
    }

    public List<String> getServiceUrls() {
        return serviceUrls;
    }
}