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
import biz.c24.testtransactions.Transactions;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.dataformat.C24IOContentType;
import org.apache.camel.test.CamelTestSupport;

/**
 * @version $Revision$
 */
public class XPathTest extends CamelTestSupport {
    public void testParsingMessage() throws Exception {
        MockEndpoint resultEndpoint = resolveMandatoryEndpoint("mock:result", MockEndpoint.class);
        resultEndpoint.expectedMessageCount(1);

        resultEndpoint.assertIsSatisfied();

        List<Exchange> list = resultEndpoint.getReceivedExchanges();
        Exchange exchange = list.get(0);
        Message in = exchange.getIn();
        log.info("Headers: " + in.getHeaders());
        ComplexDataObject object = assertIsInstanceOf(ComplexDataObject.class, in.getBody());
        log.info("Received: " + object);
    }

    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            public void configure() {

                Namespaces ns = new Namespaces("foo", "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.01");

                from("file:src/test/data?noop=true").
                        unmarshal().c24io(Transactions.class, C24IOContentType.Xml).
                        //setHeader("foo", ns.xquery("//foo:MsgId", String.class)).
                        filter(ns.xquery("//foo:MsgId = 'PFSM1234'")).
                        to("mock:result");
            }
        };
    }
}