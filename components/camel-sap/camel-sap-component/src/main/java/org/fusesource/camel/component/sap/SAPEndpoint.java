package org.fusesource.camel.component.sap;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;

/**
 * Represents a SAP endpoint.
 */
public class SAPEndpoint extends DefaultEndpoint {

	protected boolean isServer;
	protected String destinationName;
	protected String serverName;
	protected String rfcName;

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
}
