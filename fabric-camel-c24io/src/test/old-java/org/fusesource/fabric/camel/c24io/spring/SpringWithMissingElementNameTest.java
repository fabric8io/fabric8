/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel.c24io.spring;
import java.util.List;

import iso.std.iso.x20022.tech.xsd.pacs.x008.x001.x01.Document;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit38.AbstractJUnit38SpringContextTests;


/**
 * @version $Revision$
 */
@ContextConfiguration
public class SpringWithMissingElementNameTest extends AbstractJUnit38SpringContextTests {
    @EndpointInject(uri = "mock:result")
    MockEndpoint resultEndpoint;

    public void testBadElementName() throws Exception {
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.assertIsSatisfied();
        List<Exchange> list = resultEndpoint.getExchanges();
        assertEquals("list size", 1, list.size());
        Exchange exchange = list.get(0);
        Object body = exchange.getIn().getBody();
        assertTrue("The body should be instance of Document", body instanceof Document);
        Document document = (Document) body;
        System.out.println("Found: " + document);
    }
}