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
package org.jboss.fuse.examples.cxf.jaxrs.client;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;

/**
 * The client class has a main method that accesses a few of the resources defined in our JAX-RS example using
 * the Apache Commons HttpClient classes.
 */
public final class Client {

    private Client() {
    }

    public static void main(String args[]) throws Exception {

        /**
         * HTTP GET http://localhost:8181/cxf/crm/customerservice/customers/123
         * returns the XML document representing customer 123
         *
         * On the server side, it matches the CustomerService's getCustomer() method
         */
        System.out.println("Sent HTTP GET request to query customer info");
        URL url = new URL("http://localhost:8181/cxf/crm/customerservice/customers/123");
        InputStream in = url.openStream();
        System.out.println(getStringFromInputStream(in));

        /**
         * HTTP GET http://localhost:8181/cxf/crm/customerservice/orders/223/products/323
         * returns the XML document representing product 323 in order 223
         *
         * On the server side, it matches the Order's getProduct() method
         */
        System.out.println("\n");
        System.out.println("Sent HTTP GET request to query sub resource product info");
        url = new URL("http://localhost:8181/cxf/crm/customerservice/orders/223/products/323");
        in = url.openStream();
        System.out.println(getStringFromInputStream(in));

        /**
         * HTTP PUT http://localhost:8181/cxf/crm/customerservice/customers is used to upload the contents of
         * the update_customer.xml file to update the customer information for customer 123.
         *
         * On the server side, it matches the CustomerService's updateCustomer() method
         */
        System.out.println("\n");
        System.out.println("Sent HTTP PUT request to update customer info");
        Client client = new Client();
        String inputFile = client.getClass().getResource("update_customer.xml").getFile();
        File input = new File(inputFile);
        PutMethod put = new PutMethod("http://localhost:8181/cxf/crm/customerservice/customers");
        RequestEntity entity = new FileRequestEntity(input, "text/xml; charset=ISO-8859-1");
        put.setRequestEntity(entity);
        HttpClient httpclient = new HttpClient();

        try {
            int result = httpclient.executeMethod(put);
            System.out.println("Response status code: " + result);
            System.out.println("Response body: ");
            System.out.println(put.getResponseBodyAsString());
        } finally {
            // Release current connection to the connection pool once you are
            // done
            put.releaseConnection();
        }

        /**
         * HTTP POST http://localhost:8181/cxf/crm/customerservice/customers is used to upload the contents of
         * the add_customer.xml file to add a new customer to the system.
         *
         * On the server side, it matches the CustomerService's addCustomer() method
         */
        System.out.println("\n");
        System.out.println("Sent HTTP POST request to add customer");
        inputFile = client.getClass().getResource("add_customer.xml").getFile();
        input = new File(inputFile);
        PostMethod post = new PostMethod("http://localhost:8181/cxf/crm/customerservice/customers");
        post.addRequestHeader("Accept" , "text/xml");
        entity = new FileRequestEntity(input, "text/xml; charset=ISO-8859-1");
        post.setRequestEntity(entity);
        httpclient = new HttpClient();

        try {
            int result = httpclient.executeMethod(post);
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

    /*
     * Just a simple helper method to read bytes from an InputStream and return the String representation.
     */
    private static String getStringFromInputStream(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int c = 0;
        while ((c = in.read()) != -1) {
            bos.write(c);
        }
        in.close();
        bos.close();
        return bos.toString();
    }

}
