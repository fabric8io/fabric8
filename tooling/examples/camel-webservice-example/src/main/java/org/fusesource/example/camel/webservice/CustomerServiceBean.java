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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.camel.Body;
import org.fusesource.example.Customer;
import org.fusesource.example.CustomerType;
import org.fusesource.example.GetAllCustomersResponse;
import org.fusesource.example.GetCustomerByName;
import org.fusesource.example.GetCustomerByNameResponse;
import org.fusesource.example.SaveCustomer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomerServiceBean {

    private static Logger log = LoggerFactory.getLogger(CustomerServiceBean.class);
    static List<Customer> customers = new ArrayList<Customer>();
    Random randomGenerator = new Random();

    @SuppressWarnings("unused")
    private void generateCustomer() {
        Customer customer = new Customer();
        customer.setName("Fuse");
        customer.setNumOrders(randomGenerator.nextInt(100));
        customer.setRevenue(randomGenerator.nextInt(10000));
        customer.setType(CustomerType.BUSINESS);
        customer.setTest(BigDecimal.valueOf(100.00));
        customer.getAddress().add("FuseSource Office");
        customers.add(customer);
    }

    public SaveCustomer createCustomer(@Body String name) {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setNumOrders(randomGenerator.nextInt(100));
        customer.setRevenue(randomGenerator.nextInt(10000));
        customer.setType(CustomerType.BUSINESS);
        customer.setTest(BigDecimal.valueOf(100.00));
        customer.getAddress().add("FuseSource Office");

        SaveCustomer result = new SaveCustomer();
        result.setCustomer(customer);
        return result;
    }

    public GetAllCustomersResponse getCustomers() {

        GetAllCustomersResponse response = new GetAllCustomersResponse();
        response.getReturn().addAll(customers);
        return response;
    }

    public GetCustomerByNameResponse getCustomerByName(@Body GetCustomerByName cSearched) {

        List<Customer> result = new ArrayList<Customer>();
        // Search for Customer using name as key
        for(Customer c : customers) {
            if (c.getName().equals(cSearched.getName())) {
               result.add(c);
               log.info(">> Customer find !");
               break;
            }
        }

        GetCustomerByNameResponse response = new GetCustomerByNameResponse();
        response.getReturn().addAll(result);

        return response;

    }

    public Customer saveCustomer(@Body SaveCustomer c) {

        String address = (c.getCustomer().getAddress().get(0) != null) ?  c.getCustomer().getAddress().get(0) : "Unknown address";
        XMLGregorianCalendar birthDate = c.getCustomer().getBirthDate();

        // enrich the customer received from backend data
        Customer customer = new Customer();
        customer.setName(c.getCustomer().getName());
        customer.getAddress().add(address);
        customer.setBirthDate(birthDate);
        customer.setNumOrders(randomGenerator.nextInt(100));
        customer.setRevenue(randomGenerator.nextInt(10000));
        customer.setType(CustomerType.PRIVATE);
        customer.setTest(BigDecimal.valueOf(100.00));
        customers.add(customer);

        log.info(">> Customer created and added in the array.");

        return customer;
    }



}
