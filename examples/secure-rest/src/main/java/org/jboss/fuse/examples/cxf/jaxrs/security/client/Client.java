/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jboss.fuse.examples.cxf.jaxrs.security.client;

import java.io.File;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.auth.BasicScheme;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;

public final class Client {

    private Client() {
    }

    public static void main(String args[]) throws Exception {
        // Now we need to use the basic authentication to send the request
        HttpClient httpClient = new HttpClient();
        httpClient.getState().setCredentials(
                  AuthScope.ANY,
                  new UsernamePasswordCredentials("admin", "admin")
        );
        // Use basic authentication
        AuthScheme scheme = new BasicScheme();


        /**
         * HTTP GET http://localhost:8181/cxf/securecrm/customerservice/customers/123
         * returns the XML document representing customer 123
         *
         * On the server side, it matches the CustomerService's getCustomer() method
         */
        System.out.println("Sent HTTP GET request to query customer info with basic authentication info.");
        GetMethod get = new GetMethod("http://localhost:8181/cxf/securecrm/customerservice/customers/123");
        get.getHostAuthState().setAuthScheme(scheme);
        try {
            httpClient.executeMethod(get);
            System.out.println(get.getResponseBodyAsString());
        } finally {
            get.releaseConnection();
        }

        /**
         * HTTP GET http://localhost:8181/cxf/securecrm/customerservice/customers/123
         * without passing along authentication credentials - this will result in a security exception in the response.
         */
        System.out.println("\n");
        System.out.println("Sent HTTP GET request to query customer info without basic authentication info.");
        get = new GetMethod("http://localhost:8181/cxf/securecrm/customerservice/customers/123");
        try {
            httpClient.executeMethod(get);
            // we should get the security exception here
            System.out.println(get.getResponseBodyAsString());
        } finally {
            get.releaseConnection();
        }

        /**
         * HTTP GET http://localhost:8181/cxf/securecrm/customerservice/orders/223/products/323
         * returns the XML document representing product 323 in order 223
         *
         * On the server side, it matches the Order's getProduct() method
         */
        System.out.println("\n");
        System.out.println("Sent HTTP GET request to query sub resource product info");
        get = new GetMethod("http://localhost:8181/cxf/securecrm/customerservice/orders/223/products/323");
        get.getHostAuthState().setAuthScheme(scheme);
        try {
            httpClient.executeMethod(get);
            System.out.println(get.getResponseBodyAsString());
        } finally {
            get.releaseConnection();
        }

        /**
         * HTTP PUT http://localhost:8181/cxf/securecrm/customerservice/customers is used to upload the contents of
         * the update_customer.xml file to update the customer information for customer 123.
         *
         * On the server side, it matches the CustomerService's updateCustomer() method
         */
        System.out.println("\n");
        System.out.println("Sent HTTP PUT request to update customer info");
        
        String inputFile = Client.class.getResource("update_customer.xml").getFile();
        File input = new File(inputFile);
        PutMethod put = new PutMethod("http://localhost:8181/cxf/securecrm/customerservice/customers");
        put.getHostAuthState().setAuthScheme(scheme);
        RequestEntity entity = new FileRequestEntity(input, "text/xml; charset=ISO-8859-1");
        put.setRequestEntity(entity);
        

        try {
            int result = httpClient.executeMethod(put);
            System.out.println("Response status code: " + result);
            System.out.println("Response body: ");
            System.out.println(put.getResponseBodyAsString());
        } finally {
            // Release current connection to the connection pool once you are
            // done
            put.releaseConnection();
        }

        /**
         * HTTP POST http://localhost:8181/cxf/securecrm/customerservice/customers is used to upload the contents of
         * the add_customer.xml file to add a new customer to the system.
         *
         * On the server side, it matches the CustomerService's addCustomer() method
         */
        System.out.println("\n");
        System.out.println("Sent HTTP POST request to add customer");
        inputFile = Client.class.getResource("add_customer.xml").getFile();
        input = new File(inputFile);
        PostMethod post = new PostMethod("http://localhost:8181/cxf/securecrm/customerservice/customers");
        post.getHostAuthState().setAuthScheme(scheme);
        post.addRequestHeader("Accept" , "text/xml");
        entity = new FileRequestEntity(input, "text/xml; charset=ISO-8859-1");
        post.setRequestEntity(entity);

        try {
            int result = httpClient.executeMethod(post);
            System.out.println("Response status code: " + result);
            System.out.println("Response body: ");
            System.out.println(post.getResponseBodyAsString());
        } finally {
            // Release current connection to the connection pool once you are
            // done
            post.releaseConnection();
        }

        System.out.println("\n");
        System.exit(0);
    }

}
