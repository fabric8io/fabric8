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
import org.apache.camel.Processor;
import org.apache.camel.Producer;

import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.server.JCoServer;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerFactory;
import com.sap.conn.jco.server.JCoServerFunctionHandler;
import com.sap.conn.jco.server.JCoServerFunctionHandlerFactory;

/**
 * Represents an SAP server endpoint for inbound communication from SAP.
 */
public class SAPServerEndpoint extends SAPEndpoint {

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
		public void sessionClosed(JCoServerContext serverContext, String arg1, boolean arg2) {
			statefulSessions.remove(serverContext.getSessionID());
		}

		@Override
		public JCoServerFunctionHandler getCallHandler(JCoServerContext serverContext, String functionName) {
			JCoServerFunctionHandler handler = callHandlers.get(functionName);
			if (handler == null)
				return null;
			
			return null;
		}
		
	}
	
	protected String serverName;
	protected String rfcName;
	protected JCoServer server;
	
	public SAPServerEndpoint() {
		super();
	}

	public SAPServerEndpoint(String endpointUri, SAPComponent component) {
		super(endpointUri, component);
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
		return new SAPConsumer(this, processor);
	}

	protected JCoServer getServer() throws JCoException {
		if (server == null) {
			server = JCoServerFactory.getServer(serverName);
		}
		return server;
	}
}
