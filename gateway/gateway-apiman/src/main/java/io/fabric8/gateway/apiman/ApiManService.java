package io.fabric8.gateway.apiman;

import io.fabric8.gateway.api.apimanager.ApiManagerService;
import io.fabric8.gateway.api.handlers.http.HttpGateway;
import io.fabric8.gateway.api.handlers.http.HttpGatewayHandler;
import io.fabric8.gateway.api.handlers.http.HttpServiceResponseHandler;

import org.overlord.apiman.rt.engine.IEngine;
import org.overlord.apiman.rt.engine.async.IAsyncHandler;
import org.overlord.apiman.rt.engine.beans.ServiceResponse;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;
/**
 * Implementation of the IApiManager interface specifically for the <i>Overlord
 * ApiMan</i> API Manager.
 */
public class ApiManService implements ApiManagerService {

	private boolean apiManagerEnabled = false;
	
	/** the Overlord APIMan Engine */
	private IEngine engine;
	public final static String ATTR_HTTP_CLIENT = "httpClient";
	public final static String ATTR_CLIENT_RESPONSE = "clientResponse";
	
	/**
	 * @see ApiManagerService#isApiManagerEnabled()
	 */
	@Override
	public boolean isApiManagerEnabled() {
		return apiManagerEnabled;
	}
	
	/**
	 * @see ApiManagerService#setApiManagerEnabled(boolean)
	 */
	@Override
	public void setApiManagerEnabled(boolean apiManagerEnabled) {
		this.apiManagerEnabled = apiManagerEnabled;
	}
	
	/**
	 * @see ApiManagerService#setEngine(Object)
	 */
	@Override
	public void setEngine(Object engine) {
		setApiManagerEnabled(true);
		this.engine = (IEngine) engine;
	}
	/**
	 * @see ApiManagerService#getEngine(Object)
	 */
	@Override
	public Object getEngine() {
		return engine;
	}
	/**
	 * @see HttpGateway#createClientResponseHandler(HttpClient, HttpServerRequest, Object)
	 */
	@SuppressWarnings("unchecked")
	@Override 
	public Handler<HttpClientResponse> createServiceResponseHandler(
			final HttpClient httpClient, final HttpServerRequest httpServerRequest,
			final Object apiManagementResponseHandler) {
		if (isApiManagerEnabled()) {
			return new ApiManHttpServiceResponseHandler(httpClient, (IAsyncHandler<ServiceResponse>) apiManagementResponseHandler);
		} else {
			return new HttpServiceResponseHandler(httpClient, httpServerRequest);
		}
	}
    /**
     * @see ApiManagerService#createHttpGatewayHandler(Vertx, HttpGateway)
     */
	@Override
	public Handler<HttpServerRequest> createHttpGatewayHandler(Vertx vertx,
			HttpGateway httpGateway) {
		if (isApiManagerEnabled()) {
			return new ApiManHttpGatewayHandler(vertx, httpGateway, this);
		} else {
			return new HttpGatewayHandler(vertx, httpGateway);
		}
	}

}
