/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.quickstarts.rest;

import junit.framework.Assert;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * The client class has a main method that accesses a few of the resources defined in our JAX-RS example using
 * the Apache Commons HttpClient classes.
 */
public final class CrmTest {

    public static final String CUSTOMER_TEST_URL = "http://localhost:8181/cxf/crm/customerservice/customers/123";
    public static final String PRODUCT_ORDER_TEST_URL = "http://localhost:8181/cxf/crm/customerservice/orders/223/products/323";
    public static final String CUSTOMER_SERVICE_URL = "http://localhost:8181/cxf/crm/customerservice/customers";
    private static final Logger LOG = LoggerFactory.getLogger(CrmTest.class);
    private URL url;
    private InputStream in;

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

    /**
     * HTTP GET http://localhost:8181/cxf/crm/customerservice/customers/123
     * returns the XML document representing customer 123
     * <p/>
     * On the server side, it matches the CustomerService's getCustomer() method
     *
     * @throws Exception
     */
    @Test
    public void getCustomerTest() throws Exception {
        LOG.info("Sent HTTP GET request to query customer info");
        url = new URL(CUSTOMER_TEST_URL);
        InputStream in = null;
        try {
            in = url.openStream();
        } catch (IOException e) {
            LOG.error("Error connecting to {}", CUSTOMER_TEST_URL);
            LOG.error("You should build the 'rest' quick start and deploy it to a local Fabric8 before running this test");
            LOG.error("Please read the README.md file in 'rest' quick start root");
            Assert.fail("Connection error");
        }
        String res = getStringFromInputStream(in);
        LOG.info(res);
        Assert.assertTrue(res.contains("123"));
    }

    /**
     * HTTP GET http://localhost:8181/cxf/crm/customerservice/orders/223/products/323
     * returns the XML document representing product 323 in order 223
     * <p/>
     * On the server side, it matches the Order's getProduct() method
     *
     * @throws Exception
     */
    @Test
    public void getProductOrderTest() throws Exception {

        LOG.info("Sent HTTP GET request to query sub resource product info");
        url = new URL(PRODUCT_ORDER_TEST_URL);
        try {
            in = url.openStream();
        } catch (IOException e) {
            LOG.error("Error connecting to {}", PRODUCT_ORDER_TEST_URL);
            LOG.error("You should build the 'rest' quick start and deploy it to a local Fabric8 before running this test");
            LOG.error("Please read the README.md file in 'rest' quick start root");
            Assert.fail("Connection error");
        }

        String res = getStringFromInputStream(in);
        LOG.info(res);
        Assert.assertTrue(res.contains("product 323"));
    }

    /**
     * HTTP POST http://localhost:8181/cxf/crm/customerservice/customers is used to upload the contents of
     * the add_customer.json file to add a new customer to the system.
     * <p/>
     * On the server side, it matches the CustomerService's addCustomer() method
     *
     * @throws Exception
     */
    @Test
    public void postCustomerTestJson() throws IOException {
        LOG.info("Sent HTTP POST request to add customer");
        String inputFile = this.getClass().getResource("/add_customer.json").getFile();
        File input = new File(inputFile);
        PostMethod post = new PostMethod(CUSTOMER_SERVICE_URL);
        post.addRequestHeader("Accept", "application/json");
        RequestEntity entity = new FileRequestEntity(input, "application/json; charset=ISO-8859-1");
        post.setRequestEntity(entity);
        HttpClient httpclient = new HttpClient();
        String res = "";

        try {
            int result = httpclient.executeMethod(post);
            LOG.info("Response status code: " + result);
            LOG.info("Response body: ");
            res = post.getResponseBodyAsString();
            LOG.info(res);
        } catch (IOException e) {
            LOG.error("Error connecting to {}", CUSTOMER_SERVICE_URL);
            LOG.error("You should build the 'rest' quick start and deploy it to a local Fabric8 before running this test");
            LOG.error("Please read the README.md file in 'rest' quick start root");
            Assert.fail("Connection error");
        } finally {
            // Release current connection to the connection pool once you are
            // done
            post.releaseConnection();
        }
        Assert.assertTrue(res.contains("Jack"));

    }


    /**
     * HTTP POST http://localhost:8181/cxf/crm/customerservice/customers is used to upload the contents of
     * the add_customer.xml file to add a new customer to the system.
     * <p/>
     * On the server side, it matches the CustomerService's addCustomer() method
     *
     * @throws Exception
     */
    @Test
    public void postCustomerTest() throws IOException {
        LOG.info("Sent HTTP POST request to add customer");
        String inputFile = this.getClass().getResource("/add_customer.xml").getFile();
        File input = new File(inputFile);
        PostMethod post = new PostMethod(CUSTOMER_SERVICE_URL);
        post.addRequestHeader("Accept", "application/xml");
        RequestEntity entity = new FileRequestEntity(input, "application/xml; charset=ISO-8859-1");
        post.setRequestEntity(entity);
        HttpClient httpclient = new HttpClient();
        String res = "";

        try {
            int result = httpclient.executeMethod(post);
            LOG.info("Response status code: " + result);
            LOG.info("Response body: ");
            res = post.getResponseBodyAsString();
            LOG.info(res);
        } catch (IOException e) {
            LOG.error("Error connecting to {}", CUSTOMER_SERVICE_URL);
            LOG.error("You should build the 'rest' quick start and deploy it to a local Fabric8 before running this test");
            LOG.error("Please read the README.md file in 'rest' quick start root");
            Assert.fail("Connection error");
        } finally {
            // Release current connection to the connection pool once you are
            // done
            post.releaseConnection();
        }
        Assert.assertTrue(res.contains("Jack"));

    }

    /**
     * HTTP PUT http://localhost:8181/cxf/crm/customerservice/customers is used to upload the contents of
     * the update_customer.xml file to update the customer information for customer 123.
     * <p/>
     * On the server side, it matches the CustomerService's updateCustomer() method
     *
     * @throws Exception
     */
    @Test
    public void putCustomerTest() throws IOException {


        LOG.info("Sent HTTP PUT request to update customer info");

        String inputFile = this.getClass().getResource("/update_customer.xml").getFile();
        File input = new File(inputFile);
        PutMethod put = new PutMethod(CUSTOMER_SERVICE_URL);
        RequestEntity entity = new FileRequestEntity(input, "application/xml; charset=ISO-8859-1");
        put.setRequestEntity(entity);
        HttpClient httpclient = new HttpClient();
        int result = 0;
        try {
            result = httpclient.executeMethod(put);
            LOG.info("Response status code: " + result);
            LOG.info("Response body: ");
            LOG.info(put.getResponseBodyAsString());
        } catch (IOException e) {
            LOG.error("Error connecting to {}", CUSTOMER_SERVICE_URL);
            LOG.error("You should build the 'rest' quick start and deploy it to a local Fabric8 before running this test");
            LOG.error("Please read the README.md file in 'rest' quick start root");
            Assert.fail("Connection error");
        } finally {
            // Release current connection to the connection pool once you are
            // done
            put.releaseConnection();
        }

        Assert.assertEquals(result, 200);
    }


}
