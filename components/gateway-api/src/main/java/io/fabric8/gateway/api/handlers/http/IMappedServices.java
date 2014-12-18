/*
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
package io.fabric8.gateway.api.handlers.http;

import io.fabric8.gateway.api.ServiceDetails;
import io.fabric8.gateway.api.handlers.http.ProxyMappingDetails;

import java.util.Set;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;

public interface IMappedServices {

	/**
	 * Chooses a request to use
	 */
	public abstract String chooseService(HttpServerRequest request);

	/**
	 * Provides a hook so we can wrap a client response handler in a policy such
	 * as to reverse the URIs {@link io.fabric8.gateway.handlers.http.policy.ReverseUriPolicy} or
	 * add metering, limits, security or contract checks etc.
	 */
	public abstract Handler<HttpClientResponse> wrapResponseHandlerInPolicies(
			HttpServerRequest request,
			Handler<HttpClientResponse> responseHandler,
			ProxyMappingDetails proxyMappingDetails);

	/**
	 * Rewrites the URI response from a request to a URI in the gateway namespace
	 */
	public abstract String rewriteUrl(String proxiedUrl);

	public abstract String getContainer();

	public abstract String getVersion();

	public abstract String getId();

	public abstract boolean isReverseHeaders();

	public abstract ServiceDetails getServiceDetails();

	public abstract Set<String> getServiceUrls();

}