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
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.fusesource.fabric.zookeeper.spring.ZKServerFactoryBean;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@ContextConfiguration
public class MasterEndpointTest extends AbstractJUnit4SpringContextTests {

    @Autowired
    protected CamelContext camelContext;

    @EndpointInject(uri = "mock:results")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "seda:bar")
    protected ProducerTemplate template;

    // Yeah this sucks.. why does the spring context not get shutdown
    // after each test case?  Not sure!
    @Autowired
    protected ZKServerFactoryBean zkServerBean;
    @After
    public void afterRun() throws Exception {
        lastServerBean = zkServerBean;
    }
    protected static ZKServerFactoryBean lastServerBean;
    @AfterClass
    static public void shutDownZK() throws Exception {
        lastServerBean.destroy();
    }

    @Test
    public void  testEndpoint() throws Exception {
        System.out.println("===== starting test of Master endpoint!");

        String expectedBody = "<matched/>";

        resultEndpoint.expectedBodiesReceived(expectedBody);

        // lets wait for the entry to be registered...
        Thread.sleep(5000);

        template.sendBodyAndHeader(expectedBody, "foo", "bar");

        MockEndpoint.assertIsSatisfied(camelContext);

        System.out.println("===== completed test of Master endpoint!");
    }
}
