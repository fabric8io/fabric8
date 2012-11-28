/*
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
package org.fusesource.examples.cxf.jaxws.security.client;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.fusesource.examples.cxf.jaxws.security.HelloWorld;

/**
 * A Java client application that uses CXF's JaxWsProxyFactoryBean to create a web service client proxy to invoke
 * the remote web service.
 */
public class Client{

    public static void main(String[] args) {
        try {
            new Client().sendRequest();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
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
        System.out.println(ret);
    }

}
