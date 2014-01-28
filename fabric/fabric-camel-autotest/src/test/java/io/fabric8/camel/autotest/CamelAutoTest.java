/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.fabric8.camel.autotest;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.test.junit4.TestSupport;
import org.junit.Test;

public class CamelAutoTest extends TestSupport {

    private CamelContext context;

    @Test
    public void testCamelAutoTest() throws Exception {
        context = new DefaultCamelContext();
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("seda:start").routeId("foo")
                    .to("log:input")
                    .to("seda:a")
                    .to("seda:b");
            }
        });

        context.start();

        MockEndpoint input = context.getEndpoint("mock://log:input", MockEndpoint.class);
        MockEndpoint a = context.getEndpoint("mock://seda:a", MockEndpoint.class);
        MockEndpoint b = context.getEndpoint("mock://seda:b", MockEndpoint.class);

        input.expectedBodiesReceived("Hello Camel");
        a.expectedBodiesReceived("Hello Camel");
        b.expectedBodiesReceived("Hello Camel");

        // restart routes
        CamelAutoInterceptSendToEndpointStrategy strategy = new CamelAutoInterceptSendToEndpointStrategy();
        context.addRegisterEndpointCallback(strategy);

        // restart routes
        context.stopRoute("foo");
        context.startRoute("foo");

        ProducerTemplate template = context.createProducerTemplate();
        template.sendBody("seda:start", "Hello Camel");

        input.assertIsSatisfied();
        a.assertIsSatisfied();
        b.assertIsSatisfied();

        context.stop();
    }

}
