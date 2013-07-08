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

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;
import org.fusesource.camel.component.sap.model.rfc.DestinationData;
import org.fusesource.camel.component.sap.model.rfc.DestinationDataStore;
import org.fusesource.camel.component.sap.model.rfc.RfcFactory;
import org.fusesource.camel.component.sap.model.rfc.ServerData;
import org.fusesource.camel.component.sap.model.rfc.ServerDataStore;

/**
 * Represents the component that manages {@link SAPEndpoint}.
 */
public class SAPComponent extends DefaultComponent {

	protected DestinationDataStore destinationDataStore = RfcFactory.eINSTANCE.createDestinationDataStore();
	
	protected ServerDataStore serverDataStore = RfcFactory.eINSTANCE.createServerDataStore();
	
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
    	
    	// Parse URI
    	String[] urlComponents  = remaining.split(":");
    	if (urlComponents.length != 3) {
    		throw new IllegalArgumentException("URI must be of the form: sap:[destination:<destinationName>|server:<serverName>]:<rfcName>");
    	}
    	
    	if (urlComponents[0].equals("destination")) {
    		parameters.put("server", false);
    		parameters.put("destinationName", urlComponents[1]);
    		parameters.put("rfcName", urlComponents[2]);
    	} else if (urlComponents[0].equals("server")) {
    		parameters.put("server", true);
    		parameters.put("serverName", urlComponents[1]);
    		parameters.put("rfcName", urlComponents[2]);
    	} else {
    		throw new IllegalArgumentException("Must specify 'client' or 'server' in URL");
    	}

    	Endpoint endpoint = new SAPEndpoint(uri, this);
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
}
