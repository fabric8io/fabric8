package org.fusesource.camel.component.sap;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;

/**
 * Represents a SAP endpoint.
 */
public class SAPEndpoint extends DefaultEndpoint {

    public SAPEndpoint() {
    }

    public SAPEndpoint(String uri, SAPComponent component) {
        super(uri, component);
    }

    public SAPEndpoint(String endpointUri) {
        super(endpointUri);
    }

    public Producer createProducer() throws Exception {
        return new SAPProducer(this);
    }

    public Consumer createConsumer(Processor processor) throws Exception {
        return new SAPConsumer(this, processor);
    }

    public boolean isSingleton() {
        return true;
    }
}
