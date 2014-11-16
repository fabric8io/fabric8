package io.fabric8.gateway.apiman;

import io.fabric8.gateway.api.handlers.http.HttpGateway;
import io.fabric8.gateway.api.handlers.http.HttpGatewayClient;

import org.overlord.apiman.rt.engine.IConnectorFactory;
import org.overlord.apiman.rt.engine.IEngine;
import org.overlord.apiman.rt.engine.IEngineFactory;
import org.overlord.apiman.rt.engine.IRegistry;
import org.overlord.apiman.rt.engine.impl.DefaultEngineFactory;
import org.vertx.java.core.Vertx;

public class Engine {

	/**
	 * 
	 * @param vertx - a reference to Vert.x
	 * @param httpGateway - a reference to a HttpGateway implementation.
	 * @return
	 */
	public static IEngine create(final Vertx vertx, final HttpGateway httpGateway, final String port) {
		
		IEngineFactory factory = new DefaultEngineFactory() {
			
			@Override
			protected IConnectorFactory createConnectorFactory() {
				HttpGatewayClient httpGatewayClient = new HttpGatewayClient(vertx, httpGateway);
				return new Fabric8ConnectorFactory(vertx, httpGatewayClient);
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
