package org.fusesource.camel.component.sap;

import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;

/**
 * The SAP consumer.
 */
public class SAPConsumer extends DefaultConsumer {

	public SAPConsumer(SAPEndpoint endpoint, Processor processor) {
		super(endpoint, processor);
	}

}
