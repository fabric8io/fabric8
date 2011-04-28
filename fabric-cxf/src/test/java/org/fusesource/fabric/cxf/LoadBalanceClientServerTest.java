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
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;

@ContextConfiguration(locations = {"LoadBalanceContext.xml"})
public class LoadBalanceClientServerTest extends AbstractJUnit4SpringContextTests {
    @Autowired
    protected Bus bus;
    @Autowired
    protected FabricLoadBalancerFeature feature;

    @Test
    public void testClientServer() throws Exception {
        assertNotNull(bus);
        ServerFactoryBean factory = new ServerFactoryBean();
        factory.setServiceBean(new HelloImpl());
        factory.setAddress("http://localhost:9000/simple/server");
        factory.setBus(bus);
        factory.create();

        // sleep a while to let the service be published
        ClientProxyFactoryBean clientFactory = new ClientProxyFactoryBean();
        clientFactory.setServiceClass(Hello.class);
        clientFactory.setAddress("http://someotherplace");
        clientFactory.setBus(bus);
        List<AbstractFeature> features = new ArrayList<AbstractFeature>();
        features.add(feature);
        // we should setup the feature on the client server
        clientFactory.setFeatures(features);
        Hello hello = clientFactory.create(Hello.class);
        String response = hello.sayHello();
        System.out.println(response);
    }

}
