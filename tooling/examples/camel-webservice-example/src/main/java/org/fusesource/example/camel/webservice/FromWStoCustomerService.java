/*
 * Copyright 2012 Red Hat
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */


package org.fusesource.example.camel.webservice;

import org.apache.camel.builder.RouteBuilder;

public class FromWStoCustomerService extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("cxf:bean:WS").id("cxf-to-client-pojo")

            .choice()
                .when().simple("${in.header.SOAPAction} contains 'getCustomerByName'")
                    .log(">>> We will search a Customer")
                    .beanRef("customerServiceBean", "getCustomerByName")

                .when().simple("${in.header.SOAPAction} contains 'saveCustomer'")
                    .log(">>> We will save a Customer")
                    .beanRef("customerServiceBean", "saveCustomer")

                .when().simple("${in.header.SOAPAction} contains 'getAllCustomers'")
                    .log(">>> We will get all Customers")
                    .beanRef("customerServiceBean", "getCustomers")
            ;


    }
}