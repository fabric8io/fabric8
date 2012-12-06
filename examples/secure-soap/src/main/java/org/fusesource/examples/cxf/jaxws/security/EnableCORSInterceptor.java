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
package org.fusesource.examples.cxf.jaxws.security;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.Headers;

public class EnableCORSInterceptor extends AbstractPhaseInterceptor<Message> {

    public EnableCORSInterceptor() {
        super(Phase.PRE_PROTOCOL);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        Map<String, List<String>> headers = Headers.getSetProtocolHeaders(message);
        try {
            //Access-Control-Allow-Origin:* Access-Control-Allow-Methods:POST,GET
            headers.put("Access-Control-Allow-Origin", Arrays.asList("*"));
            headers.put("Access-Control-Allow-Methods", Arrays.asList("POST", "GET"));
        } catch (Exception ce) {
            throw new Fault(ce);
        }
    }
}
