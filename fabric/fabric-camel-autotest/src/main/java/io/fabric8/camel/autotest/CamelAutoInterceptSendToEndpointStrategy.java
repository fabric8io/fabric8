/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.fabric8.camel.autotest;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Producer;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.InterceptSendToEndpoint;
import org.apache.camel.spi.EndpointStrategy;
import org.apache.camel.util.EndpointHelper;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.camel.util.ObjectHelper.wrapRuntimeCamelException;

public class CamelAutoInterceptSendToEndpointStrategy implements EndpointStrategy {

    // TODO: Extend from camel-core when CAMEL-7154 has been merged

    private static final Logger LOG = LoggerFactory.getLogger(CamelAutoInterceptSendToEndpointStrategy.class);
    private final String pattern = null;
    private boolean skip;

    public Endpoint registerEndpoint(String uri, Endpoint endpoint) {
        if (endpoint instanceof InterceptSendToEndpoint) {
            // endpoint already decorated
            return endpoint;
        } else if (endpoint instanceof MockEndpoint) {
            // we should not intercept mock endpoints
            return endpoint;
        } else if (matchPattern(uri, endpoint, pattern)) {
            // if pattern is null then it mean to match all

            // only proxy if the uri is matched decorate endpoint with our proxy
            // should be false by default
            InterceptSendToEndpoint proxy = new InterceptSendToEndpoint(endpoint, skip);

            // create mock endpoint which we will use as interceptor
            // replace :// from scheme to make it easy to lookup the mock endpoint without having double :// in uri
            String key = "mock:" + endpoint.getEndpointKey().replaceFirst("://", ":");
            // strip off parameters as well
            if (key.contains("?")) {
                key = ObjectHelper.before(key, "?");
            }
            LOG.info("Adviced endpoint [" + uri + "] with mock endpoint [" + key + "]");

            MockEndpoint mock = endpoint.getCamelContext().getEndpoint(key, MockEndpoint.class);
            Producer producer;
            try {
                producer = mock.createProducer();
            } catch (Exception e) {
                throw wrapRuntimeCamelException(e);
            }

            // allow custom logic
            producer = onInterceptEndpoint(uri, endpoint, mock, producer);
            proxy.setDetour(producer);

            return proxy;
        } else {
            // no proxy so return regular endpoint
            return endpoint;
        }
    }

    /**
     * Does the pattern match the endpoint?
     *
     * @param uri      the uri
     * @param endpoint the endpoint
     * @param pattern  the pattern
     * @return <tt>true</tt> to match and therefore intercept, <tt>false</tt> if not matched and should not intercept
     */
    protected boolean matchPattern(String uri, Endpoint endpoint, String pattern) {
        return uri == null || pattern == null || EndpointHelper.matchEndpoint(endpoint.getCamelContext(), uri, pattern);
    }

    /**
     * Callback when an endpoint was intercepted with the given mock endpoint
     *
     * @param uri          the uri
     * @param endpoint     the endpoint
     * @param mockEndpoint the mocked endpoint
     * @param mockProducer the mock producer
     * @return the mock producer
     */
    protected Producer onInterceptEndpoint(String uri, Endpoint endpoint, MockEndpoint mockEndpoint, Producer mockProducer) {
        CamelContext context = mockEndpoint.getCamelContext();

        // the mock endpoint should be registered in JMX
        try {
            Object me = context.getManagementStrategy().getManagementObjectStrategy().getManagedObjectForEndpoint(context, mockEndpoint);
            if (me != null && !context.getManagementStrategy().isManaged(me, null)) {
                LOG.info("Registering mock endpoint in JMX: {}", mockEndpoint);
                context.getManagementStrategy().manageObject(me);
            }
        } catch (Exception e) {
            LOG.warn("Error registering mock endpoint in JMX: " + mockEndpoint + " due " + e.getMessage() + ". This exception is ignored.");
        }

        return mockProducer;
    }

    @Override
    public String toString() {
        return "CamelAutoInterceptSendToEndpointStrategy";
    }

}
