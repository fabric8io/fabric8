/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel.c24io.validation;

import java.io.IOException;
import java.io.InputStream;

import biz.c24.io.api.presentation.Source;
import biz.c24.io.api.presentation.XMLSource;
import iso.std.iso.x20022.tech.xsd.pacs.x008.x001.x01.Document;
import iso.std.iso.x20022.tech.xsd.pacs.x008.x001.x01.DocumentElement;
import org.apache.camel.test.CamelTestSupport;
import org.apache.camel.Processor;
import org.apache.camel.ValidationException;
import org.fusesource.fabric.camel.c24io.C24IOValidator;
import org.apache.camel.builder.Builder;
import org.apache.camel.builder.ProcessorBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.processor.MyValidator;

/**
 * @version $Revision$
 */
public class ValidationTest extends CamelTestSupport {
    protected Processor validator = new MyValidator();
    protected MockEndpoint validEndpoint;
    protected MockEndpoint invalidEndpoint;

    public void testValidMessage() throws Exception {
        validEndpoint.expectedMessageCount(1);
        invalidEndpoint.expectedMessageCount(0);

        Document body = createValidBody();
        Object result = template.requestBody("direct:start", body);

        MockEndpoint.assertIsSatisfied(validEndpoint, invalidEndpoint);
        assertEquals("validResult", result);
    }

    public void testInvalidMessage() throws Exception {
        invalidEndpoint.expectedMessageCount(1);
        validEndpoint.expectedMessageCount(0);

        Document body = new Document();
        Object result = template.requestBody("direct:start", body);

        MockEndpoint.assertIsSatisfied(validEndpoint, invalidEndpoint);
        assertEquals("invalidResult", result);
    }

    public void testinvalidThenValidMessage() throws Exception {
        validEndpoint.expectedMessageCount(2);
        invalidEndpoint.expectedMessageCount(1);

        Object result;

        Document body = new Document();
        result = template.requestBody("direct:start", body);
        assertEquals("invalidResult", result);

        body = createValidBody();
        result = template.requestBody("direct:start", body);
        assertEquals("validResult", result);

        body = createValidBody();
        result = template.requestBody("direct:start", body);
        assertEquals("validResult", result);

        MockEndpoint.assertIsSatisfied(validEndpoint, invalidEndpoint);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        validEndpoint = getMockEndpoint("mock:valid");
        invalidEndpoint = getMockEndpoint("mock:invalid");

        validEndpoint.whenAnyExchangeReceived(ProcessorBuilder.setOutBody(Builder.constant("validResult")));
        invalidEndpoint.whenAnyExchangeReceived(ProcessorBuilder.setOutBody(Builder.constant("invalidResult")));
    }

    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            public void configure() {
                from("direct:start").
                        doTry().
                            process(new C24IOValidator()).
                            to("mock:valid").
                        doCatch(ValidationException.class).
                            to("mock:invalid");
            }
        };
    }

    protected Document createValidBody() throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream("org/fusesource/fabric/camel/c24io/validation/pacs.008.001.01-valid.xml");
        assertNotNull("Should have found valid XML!", in);

        Source src = new XMLSource(in);
        return (Document) src.readObject(DocumentElement.getInstance());
    }
}