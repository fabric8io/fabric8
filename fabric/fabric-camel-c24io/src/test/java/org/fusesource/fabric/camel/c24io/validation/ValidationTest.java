/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.camel.c24io.validation;

import biz.c24.io.api.presentation.Source;
import biz.c24.io.api.presentation.TextualSource;
import biz.c24.testtransactions.Transactions;

import org.apache.camel.ValidationException;
import org.apache.camel.builder.Builder;
import org.apache.camel.builder.ProcessorBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.CamelTestSupport;

import org.fusesource.fabric.camel.c24io.C24IOValidator;
import org.fusesource.fabric.camel.c24io.SampleDataFiles;

import java.io.IOException;
import java.io.InputStream;

/**
 * @version $Revision$
 */
public class ValidationTest extends CamelTestSupport {
    protected MockEndpoint validEndpoint;
    protected MockEndpoint invalidEndpoint;

    public void testValidMessage() throws Exception {
        validEndpoint.expectedMessageCount(1);
        invalidEndpoint.expectedMessageCount(0);

        Transactions body = createValidBody();
        Object result = template.requestBody("direct:start", body);

        MockEndpoint.assertIsSatisfied(validEndpoint, invalidEndpoint);
        assertEquals("validResult", result);
    }

    public void testInvalidMessage() throws Exception {
        invalidEndpoint.expectedMessageCount(1);
        validEndpoint.expectedMessageCount(0);

        Transactions body = new Transactions();
        Object result = template.requestBody("direct:start", body);

        MockEndpoint.assertIsSatisfied(validEndpoint, invalidEndpoint);
        assertEquals("invalidResult", result);
    }

    public void testinvalidThenValidMessage() throws Exception {
        validEndpoint.expectedMessageCount(2);
        invalidEndpoint.expectedMessageCount(1);

        Object result;

        Transactions body = new Transactions();
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

    protected Transactions createValidBody() throws IOException {
        InputStream in = SampleDataFiles.sampleTransactionsFile();
        Source src = new TextualSource(in);

        // TODO how to do this now?
        //return (Transactions) src.readObject(Transactions.getInstance());
        Transactions instance = new Transactions();
        src.readObject(instance);
        return instance;
    }
}