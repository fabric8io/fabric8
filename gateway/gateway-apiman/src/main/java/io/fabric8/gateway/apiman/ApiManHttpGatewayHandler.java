/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.gateway.apiman;

import io.fabric8.gateway.api.CallDetailRecord;
import io.fabric8.gateway.api.apimanager.ApiManagerService;
import io.fabric8.gateway.api.handlers.http.HttpGateway;

import java.util.HashMap;
import java.util.Map;

import org.overlord.apiman.rt.engine.EngineResult;
import org.overlord.apiman.rt.engine.IEngine;
import org.overlord.apiman.rt.engine.async.IAsyncHandler;
import org.overlord.apiman.rt.engine.async.IAsyncResult;
import org.overlord.apiman.rt.engine.beans.PolicyFailure;
import org.overlord.apiman.rt.engine.beans.PolicyFailureType;
import org.overlord.apiman.rt.engine.beans.ServiceRequest;
import org.overlord.apiman.rt.engine.beans.ServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

/**
 * 
 */
public class ApiManHttpGatewayHandler implements Handler<HttpServerRequest> {
    private static final transient Logger LOG = LoggerFactory.getLogger(ApiManHttpGatewayHandler.class);

    private final HttpGateway httpGateway;
    private ApiManagerService apiManager;
    
    public ApiManHttpGatewayHandler(final Vertx vertx, final HttpGateway httpGateway, ApiManagerService apiManager) {
        this.httpGateway = httpGateway;
    	LOG.info("HTTP Requests are routed via APIMan");
    	this.apiManager = apiManager;
    }

    /**
     * Handles the request coming into the gateway from an external client application.
     * This handler implements the Overlord APIMan IAsyncHandler which is invoked
     * by the APIMan Engine when the engine completes it work. It is in this
     * handler we respond back to the external client application.
     */
    @Override
    public void handle(final HttpServerRequest request) {
    	final long callStart = System.nanoTime();
    
		//1. Create APIMan ServiceRequest
		ServiceRequest srequest = new ServiceRequest();
		srequest.setRawRequest(request);
		srequest.setApiKey(getApiKey(request));
        srequest.setType(request.method());
        
        srequest.setRemoteAddr(request.remoteAddress().getAddress().getHostAddress());
        srequest.setDestination(request.path());
        Map<String,String> headerMap = new HashMap<String,String>();
        for (String key : request.headers().names()) {
    		headerMap.put(key, request.headers().get(key));
		}
        srequest.setHeaders(headerMap);
        
        final HttpServerResponse response = request.response();
        //2. Create APIMan Handler and execute
        IAsyncHandler<EngineResult> asyncHandler = new IAsyncHandler<EngineResult>() {
			@Override
			public void handle(IAsyncResult<EngineResult> result) {
				
				if (result.isError()) {
					//This can happen when an internal exception occurs; we need to send back a 500
					response.setStatusCode(500);
					response.setStatusMessage("Gateway Internal Error");
					response.end();
					LOG.error("Gateway Internal Error: " + result.getError().getMessage(), result.getError());
				} else {
					EngineResult engineResult = result.getResult();
					
					if (engineResult.isFailure()) {
						ServiceResponse serviceResponse = engineResult.getServiceResponse();
						if (serviceResponse!=null) {
							final HttpClient finalClient = (HttpClient) serviceResponse.getAttribute("finalClient");
							finalClient.close();
						}
						PolicyFailure policyFailure = engineResult.getPolicyFailure();
						response.putHeader("X-Policy-Failure-Type", String.valueOf(policyFailure.getType()));
						response.putHeader("X-Policy-Failure-Message", policyFailure.getMessage());
						response.putHeader("X-Policy-Failure-Code", String.valueOf(policyFailure.getFailureCode()));
				        int errorCode = 500;
				        if (policyFailure.getType() == PolicyFailureType.Authentication) {
				            errorCode = 401;
				        } else if (policyFailure.getType() == PolicyFailureType.Authorization) {
				            errorCode = 403;
				        }
				        response.setStatusCode(errorCode);
						response.setStatusMessage(policyFailure.getMessage());
						response.end();
					} else if (engineResult.isResponse()) {
						 
						ServiceResponse serviceResponse = engineResult.getServiceResponse();
						
						response.setStatusCode(serviceResponse.getCode());
						response.setStatusMessage(serviceResponse.getMessage());
						
						
						HttpClientResponse clientResponse = (HttpClientResponse) serviceResponse.getAttribute(ApiManService.ATTR_CLIENT_RESPONSE);
						if (clientResponse != null) {
							final HttpClient httpClient = (HttpClient) serviceResponse.getAttribute(ApiManService.ATTR_HTTP_CLIENT);
							
							response.setChunked(true);
	                        clientResponse.dataHandler(new Handler<Buffer>() {
	                            public void handle(Buffer data) {
	                                if (LOG.isDebugEnabled()) {
	                                    LOG.debug("Proxying response body:" + data);
	                                }
	                                response.write(data);
	                            }
	                        });
	                        clientResponse.endHandler(new VoidHandler() {
	                            public void handle() {
	                            	response.end();
	                                httpClient.close();
	                            }
	                        });
							
							LOG.debug("ResponseCode from downstream " + clientResponse.statusCode());
						}
						CallDetailRecord cdr = new CallDetailRecord(System.nanoTime() - callStart, clientResponse.statusMessage());
						httpGateway.addCallDetailRecord(cdr);
					}
				}
			}
        };
        ((IEngine) apiManager.getEngine()).execute(srequest, asyncHandler);
    }
    
    /**
     * Gets the API Key from the request.  The API key can be passed either via
     * a custom http request header called X-API-Key or else by a query parameter
     * in the URL called apikey.
     * @param request the inbound request
     * @return the api key or null if not found
     */
    protected String getApiKey(HttpServerRequest request) {
        String apiKey = request.headers().get("X-API-Key"); //$NON-NLS-1$
        if (apiKey == null || apiKey.trim().length() == 0) {
            apiKey = getApiKeyFromQuery(request);
        }
        return apiKey;
    }

    /**
     * Gets the API key from the request's query string.
     * @param request the inbound request
     * @return the api key or null if not found
     */
    protected String getApiKeyFromQuery(HttpServerRequest request) {
        String queryString = request.query();
        if (queryString == null) {
            return null;
        }
        int idx = queryString.indexOf("apikey="); //$NON-NLS-1$
        if (idx >= 0) {
            int endIdx = queryString.indexOf('&', idx);
            if (endIdx == -1) {
                endIdx = queryString.length();
            }
            return queryString.substring(idx + 7, endIdx);
        } else {
            return null;
        }
    }


}
