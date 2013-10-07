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

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;
import org.fusesource.camel.component.sap.model.rfc.DestinationData;
import org.fusesource.camel.component.sap.model.rfc.DestinationDataStore;
import org.fusesource.camel.component.sap.model.rfc.RepositoryData;
import org.fusesource.camel.component.sap.model.rfc.RepositoryDataStore;
import org.fusesource.camel.component.sap.model.rfc.RfcFactory;
import org.fusesource.camel.component.sap.model.rfc.ServerData;
import org.fusesource.camel.component.sap.model.rfc.ServerDataStore;
import org.fusesource.camel.component.sap.util.ComponentDestinationDataProvider;
import org.fusesource.camel.component.sap.util.ComponentServerDataProvider;
import org.fusesource.camel.component.sap.util.RfcUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.conn.jco.JCoCustomRepository;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.server.JCoServer;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerContextInfo;
import com.sap.conn.jco.server.JCoServerErrorListener;
import com.sap.conn.jco.server.JCoServerExceptionListener;
import com.sap.conn.jco.server.JCoServerFactory;
import com.sap.conn.jco.server.JCoServerFunctionHandler;
import com.sap.conn.jco.server.JCoServerFunctionHandlerFactory;
import com.sap.conn.jco.server.JCoServerState;
import com.sap.conn.jco.server.JCoServerStateChangedListener;

/**
 * Represents the component that manages {@link SAPDestinationEndpoint} and {@link SAPServerEndpoint} instances.
 * 
 * @author William Collins <punkhornsw@gmail.com>
 *
 */
public class SAPComponent extends DefaultComponent {

	private static final Logger LOG = LoggerFactory.getLogger(SAPComponent.class);
	
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
			return handler;
		}
		
	}
	
	public class ServerErrorAndExceptionListener implements JCoServerErrorListener, JCoServerExceptionListener {

		@Override
		public void serverExceptionOccurred(JCoServer jcoServer, String connectionId,
				JCoServerContextInfo serverContext, Exception exception) {
			LOG.warn(">>> Exception occured on " + jcoServer.getProgramID() + " connection " + connectionId, exception);
		}

		@Override
		public void serverErrorOccurred(JCoServer jcoServer, String connectionId,
				JCoServerContextInfo serverContext, Error error) {
			LOG.warn(">>> Error occured on " + jcoServer.getProgramID() + " connection " + connectionId, error);
		}
		
	}
	
	public class ServerStateChangedListener implements JCoServerStateChangedListener {

		@Override
		public void serverStateChangeOccurred(JCoServer jcoServer,
				JCoServerState oldState, JCoServerState newState) {
			LOG.info(">>> Server state changed from " + oldState.toString() + " to " + newState.toString() + " on " + jcoServer.getProgramID());
		}
		
	}

	protected DestinationDataStore destinationDataStore = RfcFactory.eINSTANCE.createDestinationDataStore();
	
	protected ServerDataStore serverDataStore = RfcFactory.eINSTANCE.createServerDataStore();
	
	protected RepositoryDataStore repositoryDataStore = RfcFactory.eINSTANCE.createRepositoryDataStore();
	
	protected Map<String,JCoServer> activeServers = new HashMap<String,JCoServer>();
	
	protected Map<String,JCoCustomRepository> repositories = new HashMap<String,JCoCustomRepository>();
	
	protected ServerErrorAndExceptionListener serverErrorAndExceptionListener = new ServerErrorAndExceptionListener();
	
	protected ServerStateChangedListener serverStateChangedListener = new ServerStateChangedListener();
	
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
    	
    	// Parse URI
    	String[] urlComponents  = remaining.split(":");
    	if (urlComponents.length != 3) {
    		throw new IllegalArgumentException("URI must be of the form: sap:[destination:<destinationName>|server:<serverName>]:<rfcName>");
    	}
    	
    	Endpoint endpoint;
    	if (urlComponents[0].equals("destination")) {
    		parameters.put("destinationName", urlComponents[1]);
    		parameters.put("rfcName", urlComponents[2]);
    		endpoint = new SAPDestinationEndpoint(uri, this);
    	} else if (urlComponents[0].equals("server")) {
    		parameters.put("serverName", urlComponents[1]);
    		parameters.put("rfcName", urlComponents[2]);
    		endpoint = new SAPServerEndpoint(uri, this);
    	} else {
    		throw new IllegalArgumentException("Must specify 'client' or 'server' in URL");
    	}

        setProperties(endpoint, parameters);
        return endpoint;
    }
    
    public void setDestinationDataStore(Map<String, DestinationData> destinationDataEntries) {
    	destinationDataStore.getEntries().clear();
    	destinationDataStore.getEntries().putAll(destinationDataEntries);
    }
    
    public Map<String, DestinationData> getDestinationDataStore() {
    	return destinationDataStore.getEntries().map();
    }
    
    public void setServerDataStore(Map<String, ServerData> serverDataEntries) {
    	serverDataStore.getEntries().clear();
    	serverDataStore.getEntries().putAll(serverDataEntries);
    }
    
    public Map<String, ServerData> getServerDataStore() {
    	return serverDataStore.getEntries().map();
    }
    
    public Map<String, RepositoryData> getRepositoryDataStore() {
		return repositoryDataStore.getEntries().map();
	}

	public void setRepositoryDataStore(Map<String, RepositoryData> repositoryDataEntries) {
		this.repositoryDataStore.getEntries().clear();
		this.repositoryDataStore.getEntries().putAll(repositoryDataEntries);
	}
	
	synchronized protected JCoServer getServer(String serverName) throws Exception {
		JCoServer server = activeServers.get(serverName);
		if (server == null) {
			server = JCoServerFactory.getServer(serverName);
			
			server.setCallHandlerFactory(new FunctionHandlerFactory());
			
			server.addServerExceptionListener(serverErrorAndExceptionListener);
			server.addServerErrorListener(serverErrorAndExceptionListener);
			server.addServerStateChangedListener(serverStateChangedListener);
			
			JCoCustomRepository repository = getRepository(serverName);
			if (repository != null) {
				String repositoryDestination = server.getRepositoryDestination();
				if (repositoryDestination != null) {
					try {
						repository.setDestination(JCoDestinationManager.getDestination(repositoryDestination));
					} catch (Exception e) {
						LOG.warn("Unable to set destination on custom repository '" + serverName + "'", e);
					}
				}
				server.setRepository(repository);
			}
			
			activeServers.put(serverName, server);
			
			if (isStarted()) {
				server.start();
				LOG.debug("Started server " + server.getProgramID());
			}
		}
		return server;
	}
	
	protected FunctionHandlerFactory getServerHandlerFactory(String serverName) throws Exception {
		JCoServer server = getServer(serverName);
		if (server == null) {
			return null;
		}
		return (FunctionHandlerFactory) server.getCallHandlerFactory();
	}
	
	synchronized protected JCoCustomRepository getRepository(String serverName) {
		JCoCustomRepository repository = repositories.get(serverName);
		if (repository == null) {
			RepositoryData repositoryData = repositoryDataStore.getEntries().get(serverName);
			if (repositoryData != null) {
				repository = RfcUtil.createRepository(serverName, repositoryData);
				repositories.put(serverName, repository);
			}
		}
		return repository;
	}

	@Override
    protected void doStart() throws Exception {
    	super.doStart();
    	ComponentDestinationDataProvider.INSTANCE.addDestinationDataStore(destinationDataStore);
    	ComponentServerDataProvider.INSTANCE.addServerDataStore(serverDataStore);
    	for(JCoServer server: activeServers.values()) {
    		server.start();
    	}
    }
    
    @Override
    protected void doStop() throws Exception {
    	ComponentDestinationDataProvider.INSTANCE.removeDestinationDataStore(destinationDataStore);
    	ComponentServerDataProvider.INSTANCE.removeServerDataStore(serverDataStore);
    	super.doStop();
    }
}
