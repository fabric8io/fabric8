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

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;

/**
 * Represents an SAP destination endpoint for outbound communication to SAP.
 */
public class SAPDestinationEndpoint extends SAPEndpoint {

	protected String destinationName;
	protected String rfcName;
	protected JCoDestination destination;

	public SAPDestinationEndpoint() {
		super();
	}

	public SAPDestinationEndpoint(String endpointUri, SAPComponent component) {
		super(endpointUri, component);
	}

	@Override
	public boolean isServer() {
		return false;
	}

	@Override
	public Producer createProducer() throws Exception {
		return new SAPProducer(this);	}

	@Override
	public Consumer createConsumer(Processor processor) throws Exception {
		throw new UnsupportedOperationException(
				"Destination endpoints do not support consumers");
	}

	public String getDestinationName() {
		return destinationName;
	}

	public void setDestinationName(String destinationName) {
		this.destinationName = destinationName;
	}

	public String getRfcName() {
		return rfcName;
	}

	public void setRfcName(String rfcName) {
		this.rfcName = rfcName;
	}

	protected JCoDestination getDestination() throws JCoException {
		if (destination == null) {
			destination = JCoDestinationManager.getDestination((destinationName));
		}
		return destination;
	}
}
