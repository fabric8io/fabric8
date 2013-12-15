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
package io.fabric8.jaxb.dynamic.camel;

import java.util.List;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import io.fabric8.jaxb.dynamic.DynamicJaxbDataFormat;
import io.fabric8.jaxb.dynamic.DefaultDynamicCompiler;
import io.fabric8.jaxb.dynamic.XjcTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lets use the Dynamic JAXB and Camel together.
 *
 * We'll process some XML files by loading some XSDs at runtime and then using those to define a
 * JaxbDataFormat
 */
public class CamelXjcTest extends CamelTestSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(CamelXjcTest.class);

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    protected DynamicJaxbDataFormat dataFormat = new DynamicJaxbDataFormat();

    public void setUp() throws Exception {

        // this would typically be done by dependency injection...
        dataFormat.setCompiler(new DefaultDynamicCompiler(XjcTest.getSchemaURL("xsds/report.xsd")));

        super.setUp();
    }

    @Test
    public void testSendMatchingMessage() throws Exception {
        resultEndpoint.expectedMessageCount(1);

        resultEndpoint.assertIsSatisfied();
        List<Exchange> exchanges = resultEndpoint.getExchanges();
        Exchange exchange = exchanges.get(0);
        Object body = exchange.getIn().getBody();
        assertNotNull("Should have received a body", body);
        LOG.info("Received body " + body);
        assertEquals("body class name", "com.foo.report.Report", body.getClass().getName());
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            public void configure() {
                from("file:src/test/data?noop=true").
                        unmarshal(dataFormat).
                        filter().simple("${body.author[0].name} == 'Knuth'").
                        to("mock:result");
            }
        };
    }
}