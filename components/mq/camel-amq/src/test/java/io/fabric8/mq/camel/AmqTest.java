/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.mq.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Test;

/**
 * Asserts that in a plain JVM we can instantiate the AMQ component
 */
public class AmqTest {
    @Test
    public void testComponent() throws Exception {
        CamelContext camelContext = new DefaultCamelContext();
        Endpoint endpoint = camelContext.getEndpoint("amq:someQueue");
        System.out.println("Found endpoint: " + endpoint);
        AMQComponent amq = (AMQComponent) camelContext.getComponent("amq");
        System.out.println("BrokerURL: " + amq.getConfiguration().getBrokerURL());
    }

}
