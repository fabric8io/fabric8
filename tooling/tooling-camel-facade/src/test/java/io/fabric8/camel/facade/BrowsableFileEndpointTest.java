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

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.test.junit4.CamelTestSupport;
import io.fabric8.camel.facade.mbean.CamelBrowsableEndpointMBean;
import io.fabric8.camel.facade.mbean.CamelEndpointMBean;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class BrowsableFileEndpointTest extends CamelTestSupport {

    private LocalCamelFacade local;
    private String name = "myCamel";

    @Before
    public void setUp() throws Exception {
        // make SEDA testing faster
        System.setProperty("CamelSedaPollTimeout", "10");
        super.setUp();
        local = new LocalCamelFacade(context);
    }

    @Override
    protected boolean useJmx() {
        return true;
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        deleteDirectory("target/inbox");
        deleteDirectory("target/outbox");

        DefaultCamelContext answer = new DefaultCamelContext();
        answer.setName(name);
        return answer;
    }

    @Test
    public void testBrowsableEndpoints() throws Exception {
        template.sendBodyAndHeader("file:target/inbox", "Hello World", Exchange.FILE_NAME, "hello.txt");
        // give time to process file
        Thread.sleep(2000);

        List<CamelEndpointMBean> endpoints = local.getEndpoints("myCamel");

        for (CamelEndpointMBean endpoint : endpoints) {
            if (endpoint instanceof CamelBrowsableEndpointMBean) {
                log.info("Browsable endpoint {}", endpoint);
            } else {
                log.info("Regular endpoint {}", endpoint);
            }
        }

        // get file:outbox endpoint
        CamelBrowsableEndpointMBean browsable = null;
        for (CamelEndpointMBean endpoint : endpoints) {
            if (endpoint.getEndpointUri().endsWith("file://target/outbox")) {
                browsable = (CamelBrowsableEndpointMBean) endpoint;
                break;
            }
        }

        assertNotNull("Should find browsable", browsable);

        assertEquals(1, browsable.queueSize());
        assertEquals("Hello World", browsable.browseMessageBody(0));
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("file:target/inbox?delete=true").routeId("in-route")
                        .to("file:target/outbox").id("out");
            }
        };
    }

}
