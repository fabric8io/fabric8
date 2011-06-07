/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel.c24io;

import java.util.List;

import biz.c24.io.api.data.ComplexDataObject;
import org.apache.camel.test.CamelTestSupport;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;

/**
 * @version $Revision$
 */
public class NoSourceTest extends CamelTestSupport {
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