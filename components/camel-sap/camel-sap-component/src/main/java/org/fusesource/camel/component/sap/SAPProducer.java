package org.fusesource.camel.component.sap;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SAP producer.
 */
public class SAPProducer extends DefaultProducer {
    private static final transient Logger LOG = LoggerFactory.getLogger(SAPProducer.class);

    public SAPProducer(SAPEndpoint endpoint) {
        super(endpoint);
    }

    public void process(Exchange exchange) throws Exception {
    	LOG.debug(exchange.getIn().getBody().toString());
    }

}
