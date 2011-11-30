/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel;

import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.impl.ProducerCache;
import org.apache.camel.processor.loadbalancer.LoadBalancer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.fabric.groups.ChangeListener;
import org.fusesource.fabric.groups.Group;

import java.io.UnsupportedEncodingException;

/**
 * Creates an endpoint which uses FABRIC to map a logical name to physical endpoint names
 */
public class FabricLocatorEndpoint extends DefaultEndpoint {
    private static final transient Log LOG = LogFactory.getLog(FabricLocatorEndpoint.class);

    private final FabricComponent component;
    private final Group group;

    private LoadBalancerFactory loadBalancerFactory;
    private LoadBalancer loadBalancer;


    public FabricLocatorEndpoint(String uri, FabricComponent component, Group group) {
        super(uri, component);
        this.component = component;
        this.group = group;
    }

    @SuppressWarnings("unchecked")
    public Producer createProducer() throws Exception {
        final FabricLocatorEndpoint endpoint = this;
        return new DefaultProducer(endpoint) {
            public void process(Exchange exchange) throws Exception {
                loadBalancer.process(exchange);
            }
        };
    }

    public Consumer createConsumer(Processor processor) throws Exception {
        throw new UnsupportedOperationException("You cannot consume from a FABRIC endpoint using just its fabric name directly, you must use fabric:name:someActualUri instead");
    }

    public boolean isSingleton() {
        return true;
    }

    @Override
    public void start() throws Exception {
        super.start();
        if (loadBalancer == null) {
            loadBalancer = createLoadBalancer();
        }
        group.add(new ChangeListener(){
            public void changed() {
                // TODO - should we be clearing all the members here???
                for (byte[] uri : group.members().values()) {
                    try {
                        loadBalancer.addProcessor(getProcessor(new String(uri, "UTF-8")));
                    } catch (UnsupportedEncodingException ignore) {
                    }
                }
            }
            public void connected() {
                changed();
            }
            public void disconnected() {
                changed();
            }
        });
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        group.close();
    }

    public Processor getProcessor(String uri) {
        final Endpoint endpoint = getCamelContext().getEndpoint(uri);
        return new Processor() {

            public void process(Exchange exchange) throws Exception {
                ProducerCache producerCache = component.getProducerCache();
                Producer producer = producerCache.acquireProducer(endpoint);
                try {
                    producer.process(exchange);
                } finally {
                    producerCache.releaseProducer(endpoint, producer);
                }
            }

            @Override
            public String toString() {
                return "Producer for " + endpoint;
            }
        };
    }

    // Properties
    //-------------------------------------------------------------------------


    public FabricComponent getComponent() {
        return component;
    }

    public LoadBalancerFactory getLoadBalancerFactory() {
        if (loadBalancerFactory == null) {
            loadBalancerFactory = component.getLoadBalancerFactory();
        }
        return loadBalancerFactory;
    }

    public void setLoadBalancerFactory(LoadBalancerFactory loadBalancerFactory) {
        this.loadBalancerFactory = loadBalancerFactory;
    }

    public LoadBalancer createLoadBalancer() {
        return getLoadBalancerFactory().createLoadBalancer();
    }
}
