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

import io.fabric8.gateway.api.handlers.http.HttpGateway;
import io.fabric8.gateway.api.handlers.http.HttpGatewayServiceClient;

import io.apiman.gateway.engine.IConnectorFactory;
import io.apiman.gateway.engine.IEngine;
import io.apiman.gateway.engine.IEngineFactory;
import io.apiman.gateway.engine.IRegistry;
import io.apiman.gateway.engine.impl.DefaultEngineFactory;
import org.vertx.java.core.Vertx;

public class Engine {

	/**
	 * The APIMan Engine that applies policies before and after each service request.
	 * The engine's configuration is persisted by a JSON file called registry.json
	 * which lives in the data/apiman directory.
	 * 
	 * @param vertx - a reference to Vert.x
	 * @param httpGateway - a reference to a HttpGateway implementation.
	 * @return IEngine - the APIMan Engine that applies policies.
	 */
	public static IEngine create(final Vertx vertx, final HttpGateway httpGateway, final String port) {
		
		IEngineFactory factory = new DefaultEngineFactory() {
			
			@Override
			protected IConnectorFactory createConnectorFactory() {
				HttpGatewayServiceClient httpGatewayServiceClient = new HttpGatewayServiceClient(vertx, httpGateway);
				return new Fabric8ConnectorFactory(vertx, httpGatewayServiceClient);
			}
			
			@Override
			protected IRegistry createRegistry() {
				try {
					FileBackedRegistry registry = new FileBackedRegistry();
					registry.load(port);
					return registry;
				} catch (Exception e) {
					throw new RuntimeException(e.getMessage(),e);
				}
			}
			
		};
		IEngine apimanEngine = factory.createEngine();
		
        return apimanEngine;
	}
	
}
