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

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Consumer;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerFunctionHandler;
import com.sap.conn.jco.server.JCoServerFunctionHandlerFactory;

/**
 * Represents a SAP endpoint.
 */
public class SAPEndpoint extends DefaultEndpoint {

	class FunctionHandlerFactory implements JCoServerFunctionHandlerFactory {
		
		class SessionContext {
			Map<String, Object> cachedSessionData = new HashMap<String, Object>(); 
		}
		
		private Map<String, JCoServerFunctionHandler> callHandlers = new HashMap<String, JCoServerFunctionHandler>();
		private Map<String, SessionContext> statefulSessions = new HashMap<String, SessionContext>();
		
		public void registerHandler(String functionName, JCoServerFunctionHandler handler) {
			callHandlers.put(functionName, handler);
		}

		public JCoServerFunctionHandler unregisterHandler(String functionName) {
			return callHandlers.remove(functionName);
		}

		@Override
		public void sessionClosed(JCoServerContext arg0, String arg1, boolean arg2) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public JCoServerFunctionHandler getCallHandler(JCoServerContext serverContext, String functionName) {
			JCoServerFunctionHandler handler = callHandlers.get(functionName);
			if (handler == null)
				return null;
			
			return null;
		}
		
	}
	
	protected boolean isServer;
	protected String destinationName;
	protected String serverName;
	protected String rfcName;
	protected ExchangePattern mep;
	protected JCoDestination destination;
	
	

	public SAPEndpoint() {
	}

	public SAPEndpoint(String uri, SAPComponent component) {
		super(uri, component);
	}

	public Producer createProducer() throws Exception {
		if (!isServer) {
			return new SAPProducer(this);
		}
		throw new UnsupportedOperationException(
				"Server endpoints do not support producers");
	}

	public Consumer createConsumer(Processor processor) throws Exception {
		if (isServer) {
			return new SAPConsumer(this, processor);
		}
		throw new UnsupportedOperationException(
				"Destination endpoints do not support consumers");
	}

	public boolean isSingleton() {
		return false;
	}

	public boolean isServer() {
		return isServer;
	}

	public void setServer(boolean isServer) {
		this.isServer = isServer;
	}

	public String getDestinationName() {
		return destinationName;
	}

	public void setDestinationName(String destinationName) {
		this.destinationName = destinationName;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public String getRfcName() {
		return rfcName;
	}

	public void setRfcName(String rfcName) {
		this.rfcName = rfcName;
	}

	public ExchangePattern getMep() {
		return mep;
	}

	public void setMep(ExchangePattern mep) {
		this.mep = mep;
	}
	
	protected JCoDestination getDestination() throws JCoException {
		if (destination == null) {
			destination = JCoDestinationManager.getDestination((destinationName));
		}
		return destination;
	}

}
