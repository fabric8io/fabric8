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

import io.fabric8.gateway.api.handlers.http.HttpGatewayServiceClient;
import io.apiman.gateway.engine.IConnectorFactory;
import io.apiman.gateway.engine.IServiceConnection;
import io.apiman.gateway.engine.IServiceConnectionResponse;
import io.apiman.gateway.engine.IServiceConnector;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.Service;
import io.apiman.gateway.engine.beans.ServiceRequest;
import io.apiman.gateway.engine.beans.exceptions.ConnectorException;
import io.apiman.gateway.engine.io.IApimanBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpServerRequest;

/**
 * Connector factory that uses HTTP to invoke back end systems.
 *
 */
public class Fabric8ConnectorFactory implements IConnectorFactory {
    
	private static final transient Logger LOG = LoggerFactory.getLogger(Fabric8ConnectorFactory.class);

    private HttpGatewayServiceClient httpGatewayClient;

    /**
     * Constructor.
     */
    public Fabric8ConnectorFactory(Vertx vertx, HttpGatewayServiceClient httpGatewayClient) {
    	this.httpGatewayClient = httpGatewayClient;
    }

    /**
     * @see org.overlord.apiman.rt.engine.IConnectorFactory#createConnector(org.overlord.apiman.rt.engine.beans.ServiceRequest, org.overlord.apiman.rt.engine.beans.Service)
     */
    @Override
    public IServiceConnector createConnector(ServiceRequest request, final Service service) {
        // A reference to the connector is handed to APIMan
        IServiceConnector connector = 		
        		new IServiceConnector() {

			@Override
			public IServiceConnection connect(ServiceRequest serviceRequest,
					IAsyncResultHandler<IServiceConnectionResponse> handler)
					throws ConnectorException {
				
				final HttpServerRequest httpRequest = (HttpServerRequest) serviceRequest.getRawRequest();
            	httpRequest.headers().set(serviceRequest.getHeaders());
            	final HttpClientRequest vxServiceClientRequest = httpGatewayClient.execute(httpRequest, handler);
            	
            	//returning the connection, which will call the service
            	return new IServiceConnection() {
                    private boolean streamFinished = false;
 
                    
                    @Override
                    public void abort() {
                    	LOG.warn("Abort called for request " + httpRequest.path());
                    	//do we need to any other cleanup?
                        end();
                    }
 
                    @Override
                    public void write(IApimanBuffer chunk) {
                    	if (LOG.isDebugEnabled()) {
                    		LOG.debug("Writing chuck for " + httpRequest.path() + ": " + chunk);
                    	}
                        if(streamFinished) {
                            throw new IllegalStateException("Attempted write to connector after #end() was called."); //$NON-NLS-1$
                        }            
 
                        if(chunk.getNativeBuffer() instanceof Buffer) {
                        	vxServiceClientRequest.write((Buffer) chunk.getNativeBuffer());
                        } else {
                            throw new IllegalArgumentException("Chunk not of expected Vert.x Buffer type."); //$NON-NLS-1$
                        }
                    }
 
                    @Override
                    public void end() {
                    	vxServiceClientRequest.end();
                        streamFinished = true;
                    }
 
                    @Override
                    public boolean isFinished() {
                        return streamFinished;
                    }
                };
			}
        };
        return connector;
    }

}
