/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.cxf;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@ContextConfiguration(locations = {"FailOverContext.xml"})
public class FailOverClientServerTest extends AbstractJUnit4SpringContextTests {
    @Autowired
    protected Bus bus;
    @Autowired
    protected FabricLoadBalancerFeature feature;

    @Test
    public void testClientServer() throws Exception {
        assertNotNull(bus);
        // The bus is load the feature
        ServerFactoryBean factory = new ServerFactoryBean();
        factory.setServiceBean(new HelloImplFail());
        factory.setAddress("http://localhost:9000/fail/server");
        factory.setBus(bus);
        factory.create();


        factory = new ServerFactoryBean();
        factory.setServiceBean(new HelloImpl());
        factory.setAddress("http://localhost:9000/simple/server");
        factory.setBus(bus);
        factory.create();

        // sleep a while to let the service be published
        ClientProxyFactoryBean clientFactory = new ClientProxyFactoryBean();
        clientFactory.setServiceClass(Hello.class);
        // The address is not the actual address that the client will access
        clientFactory.setAddress("http://someotherplace");
        clientFactory.setBus(bus);
        List<AbstractFeature> features = new ArrayList<AbstractFeature>();
        features.add(feature);
        // we need to setup the feature on the clientfactory
        clientFactory.setFeatures(features);
        // set this interceptor to simulate the Transport level exception
        List<Interceptor<? extends Message>> outInterceptor = new ArrayList<Interceptor<? extends Message>>();
        outInterceptor.add(new TransportFailureInterceptor());
        clientFactory.setOutInterceptors(outInterceptor);

        Hello hello = clientFactory.create(Hello.class);
        String response = hello.sayHello();
        System.out.println("Get the response here " + response);
        assertEquals("Get a wrong response", "Hello", response);


    }

}
