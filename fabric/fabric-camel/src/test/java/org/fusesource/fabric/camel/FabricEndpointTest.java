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

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.fusesource.fabric.zookeeper.spring.ZKServerFactoryBean;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@ContextConfiguration
@Ignore("[FABRIC-528] Fix fabric/fabric-camel tests")
public class FabricEndpointTest extends AbstractJUnit4SpringContextTests {
    @Autowired
    protected CamelContext camelContext;

    @EndpointInject(uri = "mock:results")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
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
    public void testEndpoint() throws Exception {
        System.out.println("===== starting test of Camel Fabric!");

        String expectedBody = "<matched/>";

        resultEndpoint.expectedBodiesReceived(expectedBody);

        // lets wait for the entry to be registered...
        Thread.sleep(5000);

        template.sendBodyAndHeader(expectedBody, "foo", "bar");

        MockEndpoint.assertIsSatisfied(camelContext);

        System.out.println("  ==== testing NoExitFabricEndpoint !");
        try {
            template.sendBody("fabric:noExist", "Test");
            Assert.fail("Expect exception here");
        } catch (Exception ex) {
            Assert.assertTrue("Get a wrong exception. ", ex.getCause() instanceof IllegalStateException);
        }

        System.out.println("===== completed test of Camel Fabric!");
    }

}
