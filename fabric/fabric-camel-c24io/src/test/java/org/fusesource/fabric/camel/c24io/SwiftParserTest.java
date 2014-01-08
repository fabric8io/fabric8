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

package io.fabric8.camel.c24io;

import biz.c24.io.api.data.ComplexDataObject;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Ignore;

import java.util.List;

/**
 * Tests the use of the {@link SwiftFormat}
 */
public class SwiftParserTest extends CamelTestSupport {

    public void testParsingMessage() throws Exception {
        MockEndpoint resultEndpoint = getMockEndpoint("mock:result");
        MockEndpoint marshalledEndpoint = getMockEndpoint("mock:marshalled");

        resultEndpoint.expectedMessageCount(1);
        marshalledEndpoint.expectedMessageCount(1);

        assertMockEndpointsSatisfied();

        assertReceivedComplexDataObject(resultEndpoint);
        assertReceivedMarshalledMessage(marshalledEndpoint);
    }

    protected void assertReceivedComplexDataObject(MockEndpoint endpoint) {
        List<Exchange> list = endpoint.getReceivedExchanges();
        Exchange exchange = list.get(0);
        Message in = exchange.getIn();
        ComplexDataObject object = assertIsInstanceOf(ComplexDataObject.class, in.getBody());
        log.info("Received CDO: " + object);
    }

    protected void assertReceivedMarshalledMessage(MockEndpoint endpoint) {
        List<Exchange> list = endpoint.getReceivedExchanges();
        Exchange exchange = list.get(0);
        Message in = exchange.getIn();
        Object object = assertIsInstanceOf(byte[].class, in.getBody());
        log.info("Received binary blob: " + object);
    }

    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            public void configure() {

                from("file:src/test/swift?noop=true").
                        unmarshal(new SwiftFormat()).
                        to("mock:result", "direct:marshal");

                from("direct:marshal").marshal(new SwiftFormat()).to("mock:marshalled");
            }
        };
    }
}