/*
 * Copyright 2010 Red Hat, Inc.
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
package io.fabric8.camel.facade;

import java.util.List;

import org.apache.activemq.spring.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsQueueEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.test.junit4.CamelTestSupport;
import io.fabric8.camel.facade.mbean.CamelBrowsableEndpointMBean;
import io.fabric8.camel.facade.mbean.CamelEndpointMBean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.apache.camel.component.jms.JmsComponent.jmsComponentAutoAcknowledge;

public class ActiveMQLocalCamelFacadeTest extends CamelTestSupport {

    private LocalCamelFacade local;
    private String name = "myCamel";

    @Before
    public void setUp() throws Exception {
        deleteDirectory("activemq-data");
        super.setUp();
        local = new LocalCamelFacade(context);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        deleteDirectory("activemq-data");
    }

    @Override
    protected boolean useJmx() {
        return true;
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        DefaultCamelContext answer = new DefaultCamelContext();
        answer.setName(name);

        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
        connectionFactory.setBrokerURL("vm://test-broker?broker.persistent=true&broker.useJmx=true");
        answer.addComponent("activemq", jmsComponentAutoAcknowledge(connectionFactory));

        return answer;
    }

    @Test
    public void testBrowsableEndpointsWithJMX() throws Exception {
        template.sendBody("direct:start", "Hello World");
        Thread.sleep(1000);

        List<CamelEndpointMBean> endpoints = local.getEndpoints("myCamel");

        CamelBrowsableEndpointMBean browsable = null;
        for (CamelEndpointMBean endpoint : endpoints) {
            if (endpoint.getEndpointUri().endsWith("activemq://queue:out")) {
                browsable = (CamelBrowsableEndpointMBean) endpoint;
                break;
            }
        }

        assertNotNull("Should find browsable", browsable);

        assertEquals(1, browsable.queueSize());
        assertNotNull(browsable.browseExchange(0));
        assertEquals("Hello World", browsable.browseMessageBody(0));
        String xml = browsable.browseMessageAsXml(0, true);
        log.info(xml);
        assertTrue(xml, xml.endsWith("<body type=\"java.lang.String\">Hello World</body>\n</message>"));
    }

    @Test
    public void testBrowsableEndpointsNoJmx() throws Exception {
        template.sendBody("direct:start", "Hello World");
        Thread.sleep(1000);

        JmsQueueEndpoint browsable = context.getEndpoint("activemq:queue:out", JmsQueueEndpoint.class);
        assertNotNull(browsable);

        assertEquals(1, browsable.queueSize());
        assertNotNull(browsable.browseExchange(0));
        assertEquals("Hello World", browsable.browseMessageBody(0));
        String xml = browsable.browseMessageAsXml(0, true);
        log.info(xml);
        assertTrue(xml, xml.endsWith("<body type=\"java.lang.String\">Hello World</body>\n</message>"));
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").routeId("in-route")
                        .to("log:in").id("toLog")
                        .to("activemq:queue:out").id("toOut");
            }
        };
    }

}
