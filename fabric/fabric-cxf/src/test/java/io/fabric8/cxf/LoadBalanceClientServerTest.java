/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.cxf;

import org.apache.cxf.Bus;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@ContextConfiguration(locations = {"LoadBalanceContext.xml"})
public class LoadBalanceClientServerTest extends AbstractJUnit4SpringContextTests {
    @Autowired
    protected Bus bus;
    @Autowired
    protected FabricLoadBalancerFeature feature;
    @Autowired
    protected Hello helloProxy;


    @After
    public void shutdown() throws Exception {
        if (applicationContext instanceof DisposableBean) {
            ((DisposableBean) applicationContext).destroy();
        }
    }

    @Test
    public void testClientServer() throws Exception {
        assertNotNull(bus);
        // The bus is load the feature
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setServiceBean(new HelloImpl());
        factory.setAddress("http://localhost:9000/simple/server");
        factory.setBus(bus);
        factory.create();

        // sleep a while to let the service be published
        for (int i = 0; i < 100; i++) {
            if (!feature.getLoadBalanceStrategy().getAlternateAddressList().isEmpty()) {
                break;
            }
            Thread.sleep(100);
        }

        JaxWsProxyFactoryBean clientFactory = new JaxWsProxyFactoryBean();
        clientFactory.setServiceClass(Hello.class);
        // The address is not the actual address that the client will access
        clientFactory.setAddress("http://someotherplace");

        List<AbstractFeature> features = new ArrayList<AbstractFeature>();
        features.add(feature);
        // we need to setup the feature on the clientfactory
        clientFactory.setFeatures(features);
        Hello hello = clientFactory.create(Hello.class);
        String response = hello.sayHello();
        assertEquals("Get a wrong response", "Hello", response);

        response = hello.sayHello();
        assertEquals("Get a wrong response", "Hello", response);


        // Try to call the hello proxy which is created by Spring with setting feature on the bus
        response = helloProxy.sayHello();
        assertEquals("Get a wrong response", "Hello", response);

    }

}
