package io.fabric8.gateway.api.handlers.http;

import io.fabric8.gateway.api.ServiceDetails;
import io.fabric8.gateway.api.handlers.http.ProxyMappingDetails;

import java.util.List;

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

	public abstract List<String> getServiceUrls();

}