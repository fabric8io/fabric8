package org.fusesource.camel.component.sap;

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;

/**
 * Represents the component that manages {@link SAPEndpoint}.
 */
public class SAPComponent extends DefaultComponent {

    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        Endpoint endpoint = new SAPEndpoint(uri, this);
        setProperties(endpoint, parameters);
        return endpoint;
    }
}
