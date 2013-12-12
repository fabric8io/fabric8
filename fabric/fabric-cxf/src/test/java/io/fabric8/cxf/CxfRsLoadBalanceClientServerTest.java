/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.cxf;

import java.util.ArrayList;
import java.util.List;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@ContextConfiguration(locations = {"LoadBalanceContext.xml"})
public class CxfRsLoadBalanceClientServerTest extends AbstractJUnit4SpringContextTests {
    @Autowired
    protected org.apache.cxf.Bus bus;
    @Autowired
    protected FabricLoadBalancerFeature feature;


    @After
    public void shutdown() throws Exception {
        if (applicationContext instanceof DisposableBean) {
            ((DisposableBean) applicationContext).destroy();
        }
    }

    @Test
    public void testClientServer() throws Exception {
        assertNotNull(bus);
        assertNotNull(feature);
        // Publish the services
        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        factory.setResourceClasses(CustomerService.class);
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

        // create the JAXRS client
        JAXRSClientFactoryBean clientFactory = new JAXRSClientFactoryBean();
        clientFactory.setServiceClass(CustomerServiceResources.class);
        clientFactory.setAddress("http://someotherplace");


        List<AbstractFeature> features = new ArrayList<AbstractFeature>();
        features.add(feature);
        // we need to setup the feature on the clientfactory
        clientFactory.setFeatures(features);
        CustomerServiceResources proxy = clientFactory.create(CustomerServiceResources.class);
        Customer response = proxy.getCustomer("123");
        assertEquals("Get a wrong customer name", "John", response.getName());


    }
}
