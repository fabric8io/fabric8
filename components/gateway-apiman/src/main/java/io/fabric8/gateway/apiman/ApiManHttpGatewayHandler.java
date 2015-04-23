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
package io.fabric8.gateway.apiman;

import io.apiman.gateway.engine.IEngine;
import io.apiman.gateway.engine.IEngineResult;
import io.apiman.gateway.engine.IServiceRequestExecutor;
import io.apiman.gateway.engine.async.IAsyncHandler;
import io.apiman.gateway.engine.async.IAsyncResult;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.PolicyFailure;
import io.apiman.gateway.engine.beans.PolicyFailureType;
import io.apiman.gateway.engine.beans.ServiceRequest;
import io.apiman.gateway.engine.beans.ServiceResponse;
import io.apiman.gateway.engine.io.IApimanBuffer;
import io.apiman.gateway.engine.io.ISignalWriteStream;
import io.apiman.gateway.vertx.io.VertxApimanBuffer;
import io.fabric8.gateway.api.CallDetailRecord;
import io.fabric8.gateway.api.apimanager.ApiManagerService;
import io.fabric8.gateway.api.handlers.http.HttpGateway;
import io.fabric8.gateway.api.handlers.http.HttpMapping;
import io.fabric8.gateway.api.handlers.http.IMappedServices;
import io.fabric8.gateway.api.handlers.http.ProxyMappingDetails;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

/**
 *
 */
public class ApiManHttpGatewayHandler implements Handler<HttpServerRequest> {
    private static final transient Logger LOG = LoggerFactory.getLogger(ApiManHttpGatewayHandler.class);

    private final HttpGateway httpGateway;
    private final ApiManagerService apiManager;

    public ApiManHttpGatewayHandler(final Vertx vertx, final HttpGateway httpGateway, final ApiManagerService apiManager) {
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

    	final HttpServerResponse response = request.response();
    	try {
	    	//0. If this is a request is show the mapping then respond right away
	    	if (HttpMapping.isMappingIndexRequest(request, httpGateway)) {
	    		HttpMapping.respond(request, httpGateway);
	    		return;
	    	}

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

	        IMappedServices mappedServices = HttpMapping.getMapping(request, httpGateway.getMappedServices());
	        if (mappedServices!=null) {
		    	ProxyMappingDetails proxyMappingDetails = mappedServices.getProxyMappingDetails();
		        String[] apiManagerServiceInfo = apiManager.getApiManagerServiceInfo(proxyMappingDetails.getServicePath());
		        if (apiManagerServiceInfo==null) throw new Exception("Service Not Found in API Manager.");
		        srequest.setServiceOrgId(apiManagerServiceInfo[0]);
		        srequest.setServiceId(apiManagerServiceInfo[1]);
		        srequest.setServiceVersion(apiManagerServiceInfo[2]);
	        } else {
	        	throw new Exception("Service Not Found in API Manager.");
	        }

	        //2. Create APIMan Handler and execute
	        IEngine engine = (IEngine) apiManager.getEngine();
	    	// Request executor, through which we can send chunks and indicate end.
			final IServiceRequestExecutor requestExecutor = engine.executor(srequest,
					new IAsyncResultHandler<IEngineResult>() {

					@Override
					public void handle(IAsyncResult<IEngineResult> iAsyncEngineResult) {

					if (! iAsyncEngineResult.isSuccess()) {
						//This can happen only when an (unexpected) internal exception occurs; we need to send back a 500
						response.setStatusCode(500);
						response.setStatusMessage("Gateway Internal Error: " + iAsyncEngineResult.getError().getMessage());
						response.end();
						LOG.error("Gateway Internal Error " + iAsyncEngineResult.getError().getMessage());
					} else {
						IEngineResult engineResult = iAsyncEngineResult.getResult();
						if (engineResult.isFailure()) {
							//The iAsyncEngineResult can be successful, but a policy can have been violated which
							//will mark the engineResult as failed.
							ServiceResponse serviceResponse = engineResult.getServiceResponse();
							if (serviceResponse!=null) {
								final HttpClient finalClient = (HttpClient) serviceResponse.getAttribute("finalClient");
								finalClient.close();
							}
							PolicyFailure policyFailure = engineResult.getPolicyFailure();
							response.putHeader("X-Policy-Failure-Type", String.valueOf(policyFailure.getType()));
							response.putHeader("X-Policy-Failure-Message", policyFailure.getMessage());
							response.putHeader("X-Policy-Failure-Code", String.valueOf(policyFailure.getFailureCode()));
					        int errorCode = 403; // Default status code for policy failure
					        if (policyFailure.getType() == PolicyFailureType.Authentication) {
					            errorCode = 401;
					        } else if (policyFailure.getType() == PolicyFailureType.Authorization) {
					            errorCode = 401;
					        }
					        response.setStatusCode(errorCode);
							response.setStatusMessage(policyFailure.getMessage());
							response.end();
							//response.close();
						} else if (engineResult.isResponse()) {
							//All is happy and we can respond back to the client.
							ServiceResponse serviceResponse = engineResult.getServiceResponse();
							response.setStatusCode(serviceResponse.getCode());
							response.setStatusMessage(serviceResponse.getMessage());
							response.setChunked(true);

							 // bodyHandler to receive response chunks.
					          engineResult.bodyHandler(new IAsyncHandler<IApimanBuffer>() {

					            @Override
					            public void handle(IApimanBuffer chunk) {

					              // Important: retrieve native buffer format directly if possible, much more efficient.
					            	response.write((Buffer) chunk.getNativeBuffer());
					            }
					          });

					          // endHandler to receive end signal.
					          engineResult.endHandler(new IAsyncHandler<Void>() {

					            @Override
					            public void handle(Void flag) {
					            	LOG.debug("ResponseCode from downstream " + response.getStatusCode());
									CallDetailRecord cdr = new CallDetailRecord(System.nanoTime() - callStart, response.getStatusMessage());
									httpGateway.addCallDetailRecord(cdr);
									response.end();
					            	//response.close();
					            	LOG.debug("Complete success, and response end.");
					            }
					          });
						}
					}
				}
			});
			//Create a streamHandler so APIMan can use it to stream the client request
			//to the back-end service
			requestExecutor.streamHandler(new IAsyncHandler<ISignalWriteStream>() {
				  @Override
				  public void handle(final ISignalWriteStream writeStream) {
				    request.dataHandler(new Handler<Buffer>() {
			            @Override
                        public void handle(Buffer data) {
			            	IApimanBuffer apimanBuffer = new VertxApimanBuffer(data);
			        		writeStream.write(apimanBuffer);
			            }
			        });
				    writeStream.end();
				  }
			});
			//Hand responsibility to APIMan
			requestExecutor.execute();
		} catch (Exception e) {
			response.setStatusCode(404);
			response.setStatusMessage("User error " + e.getMessage());
			response.end();
			LOG.error("User error " + e.getMessage());
		}
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
