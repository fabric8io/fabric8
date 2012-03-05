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


public class Client{
    public static void main(String[] args) {
        try {
        new Client().sendRequest();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void sendRequest() throws Exception {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(HelloWorld.class);
        factory.setAddress("http://localhost:8181/cxf/HelloWorldSecurity");
        HelloWorld client = (HelloWorld) factory.create();
        
        Map<String, Object> outProps = new HashMap<String, Object>();
        outProps.put("action", "UsernameToken");

        //add a CustomerSecurityInterceptor for client side to init wss4j staff
        //retrieve and set user/password,  users can easily add this interceptor
        //through spring configuration also
        ClientProxy.getClient(client).getOutInterceptors().add(new CustomerSecurityInterceptor());
        ClientProxy.getClient(client).getOutInterceptors().add(new WSS4JOutInterceptor());
        String ret = client.sayHi("ffang");
        System.out.println(ret);
    }

}
