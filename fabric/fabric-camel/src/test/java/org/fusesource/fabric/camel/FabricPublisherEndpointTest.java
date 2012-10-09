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
package org.fusesource.fabric.camel;

import java.util.List;

import junit.framework.Assert;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.ShutdownRoute;
import org.apache.camel.ShutdownRunningTask;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.SpringRouteBuilder;
import org.fusesource.fabric.zookeeper.spring.ZKServerFactoryBean;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@ContextConfiguration
public class FabricPublisherEndpointTest extends AbstractJUnit4SpringContextTests {
    private static final String ROUTE_NAME = "test_route_1";

	@Autowired
    protected CamelContext camelContext;

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
    public void testPublishEndpointWithOption() throws Exception {
      
    	
    	SpringRouteBuilder route = new SpringRouteBuilder() {
    		@Override
             public void configure() throws Exception {   
                from("fabric:cheese:seda:bar?size=10").routeId(ROUTE_NAME).to("log:mylog"); 
             }
         };

         camelContext.addRoutes(route);
         List<Route> registeredRoutes = camelContext.getRoutes();
         Assert.assertEquals("number of routes",1, registeredRoutes.size());
         Assert.assertEquals("route name", ROUTE_NAME,registeredRoutes.get(0).getId()); 

    }
}
