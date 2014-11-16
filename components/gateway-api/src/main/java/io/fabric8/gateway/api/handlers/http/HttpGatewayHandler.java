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
package io.fabric8.gateway.api.handlers.http;

import io.fabric8.gateway.api.CallDetailRecord;
import io.fabric8.gateway.api.handlers.http.HttpGatewayClient;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;

/**
 */
public class HttpGatewayHandler implements Handler<HttpServerRequest> {
    
	private HttpGatewayClient httpGatewayClient;
	private final HttpGateway httpGateway;
    
    public HttpGatewayHandler(final Vertx vertx, final HttpGateway httpGateway) {
        this.httpGateway = httpGateway;
        httpGatewayClient = new HttpGatewayClient(vertx, httpGateway);
    }

    @Override
    public void handle(final HttpServerRequest request) {
    	final long callStart = System.nanoTime();
		httpGatewayClient.execute(request, null);
		CallDetailRecord cdr = new CallDetailRecord(System.nanoTime() - callStart, request.response().getStatusMessage());
        httpGateway.addCallDetailRecord(cdr);
    	
    }
}
