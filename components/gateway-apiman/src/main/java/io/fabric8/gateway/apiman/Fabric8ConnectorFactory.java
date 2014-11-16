/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.gateway.apiman;

import io.fabric8.gateway.api.handlers.http.HttpGatewayClient;

import org.overlord.apiman.rt.engine.IConnectorFactory;
import org.overlord.apiman.rt.engine.IServiceConnector;
import org.overlord.apiman.rt.engine.async.AsyncResultImpl;
import org.overlord.apiman.rt.engine.async.IAsyncHandler;
import org.overlord.apiman.rt.engine.beans.Service;
import org.overlord.apiman.rt.engine.beans.ServiceRequest;
import org.overlord.apiman.rt.engine.beans.ServiceResponse;
import org.overlord.apiman.rt.engine.beans.exceptions.ConnectorException;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;

/**
 * Connector factory that uses HTTP to invoke back end systems.
 *
 */
public class Fabric8ConnectorFactory implements IConnectorFactory {
    
    private HttpGatewayClient httpGatewayClient;

    /**
     * Constructor.
     */
    public Fabric8ConnectorFactory(Vertx vertx, HttpGatewayClient httpGatewayClient) {
    	this.httpGatewayClient = httpGatewayClient;
    }

    /**
     * @see org.overlord.apiman.rt.engine.IConnectorFactory#createConnector(org.overlord.apiman.rt.engine.beans.ServiceRequest, org.overlord.apiman.rt.engine.beans.Service)
     */
    @Override
    public IServiceConnector createConnector(ServiceRequest request, final Service service) {
        return new IServiceConnector() {
            /**
             * @see org.overlord.apiman.rt.engine.IServiceConnector#invoke(org.overlord.apiman.rt.engine.beans.ServiceRequest, org.overlord.apiman.rt.engine.async.IAsyncHandler)
             */
            @Override
            public void invoke(ServiceRequest serviceRequest, IAsyncHandler<ServiceResponse> handler)
                    throws ConnectorException {
                try {
                	HttpServerRequest httpRequest = (HttpServerRequest) serviceRequest.getRawRequest();
                	httpRequest.headers().set(serviceRequest.getHeaders());
                	httpGatewayClient.execute(httpRequest, handler);
                    
                } catch (Throwable e) {
                    handler.handle(AsyncResultImpl.<ServiceResponse>create(e));
                }
            }
        };
    }

}
