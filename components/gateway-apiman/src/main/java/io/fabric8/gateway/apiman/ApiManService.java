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
import io.apiman.gateway.engine.async.IAsyncHandler;
import io.apiman.gateway.engine.async.IAsyncResult;
import io.fabric8.gateway.api.apimanager.ApiManagerService;
import io.fabric8.gateway.api.apimanager.ServiceMapping;
import io.fabric8.gateway.api.handlers.http.HttpGateway;

import java.util.Map;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
/**
 * Implementation of the ApiManagerService interface specifically for the <i>Overlord
 * ApiMan</i> API Manager.
 */
@ApplicationScoped
public class ApiManService implements ApiManagerService {

	private static final transient Logger LOG = LoggerFactory.getLogger(ApiManService.class);
	private Vertx vertx;
	private HttpGateway httpGateway;

	/** the APIMan Engine */
	private ApiManEngine engine;
	/** the REST API server to configure the engine */
	private HttpServer engineRestServer;
	public final static String ATTR_HTTP_CLIENT = "httpClient";
	public final static String ATTR_CLIENT_RESPONSE = "clientResponse";

	@Override
    public void init(Map<String,Object> config) {
		LOG.info("Initializing the ApiMan Engine..");
		vertx = (Vertx) config.get(ApiManagerService.VERTX);
		httpGateway = (HttpGateway) config.get(ApiManagerService.HTTP_GATEWAY);
		String port = (String) config.get(ApiManagerService.PORT);
		engine = new Engine().create(vertx, httpGateway, port);
		engineRestServer = vertx.createHttpServer();
		int portRest = Integer.valueOf(port) - 1;
		if (config.containsKey(ApiManagerService.PORT_REST)) portRest = (Integer) config.get(ApiManagerService.PORT_REST);
		engineRestServer.requestHandler(new ApiManRestRequestHandler(engine));
		engineRestServer.listen(portRest, "localhost");
		LOG.info("The ApiMan REST Service is listening at on port " + portRest);

	}

	@PreDestroy
	public void deactivateComponent() {
		engineRestServer.close();
		engineRestServer = null;
		engine = null;
	}

	/**
	 * @see ApiManagerService#getEngine(Object)
	 */
	@Override
	public Object getEngine() {
		return engine;
	}
	/**
	 * @see HttpGateway#createClientResponseHandler(HttpClient, HttpServerRequest, Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Handler<HttpClientResponse> createServiceResponseHandler(
			final HttpClient httpClient, final Object apiManagementResponseHandler) {
			return new ApiManHttpServiceResponseHandler(httpClient, (IAsyncHandler<IAsyncResult<IServiceConnectionResponse>>) apiManagementResponseHandler);
	}
    /**
     * @see ApiManagerService#createHttpGatewayHandler()
     */
	@Override
	public Handler<HttpServerRequest> createApiManagerHttpGatewayHandler() {
		return new ApiManHttpGatewayHandler(vertx, httpGateway, this);
	}

	@Override
	public ServiceMapping getApiManagerServiceMapping(String servicePath) {
		return engine.getServiceMapping(servicePath);
	}

}
