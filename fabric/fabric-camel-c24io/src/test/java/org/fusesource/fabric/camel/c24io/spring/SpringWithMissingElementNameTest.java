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
package io.fabric8.camel.c24io.spring;

import java.util.List;

import biz.c24.io.gettingstarted.transaction.Transactions;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit38.AbstractJUnit38SpringContextTests;


/**
 * @version $Revision$
 */
@ContextConfiguration
public class SpringWithMissingElementNameTest extends AbstractJUnit38SpringContextTests {
    @EndpointInject(uri = "mock:result")
    MockEndpoint resultEndpoint;

    @Test
    public void testBadElementName() throws Exception {
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.assertIsSatisfied();
        List<Exchange> list = resultEndpoint.getExchanges();
        assertEquals("list size", 1, list.size());
        Exchange exchange = list.get(0);
        Object body = exchange.getIn().getBody();
        assertTrue("The body should be instance of Transactions", body instanceof Transactions);
        Transactions document = (Transactions) body;
        System.out.println("Found: " + document);
    }
}