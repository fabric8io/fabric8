/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.quickstarts.rest.secure;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@com.wordnik.swagger.annotations.Api(value = "/customerservice", description = "Operations about CustomerService")
/**
 * This Java class with be hosted in the URI path defined by the @Path annotation. @Path annotations on the methods
 * of this class always refer to a path relative to the path defined at the class level.
 * <p/>
 * For example, with 'http://localhost:8181/cxf' as the default CXF servlet path and '/crm' as the JAX-RS server path,
 * this class will be hosted in 'http://localhost:8181/cxf/crm/customerservice'.  An @Path("/customers") annotation on
 * one of the methods would result in 'http://localhost:8181/cxf/customerservice/customers'.
 */
@Path("/customerservice/")
public class CustomerService {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerService.class);

    long currentId = 123;
    Map<Long, Customer> customers = new HashMap<Long, Customer>();
    Map<Long, Order> orders = new HashMap<Long, Order>();

    public CustomerService() {
        init();
    }

    /**
     * This method is mapped to an HTTP GET of 'http://localhost:8181/cxf/crm/customerservice/customers/{id}'.  The value for
     * {id} will be passed to this message as a parameter, using the @PathParam annotation.
     * <p/>
     * The method returns a Customer object - for creating the HTTP response, this object is marshaled into XML using JAXB.
     * <p/>
     * For example: surfing to 'http://localhost:8181/cxf/crm/customerservice/customers/123' will show you the information of
     * customer 123 in XML format.
     */
    @com.wordnik.swagger.annotations.ApiOperation(
        value = "getCustomer",
        notes = "/** \n * This method is mapped to an HTTP GET of 'http://localhost:8181/cxf/crm/customerservice/customers/{id}'.  The value for {id} will be passed to this message as a parameter, using the @PathParam annotation. <p/> The method returns a Customer object - for creating the HTTP response, this object is marshaled into XML using JAXB. <p/> For example: surfing to 'http://localhost:8181/cxf/crm/customerservice/customers/123' will show you the information of customer 123 in XML format.\n */\n")
    @GET
    @Path("/customers/{id}/")
    @Produces("application/xml")
    public Customer getCustomer(@com.wordnik.swagger.annotations.ApiParam(value = "id") @PathParam("id") String id) {
        LOG.info("Invoking getCustomer, Customer id is: {}", id);
        long idNumber = Long.parseLong(id);
        Customer c = customers.get(idNumber);
        return c;
    }

    /**
     * Using HTTP PUT, we can can upload the XML representation of a customer object.  This operation will be mapped
     * to the method below and the XML representation will get unmarshaled into a real Customer object using JAXB.
     * <p/>
     * The method itself just updates the customer object in our local data map and afterwards uses the Reponse class to
     * build the appropriate HTTP response: either OK if the update succeeded (translates to HTTP Status 200/OK) or not
     * modified if the method failed to update a customer object (translates to HTTP Status 304/Not Modified).
     * <p/>
     * Note how this method is using the same @Path value as our next method - the HTTP method used will determine which
     * method is being invoked.
     */
    @com.wordnik.swagger.annotations.ApiOperation(
        value = "updateCustomer",
        notes = "/** \n * Using HTTP PUT, we can can upload the XML representation of a customer object.  This operation will be mapped to the method below and the XML representation will get unmarshaled into a real Customer object using JAXB. <p/> The method itself just updates the customer object in our local data map and afterwards uses the Reponse class to build the appropriate HTTP response: either OK if the update succeeded (translates to HTTP Status 200/OK) or not modified if the method failed to update a customer object (translates to HTTP Status 304/Not Modified). <p/> Note how this method is using the same @Path value as our next method - the HTTP method used will determine which method is being invoked.\n */\n")
    @PUT
    @Path("/customers/")
    public Response updateCustomer(@com.wordnik.swagger.annotations.ApiParam(value = "customer") Customer customer) {
        LOG.info("Invoking updateCustomer, Customer name is: {}", customer.getName());
        Customer c = customers.get(customer.getId());
        Response r;
        if (c != null) {
            customers.put(customer.getId(), customer);
            r = Response.ok().build();
        } else {
            r = Response.notModified().build();
        }

        return r;
    }

    /**
     * Using HTTP POST, we can add a new customer to the system by uploading the XML representation for the customer.
     * This operation will be mapped to the method below and the XML representation will get unmarshaled into a real
     * Customer object.
     * <p/>
     * After the method has added the customer to the local data map, it will use the Response class to build the HTTP reponse,
     * sending back the inserted customer object together with a HTTP Status 200/OK.  This allows us to send back the
     * new id for the customer object to the client application along with any other data that might have been updated in
     * the process.
     * <p/>
     * Note how this method is using the same @Path value as our previous method - the HTTP method used will determine which
     * method is being invoked.
     */
    @com.wordnik.swagger.annotations.ApiOperation(
        value = "addCustomer",
        notes = "/** \n * Using HTTP POST, we can add a new customer to the system by uploading the XML representation for the customer. This operation will be mapped to the method below and the XML representation will get unmarshaled into a real Customer object. <p/> After the method has added the customer to the local data map, it will use the Response class to build the HTTP reponse, sending back the inserted customer object together with a HTTP Status 200/OK.  This allows us to send back the new id for the customer object to the client application along with any other data that might have been updated in the process. <p/> Note how this method is using the same @Path value as our previous method - the HTTP method used will determine which method is being invoked.\n */\n")
    @POST
    @Path("/customers/")
    public Response addCustomer(@com.wordnik.swagger.annotations.ApiParam(value = "customer") Customer customer) {
        LOG.info("Invoking addCustomer, Customer name is: {}", customer.getName());
        customer.setId(++currentId);

        customers.put(customer.getId(), customer);

        return Response.ok().type("application/xml").entity(customer).build();
    }

    /**
     * This method is mapped to an HTTP DELETE of 'http://localhost:8181/cxf/crm/customerservice/customers/{id}'.  The value for
     * {id} will be passed to this message as a parameter, using the @PathParam annotation.
     * <p/>
     * The method uses the Response class to create the HTTP response: either HTTP Status 200/OK if the customer object was
     * successfully removed from the local data map or a HTTP Status 304/Not Modified if it failed to remove the object.
     */
    @com.wordnik.swagger.annotations.ApiOperation(
        value = "deleteCustomer",
        notes = "/** \n * This method is mapped to an HTTP DELETE of 'http://localhost:8181/cxf/crm/customerservice/customers/{id}'.  The value for {id} will be passed to this message as a parameter, using the @PathParam annotation. <p/> The method uses the Response class to create the HTTP response: either HTTP Status 200/OK if the customer object was successfully removed from the local data map or a HTTP Status 304/Not Modified if it failed to remove the object.\n */\n")
    @DELETE
    @Path("/customers/{id}/")
    public Response deleteCustomer(@com.wordnik.swagger.annotations.ApiParam(value = "id") @PathParam("id") String id) {
        LOG.info("Invoking deleteCustomer, Customer id is: {}", id);
        long idNumber = Long.parseLong(id);
        Customer c = customers.get(idNumber);

        Response r;
        if (c != null) {
            r = Response.ok().build();
            customers.remove(idNumber);
        } else {
            r = Response.notModified().build();
        }

        return r;
    }

    /**
     * This method is mapped to an HTTP GET of 'http://localhost:8181/cxf/crm/customerservice/orders/{id}'.  The value for
     * {id} will be passed to this message as a parameter, using the @PathParam annotation.
     * <p/>
     * The method returns an Order object - the class for that object includes a few more JAX-RS annotations, allowing it to
     * display one of these two outputs, depending on the actual URI path being used:
     * - display the order information itself in XML format
     * - display details about a product in the order in XML format in a path relative to the URI defined here
     */
    @com.wordnik.swagger.annotations.ApiOperation(
        value = "getOrder",
        notes = "/** \n * This method is mapped to an HTTP GET of 'http://localhost:8181/cxf/crm/customerservice/orders/{id}'.  The value for {id} will be passed to this message as a parameter, using the @PathParam annotation. <p/> The method returns an Order object - the class for that object includes a few more JAX-RS annotations, allowing it to display one of these two outputs, depending on the actual URI path being used: - display the order information itself in XML format - display details about a product in the order in XML format in a path relative to the URI defined here\n */\n")
    @Path("/orders/{orderId}/")
    public Order getOrder(@com.wordnik.swagger.annotations.ApiParam(value = "orderId") @PathParam("orderId") String orderId) {
        LOG.info("Invoking getOrder, Order id is: {}", orderId);
        long idNumber = Long.parseLong(orderId);
        Order c = orders.get(idNumber);
        return c;
    }

    /**
     * The init method is used by the constructor to insert a Customer and Order object into the local data map
     * for testing purposes.
     */
    final void init() {
        Customer c = new Customer();
        c.setName("John");
        c.setId(123);
        customers.put(c.getId(), c);

        Order o = new Order();
        o.setDescription("order 223");
        o.setId(223);
        orders.put(o.getId(), o);
    }

}
