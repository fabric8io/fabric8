/**
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
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
package org.jboss.quickstarts.fuse.soap.secure;

import junit.framework.Assert;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * A Java client application that uses CXF's JaxWsProxyFactoryBean to create a web service client proxy to invoke
 * the remote web service.
 */
public class SecureSoapTest {

    private static final Logger LOG = LoggerFactory.getLogger(SecureSoapTest.class);


    public static void main(String[] args) {
        try {
            new SecureSoapTest().sendRequest();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void sendRequest() throws Exception {
        /*
         * Set up the JaxWsFactoryBean to access our client:
         * - the Java interface defining the service
         * - the HTTP address for the service
         */
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(HelloWorld.class);
        factory.setAddress("http://localhost:8181/cxf/HelloWorldSecurity");

        /*
         * Obtain a proxy, implementing the service interface, to access the remote interface.
         * It will allow you to easily perform the HTTP SOAP request from Java code.
         */
        HelloWorld client = (HelloWorld) factory.create();

        /*
         * Add the extra configuration and interceptors required for the authentication
         */
        Map<String, Object> outProps = new HashMap<String, Object>();
        outProps.put("action", "UsernameToken");
        ClientProxy.getClient(client).getOutInterceptors().add(new CustomSecurityInterceptor());
        ClientProxy.getClient(client).getOutInterceptors().add(new WSS4JOutInterceptor());

        /*
         * Calling sayHi() on on the client object will actually perform an HTTP SOAP request instead behind the scenes
         * and returns the resulting response.
         */
        String ret = client.sayHi("World");
        LOG.info("result: " + ret);

        Assert.assertEquals("Hello World", ret);
    }

}