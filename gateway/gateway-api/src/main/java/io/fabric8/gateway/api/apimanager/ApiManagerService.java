package io.fabric8.gateway.api.apimanager;

import io.fabric8.gateway.api.handlers.http.HttpGateway;
import io.fabric8.gateway.api.handlers.http.HttpGatewayHandler;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;

/**
 * Fabric8's API Manager interface. The interface provides access to a 3rd party 'Engine',
 * as well as access to handler needed for the asynchronous nature of Fabric8's HTTPGateway.
 * The ApiManager 
 */
public interface ApiManagerService {

    /**
     * The Gateway will be a simple proxy, or send requests via an API Manager
     * true - gateway requests are routed via an API Manager
     * false - gateway requests are not send via an API Manager and the gateway functions
     * as a simple proxy.
     */
	public abstract boolean isApiManagerEnabled();
	
	/**
	 * @param apiManagerEnabled - set to true if requests are to be routed via an API Manager
	 */
	public abstract void setApiManagerEnabled(boolean apiManagerEnabled);
    
	/** Set a reference to a 3rd Party API Manager Engine */
	public abstract void setEngine(Object apiManagerEngine);
	
	/** Get a reference to a 3rd Party API Manager Engine */
	public abstract Object getEngine();
	
	/**
     * Creates a Handler of type HttpClientResponse. The HttpGateway knows if it is configured 
     * to run with an API Manager or as a simple proxy. Therefore it can return the correct handler
     * to be registered with the vert.x framework. This handler is called to handle a response
     * from a fabric8 service.
     * 
     * @param httpClient - vert.x HttpClient used to call the fabric8 service
     * @param httpServerRequest - vert.x HttpServerRequest used to create a HttpServiceResponseHandler
     *  when no API Manager is used
     * @param apiManagementResponseHandler -  
     * @return
     * 
     * @See       HttpServiceResponseHandler
     * @See ApiManHttpServiceResponseHandler
     * 
     */
    public abstract Handler<HttpClientResponse> createServiceResponseHandler(HttpClient httpClient,
			HttpServerRequest httpServerRequest, Object apiManagementResponseHandler);
    
	/**
	 * Creates an implementation of a Handler of type HttpServerRequest, and returns a reference.
	 * This handler is called by the vert.x framework, when when the gateway invoked by an external
	 * client application. 
	 * 
	 * @param vertx - a reference to Vert.x
	 * @param httpGateway - a reference to a HttpGateway implementation.
	 * @return a Handler of type HttpServerRequest
	 * 
	 * @see       HttpGatewayHandler
	 * @see ApiManHttpGatewayHandler
	 */
	public abstract Handler<HttpServerRequest> createHttpGatewayHandler(final Vertx vertx, final HttpGateway httpGateway);
		
}
