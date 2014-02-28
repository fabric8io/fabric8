/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.example.camel.webservice;

import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.apache.cxf.message.MessageContentsList;
import org.fusesource.example.GetCustomerByName;
import org.fusesource.example.GetCustomerByNameResponse;
import org.junit.Test;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class CustomerWebServiceTest extends CamelSpringTestSupport {

    @Override
    protected AbstractXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("META-INF/spring/CamelContext.xml");
    }

    @Test
    public void testCustomerFuse() throws Exception {

        GetCustomerByName searchCustomer = new GetCustomerByName();
        searchCustomer.setName("Fuse");

        MessageContentsList reply = (MessageContentsList) template.requestBodyAndHeader("cxf:bean:WS", searchCustomer, "operationName", "getCustomerByName");
        GetCustomerByNameResponse customer = (GetCustomerByNameResponse)reply.get(0);
        assertEquals(customer.getReturn().get(0).getName(), "Fuse");

    }
}
