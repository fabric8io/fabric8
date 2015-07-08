/**
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

import io.fabric8.gateway.api.CallDetailRecord;
import io.fabric8.gateway.api.handlers.http.HttpGatewayServiceClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpServerRequest;

/**
 */
public class HttpGatewayHandler implements Handler<HttpServerRequest> {
    
	private static final transient Logger LOG = LoggerFactory.getLogger(HttpGatewayHandler.class);
	
	private HttpGatewayServiceClient httpGatewayClient;
	private final HttpGateway httpGateway;
    
    public HttpGatewayHandler(final Vertx vertx, final HttpGateway httpGateway) {
        this.httpGateway = httpGateway;
        httpGatewayClient = new HttpGatewayServiceClient(vertx, httpGateway);
    }

    @Override
    public void handle(final HttpServerRequest request) {
    	
    	//If this is a request is show the mapping then repond right away
    	if (HttpMapping.isMappingIndexRequest(request, httpGateway)) {
    		HttpMapping.respond(request, httpGateway);
    		return;
    	}
    	
    	final long callStart = System.nanoTime();
    	final HttpClientRequest serviceRequest = httpGatewayClient.execute(request, null);
    	
    	//Sending the request to the service
		request.dataHandler(new Handler<Buffer>() {
            public void handle(Buffer data) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Proxying request body:" + data);
                }
                serviceRequest.write(data);
            }
        });
        request.endHandler(new VoidHandler() {
            public void handle() {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("end of the request");
                }
                serviceRequest.end();
            }
        });
		CallDetailRecord cdr = new CallDetailRecord(System.nanoTime() - callStart, request.response().getStatusMessage());
        httpGateway.addCallDetailRecord(cdr);
    	
    }
}
