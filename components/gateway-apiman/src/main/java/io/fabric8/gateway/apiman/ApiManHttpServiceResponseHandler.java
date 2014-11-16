package io.fabric8.gateway.apiman;

import java.util.HashMap;
import java.util.Map;

import org.overlord.apiman.rt.engine.async.AsyncResultImpl;
import org.overlord.apiman.rt.engine.async.IAsyncHandler;
import org.overlord.apiman.rt.engine.beans.ServiceResponse;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientResponse;
/**
 * Response Handler when a call to Overlord APIMan returns. This handler is called
 * by Vert.x and invokes the APIMan org.overlord.apiman.rt.engine.async.IAsyncHandler. 
 */
public class ApiManHttpServiceResponseHandler implements Handler<HttpClientResponse>{

	final HttpClient httpClient;
	final IAsyncHandler<ServiceResponse> apiManServiceResponseHandler;
	
	/**
	 * Constructor which requires passing in references to
	 * 
	 * @param httpClient - a Vert.x HttpClient instance.
	 * @param responseHandler - an instance of the Overlord APIMan org.overlord.apiman.rt.engine.async.IAsyncHandler.
	 */
	public ApiManHttpServiceResponseHandler(HttpClient httpClient,
			IAsyncHandler<ServiceResponse> responseHandler) {
		super();
		this.httpClient = httpClient;
		this.apiManServiceResponseHandler = responseHandler;
	}
	/**
	 * The handler creates an org.overlord.apiman.rt.engine.beans.ServiceResponse and 
	 * invokes the APIMan ServiceResponseHandler.
	 * 
	 * @param clientResponse - the vert.x HttpClientResponse from the fabric8 services.
	 */
	@Override
	public void handle(HttpClientResponse clientResponse) {
		ServiceResponse serviceResponse = new ServiceResponse();
    	serviceResponse.setCode(clientResponse.statusCode());
    	serviceResponse.setMessage(clientResponse.statusMessage());
    	serviceResponse.setAttribute(ApiManService.ATTR_CLIENT_RESPONSE, clientResponse);
    	serviceResponse.setAttribute(ApiManService.ATTR_HTTP_CLIENT, httpClient);
    	Map<String,String> headerMap = new HashMap<String,String>();
    	for (String key : clientResponse.headers().names()) {
    		headerMap.put(key, clientResponse.headers().get(key));
		}
    	serviceResponse.setHeaders(headerMap);
    	
    	apiManServiceResponseHandler.handle(AsyncResultImpl.create(serviceResponse));
	}
}
