/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.ServiceSupport;
import org.apache.camel.processor.loadbalancer.LoadBalancer;
import org.apache.camel.util.ServiceHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 */
public class EndpointLocator extends ServiceSupport {
    private static final transient Log LOG = LogFactory.getLog(EndpointLocator.class);

    private final List<String> uris;
    private LoadBalancer loadBalancer;

    public EndpointLocator(List<String> uris) {
        this.uris = uris;
    }

    public void process(Exchange exchange, FabricEndpoint endpoint) throws Exception {
        if (loadBalancer == null) {
            loadBalancer = endpoint.createLoadBalancer(uris);

            LOG.debug("Created " + loadBalancer + " for endpoint " + endpoint + " to physical URIs " + uris);
            // lets add the endpoints to the load balancer
            for (String uri : uris) {
                loadBalancer.addProcessor(endpoint.getProcessor(uri));
            }
        }
        loadBalancer.process(exchange);
    }

    @Override
    protected void doStart() throws Exception {
    }

    @Override
    protected void doStop() throws Exception {
        ServiceHelper.stopService(loadBalancer);
    }
}
