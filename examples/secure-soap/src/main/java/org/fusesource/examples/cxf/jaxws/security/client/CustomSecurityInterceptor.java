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
package org.fusesource.examples.cxf.jaxws.security.client;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;

/**
 * CXF Interceptors are a very powerful and flexible mechanism to add custom logic to the default CXF processing,
 * both when using CXF on the client side and on the server side.
 *
 * With this custom security interceptor, we will configure the default WSS4J interceptor in the client to provide the required
 * credentials to perform our web service invocation.
 */
public class CustomSecurityInterceptor extends AbstractPhaseInterceptor<Message> {

    /**
     * Configuring the interceptor to be used in the 'setup' phase.
     */
    public CustomSecurityInterceptor() {
        super(Phase.SETUP);
    }

    /**
     * This is the actual implementation for our interceptor - we define the necessary properties for doing the authentication
     * and then iterate over the rest of the interceptor chain to find the WSS4J interceptor and configure it properly.
     */
    public void handleMessage(Message message) throws Fault {
        /*
         * Define the configuration properties
         */
        Map<String, Object> outProps = new HashMap<String, Object>();
        outProps.put("action", "UsernameToken");
        outProps.put("passwordType", "PasswordText");

        /*
         * The username ('admin') is provided as a literal, the corresponding password will be determined by the client
         * password callback object.
         */
        outProps.put("user", "admin");
        outProps.put("passwordCallbackClass", ClientPasswordCallback.class.getName());

        /*
         * Find the WSS4J interceptor in the interceptor chain and set the configuration properties
         */
        for (Interceptor interceptor : message.getInterceptorChain()) {
            //set properties for WSS4JOutInterceptor
            if (interceptor.getClass().getName().equals("org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor")) {
                ((WSS4JOutInterceptor) interceptor).setProperties(outProps);
            }
        }
    }
 
}
