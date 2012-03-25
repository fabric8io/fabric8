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
// START SNIPPET: service
package org.fusesource.examples.cxf.jaxws.security;

import javax.jws.WebService;

/**
 * This is our web service implementation, which implements the web service interface.
 * We also add the @WebService annotation to it to mark this class an implementation for the endpoint interface.
 */
@WebService(endpointInterface = "org.fusesource.examples.cxf.jaxws.security.HelloWorld")
public class HelloWorldImpl implements HelloWorld {

    /**
     * Just a simple implementation for a friendly message that says hello.
     */
    public String sayHi(String name) {
        return "Hello " + name;
    }
}
