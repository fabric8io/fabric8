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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;

public class HttpServiceResponseHandler implements Handler<HttpClientResponse>{

	private static final transient Logger LOG = LoggerFactory.getLogger(HttpServiceResponseHandler.class);

	final HttpClient httpClient;
	final HttpServerRequest request;
	
	public HttpServiceResponseHandler(HttpClient httpClient,
			HttpServerRequest request) {
		super();
		this.httpClient = httpClient;
		this.request = request;
	}
	
	@Override
	public void handle(HttpClientResponse clientResponse) {
		request.response().setStatusCode(clientResponse.statusCode());
        request.response().headers().set(clientResponse.headers());
        request.response().setChunked(true);
        clientResponse.dataHandler(new Handler<Buffer>() {
            public void handle(Buffer data) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Proxying response body:" + data);
                }
                request.response().write(data);
            }
        });
        clientResponse.endHandler(new VoidHandler() {
            public void handle() {
                request.response().end();
                httpClient.close();
            }
        });
	}
}
