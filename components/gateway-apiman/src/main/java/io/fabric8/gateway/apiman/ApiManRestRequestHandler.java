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
import io.fabric8.gateway.apiman.rest.RestDispatcher;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
/**
 * Handles the rest requests from APIMan Console, to configure the engine.
 *
 */
public class ApiManRestRequestHandler implements Handler<HttpServerRequest> {

	private IEngine engine;
	/** the REST API dispatcher for configuration calls to the engine */
	private RestDispatcher dispatcher;
    
	public ApiManRestRequestHandler(IEngine engine) {
		super();
		this.engine = engine;
		dispatcher = new RestDispatcher();
	}

	@Override
	public void handle(HttpServerRequest request) {
		dispatcher.dispatch(request,this.engine);
	}

}
