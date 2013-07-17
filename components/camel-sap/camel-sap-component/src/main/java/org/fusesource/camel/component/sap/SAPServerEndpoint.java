package org.fusesource.camel.component.sap;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.fusesource.camel.component.sap.SAPEndpoint.FunctionHandlerFactory.SessionContext;

import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.server.JCoServer;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerFactory;
import com.sap.conn.jco.server.JCoServerFunctionHandler;
import com.sap.conn.jco.server.JCoServerFunctionHandlerFactory;

public class SAPServerEndpoint extends DefaultEndpoint {

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
	
	@Override
	public Producer createProducer() throws Exception {
		throw new UnsupportedOperationException(
				"Server endpoints do not support producers");
	}

	@Override
	public Consumer createConsumer(Processor processor) throws Exception {
		return new SAPConsumer(this, processor);
	}

	@Override
	public boolean isSingleton() {
		// TODO Auto-generated method stub
		return false;
	}

	protected JCoServer getServer() throws JCoException {
		if (server == null) {
			server = JCoServerFactory.getServer(serverName);
		}
		return server;
	}
}
