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

import io.apiman.gateway.engine.IServiceConnectionResponse;
import io.apiman.gateway.engine.async.AsyncResultImpl;
import io.apiman.gateway.engine.async.IAsyncHandler;
import io.apiman.gateway.engine.async.IAsyncResult;
import io.apiman.gateway.engine.beans.ServiceResponse;
import io.apiman.gateway.engine.io.IApimanBuffer;
import io.apiman.gateway.vertx.io.VertxApimanBuffer;

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientResponse;
/**
 * Response Handler when a call to Overlord APIMan returns. This handler is called
 * by Vert.x and invokes the APIMan org.overlord.apiman.rt.engine.async.IAsyncHandler.
 */
public class ApiManHttpServiceResponseHandler implements Handler<HttpClientResponse>{

	final HttpClient httpClient;
	final IAsyncHandler<IAsyncResult<IServiceConnectionResponse>> apiManServiceResponseHandler;

	/**
	 * Constructor which requires passing in references to
	 *
	 * @param httpClient - a Vert.x HttpClient instance.
	 * @param responseHandler - an instance of the Overlord APIMan org.overlord.apiman.rt.engine.async.IAsyncHandler.
	 */
	public ApiManHttpServiceResponseHandler(HttpClient httpClient,
			IAsyncHandler<IAsyncResult<IServiceConnectionResponse>> responseHandler) {
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
	public void handle(final HttpClientResponse clientResponse) {
		clientResponse.pause();

		final ServiceResponse serviceResponse = new ServiceResponse();
    	serviceResponse.setCode(clientResponse.statusCode());
    	serviceResponse.setMessage(clientResponse.statusMessage());
    	serviceResponse.setAttribute(ApiManService.ATTR_CLIENT_RESPONSE, clientResponse);
    	serviceResponse.setAttribute(ApiManService.ATTR_HTTP_CLIENT, httpClient);
    	Map<String,String> headerMap = new HashMap<String,String>();
    	for (String key : clientResponse.headers().names()) {
    		headerMap.put(key, clientResponse.headers().get(key));
		}
    	serviceResponse.setHeaders(headerMap);

		// Stream *to* client.
        final IServiceConnectionResponse streamToClient = new IServiceConnectionResponse() {

        	private boolean streamFinished = false;

            @Override
            public void transmit() {
            	clientResponse.resume();
            }

            @Override
            public ServiceResponse getHead() {
                return serviceResponse;
            }

            @Override
            public void abort() {
                // <SNIP>
            }

			@Override
			public void bodyHandler(final IAsyncHandler<IApimanBuffer> bodyHandler) {
				// TODO Auto-generated method stub
				clientResponse.dataHandler(new Handler<Buffer>() {
		            @Override
		            public void handle(Buffer chunk) {
		            	bodyHandler.handle(new VertxApimanBuffer(chunk));
		            }
		        });
			}

			@Override
			public void endHandler(final IAsyncHandler<Void> endHandler) {
				clientResponse.endHandler(new VoidHandler() {
					@Override
					protected void handle() {
						streamFinished = true;
						// TODO Auto-generated method stub
						endHandler.handle(null);
					}
		        });
			}

			@Override
			public boolean isFinished() {
				// TODO Auto-generated method stub
				return streamFinished;
			}

        };

        IAsyncResult<IServiceConnectionResponse> result = AsyncResultImpl.
        	    <IServiceConnectionResponse> create(streamToClient);
    	apiManServiceResponseHandler.handle(result);
	}
}
