/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.ServiceHelper;
import org.fusesource.fabric.zookeeper.spring.ZKServerFactoryBean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class MasterEndpointFailoverTest {
    private static final transient Logger LOG = LoggerFactory.getLogger(MasterEndpointFailoverTest.class);

    protected ProducerTemplate template;
    protected CamelContext producerContext;
    protected CamelContext consumerContext1;
    protected CamelContext consumerContext2;
    protected MockEndpoint result1Endpoint;
    protected MockEndpoint result2Endpoint;
    protected AtomicInteger messageCounter = new AtomicInteger(1);
    protected ZKServerFactoryBean serverFactoryBean = new ZKServerFactoryBean();

    @Before
    public void beforeRun() throws Exception {
        System.out.println("Starting ZK server!");
        serverFactoryBean.setPurge(true);
        serverFactoryBean.afterPropertiesSet();
        
        System.setProperty("zookeeper.url", "0.0.0.0:2181");

        producerContext = new DefaultCamelContext();
        template = producerContext.createProducerTemplate();

        consumerContext1 = new DefaultCamelContext();
        consumerContext1.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("master:MasterEndpointFailoverTest:vm:start").to("mock:result1");
            }
        });
        consumerContext2 = new DefaultCamelContext();
        consumerContext2.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("master:MasterEndpointFailoverTest:vm:start").to("mock:result2");
            }
        });

        ServiceHelper.startServices(producerContext);

        result1Endpoint = consumerContext1.getEndpoint("mock:result1", MockEndpoint.class);
        result2Endpoint = consumerContext2.getEndpoint("mock:result2", MockEndpoint.class);
    }

    @After
    public void afterRun() throws Exception {
        ServiceHelper.stopServices(consumerContext1);
        ServiceHelper.stopServices(producerContext);

        serverFactoryBean.destroy();
    }

    @Test
    public void testEndpoint() throws Exception {
        System.out.println("Starting consumerContext1");

        ServiceHelper.startServices(consumerContext1);
        assertMessageReceived(result1Endpoint, result2Endpoint);

        System.out.println("Starting consumerContext2");
        ServiceHelper.startServices(consumerContext2);
        assertMessageReceivedLoop(result1Endpoint, result2Endpoint, 3);

        System.out.println("Stopping consumerContext1");
        ServiceHelper.stopService(consumerContext1);
        assertMessageReceivedLoop(result2Endpoint, result1Endpoint, 3);
    }

    protected void assertMessageReceivedLoop(MockEndpoint masterEndpoint, MockEndpoint standbyEndpoint, int count) throws Exception {
        for (int i = 0; i < count; i++) {
            Thread.sleep(1000);
            assertMessageReceived(masterEndpoint, standbyEndpoint);
        }
    }

    protected void assertMessageReceived(MockEndpoint masterEndpoint, MockEndpoint standbyEndpoint) throws InterruptedException {
        masterEndpoint.reset();
        standbyEndpoint.reset();

        String expectedBody = createNextExpectedBody();
        masterEndpoint.expectedBodiesReceived(expectedBody);
        standbyEndpoint.expectedMessageCount(0);

        template.sendBody("vm:start", expectedBody);

        LOG.info("Expecting master: " + masterEndpoint + " and standby: " + standbyEndpoint);
        MockEndpoint.assertIsSatisfied(masterEndpoint, standbyEndpoint);
    }

    protected String createNextExpectedBody() {
        return "body:" + messageCounter.incrementAndGet();
    }
}
