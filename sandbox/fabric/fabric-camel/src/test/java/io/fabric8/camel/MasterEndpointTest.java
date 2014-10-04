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
package io.fabric8.camel;

import java.util.List;

import junit.framework.Assert;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.util.ServiceHelper;
import io.fabric8.zookeeper.spring.CuratorFactoryBean;
import io.fabric8.zookeeper.spring.ZKServerFactoryBean;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    protected CuratorFactoryBean zkClientBean;

    @Before
    public void startService() throws Exception {
        ServiceHelper.startService(camelContext);
        ServiceHelper.startService(template);
    }

    @After
    public void afterRun() throws Exception {
        lastServerBean = zkServerBean;
        lastClientBean = zkClientBean;
        ServiceHelper.stopServices(camelContext);
    }

    protected static ZKServerFactoryBean lastServerBean;

    protected static CuratorFactoryBean lastClientBean;

    @AfterClass
    static public void shutDownZK() throws Exception {
        lastClientBean.destroy();
        lastServerBean.destroy();
    }

    @Test
    public void  testEndpoint() throws Exception {
        // check the endpoint configuration
        List<Route> registeredRoutes = camelContext.getRoutes();
        Assert.assertEquals("number of routes", 1, registeredRoutes.size());
        MasterEndpoint endpoint = (MasterEndpoint) registeredRoutes.get(0).getEndpoint();
        Assert.assertEquals("wrong endpoint uri", "seda:bar", endpoint.getChild());

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
