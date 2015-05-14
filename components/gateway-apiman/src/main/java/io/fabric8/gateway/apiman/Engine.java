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

import io.apiman.gateway.api.rest.contract.exceptions.NotAuthorizedException;
import io.apiman.gateway.engine.IEngine;
import io.apiman.gateway.engine.IEngineFactory;
import io.apiman.gateway.engine.IEngineResult;
import io.apiman.gateway.engine.IRegistry;
import io.apiman.gateway.engine.IServiceRequestExecutor;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.Service;
import io.apiman.gateway.engine.beans.ServiceRequest;
import io.fabric8.gateway.api.apimanager.ServiceMapping;
import io.fabric8.gateway.api.handlers.http.HttpGateway;
import io.fabric8.gateway.api.handlers.http.IMappedServices;

import java.util.Iterator;
import java.util.Map;

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
	public ApiManEngine create(final Vertx vertx, final HttpGateway httpGateway, final String port) {
		IEngineFactory factory = new EngineFactory(vertx, httpGateway);
		if ("in-memory".equals(System.getProperty("fabric8-apiman.engine-factory"))) {
		    factory = new InMemoryEngineFactory(vertx, httpGateway);
		}
		final IEngine engine = factory.createEngine();
		ApiManEngine apimanEngine = new ApiManEngine() {

		    @Override
		    public IRegistry getRegistry() {
		        return engine.getRegistry();
		    }

			@Override
			public String getVersion() {
				return engine.getVersion();
			}

			@Override
			public IServiceRequestExecutor executor(ServiceRequest request,
					IAsyncResultHandler<IEngineResult> resultHandler) {
				return engine.executor(request, resultHandler);
			}

			/**
			 * @see io.fabric8.gateway.apiman.ApiManEngine#serviceMapping(io.apiman.gateway.engine.beans.Service)
			 */
			@Override
			public String serviceMapping(Service service) throws NotAuthorizedException {
				String serviceUrl = service.getEndpoint();
				Map<String, IMappedServices> mappedServices = httpGateway.getMappedServices();
				Iterator<String> keys = mappedServices.keySet().iterator();
				while (keys.hasNext()) {
					String key = keys.next();
					IMappedServices services = mappedServices.get(key);
					String servicePath = services.getProxyMappingDetails().getProxyServiceUrl();
					if (servicePath.equals(serviceUrl)) {
						String gatewayUrl = httpGateway.getGatewayUrl() + key;
						return gatewayUrl;
					}
				}
				throw new NotAuthorizedException("Service not found");
			}

			@Override
			public ServiceMapping getServiceMapping(String servicePath) {
				return ((DelegatingRegistryWithMapping) engine.getRegistry()).getService(servicePath);
			}
		};
        return apimanEngine;
	}
}
