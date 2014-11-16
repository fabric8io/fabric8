package io.fabric8.gateway.apiman;

import java.util.Map;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

import io.fabric8.gateway.api.apimanager.ApiManagerService;
import io.fabric8.gateway.api.handlers.http.HttpGateway;
import io.fabric8.gateway.apiman.rest.RestDispatcher;

import org.overlord.apiman.rt.engine.IEngine;
import org.overlord.apiman.rt.engine.async.IAsyncHandler;
import org.overlord.apiman.rt.engine.beans.ServiceResponse;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;
/**
 * Implementation of the ApiManagerService interface specifically for the <i>Overlord
 * ApiMan</i> API Manager.
 */
@ApplicationScoped
public class ApiManService implements ApiManagerService {

	private Vertx vertx;
	private HttpGateway httpGateway;
	
	/** the Overlord APIMan Engine */
	private IEngine engine;
	/** the REST API for the engine */
	private RestDispatcher dispatcher;
	public final static String ATTR_HTTP_CLIENT = "httpClient";
	public final static String ATTR_CLIENT_RESPONSE = "clientResponse";
	
	public void init(Map<String,Object> config) {
		vertx = (Vertx) config.get(ApiManagerService.VERTX);
		httpGateway = (HttpGateway) config.get(ApiManagerService.HTTP_GATEWAY);
		String port = (String) config.get(ApiManagerService.PORT);
		engine = Engine.create(vertx, httpGateway, port);
		dispatcher = new RestDispatcher();
	}
	
	@PreDestroy
	public void deactivateComponent() {
		engine = null;
		dispatcher = null;
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
			final HttpClient httpClient, final Object apiManagementResponseHandler) {
			return new ApiManHttpServiceResponseHandler(httpClient, (IAsyncHandler<ServiceResponse>) apiManagementResponseHandler);
	}
    /**
     * @see ApiManagerService#createHttpGatewayHandler()
     */
	@Override
	public Handler<HttpServerRequest> createApiManagerHttpGatewayHandler() {
		return new ApiManHttpGatewayHandler(vertx, httpGateway, this);
	}

	@Override
	public void handleRestRequest(HttpServerRequest request) {
		if (dispatcher!=null) {
			dispatcher.dispatch(request,this.engine);
		} else {
			request.response().setStatusCode(404);
			request.response().end("Not found");
			request.response().close();
		}
	}
	

}
