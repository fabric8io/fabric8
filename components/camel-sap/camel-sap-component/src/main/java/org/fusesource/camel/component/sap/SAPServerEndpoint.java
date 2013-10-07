/**
 * Copyright 2013 Red Hat, Inc.
 * 
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 */
package org.fusesource.camel.component.sap;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.fusesource.camel.component.sap.SAPComponent.FunctionHandlerFactory;
import org.fusesource.camel.component.sap.model.rfc.Structure;
import org.fusesource.camel.component.sap.util.RfcUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.server.JCoServer;
import com.sap.conn.jco.server.JCoServerFactory;

/**
 * Represents an SAP server endpoint for inbound communication from SAP.
 * 
 * @author William Collins <punkhornsw@gmail.com>
 *
 */
public class SAPServerEndpoint extends SAPEndpoint {
	
    private static final Logger LOG = LoggerFactory.getLogger(SAPServerEndpoint.class);


	protected String serverName;
	protected JCoServer server;
	
	public SAPServerEndpoint() {
		super();
	}

	public SAPServerEndpoint(String endpointUri, SAPComponent component) {
		super(endpointUri, component);
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	@Override
	public SAPComponent getComponent() {
		return (SAPComponent) super.getComponent();
	}
	
	@Override
	public boolean isServer() {
		return true;
	}

	@Override
	public Producer createProducer() throws Exception {
		throw new UnsupportedOperationException(
				"Server endpoints do not support producers");
	}

	@Override
	public Consumer createConsumer(Processor processor) throws Exception {
		FunctionHandlerFactory handlerFactory = getComponent().getServerHandlerFactory(serverName);
		if (handlerFactory == null) {
			throw new IllegalStateException("Function Handler Factory for '" + serverName + "' missing.");
		}
		SAPConsumer consumer = new SAPConsumer(this, processor);
		handlerFactory.registerHandler(getRfcName(), consumer);
		return consumer;
	}

	@Override
	public Structure getRequest() throws Exception {
		return RfcUtil.getRequest(getServer().getRepository(), getRfcName());
	}

	@Override
	public Structure getResponse() throws Exception {
		return RfcUtil.getResponse(getServer().getRepository(), getRfcName());
	}
	
	protected JCoServer getServer() {
		if (server == null) {
			try {
				server = JCoServerFactory.getServer(serverName);
			} catch (JCoException e) {
				LOG.warn("Failed to get server object for endpoint. This exception will be ignored.", e);
			}
		}
		return server;
	}
}
