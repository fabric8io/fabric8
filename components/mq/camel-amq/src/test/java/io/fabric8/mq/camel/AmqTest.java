/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.mq.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Asserts that in a plain JVM we can instantiate the AMQ component
 */
public class AmqTest {

    static final Logger LOG = LoggerFactory.getLogger(AmqTest.class);

    @Test
    public void testComponent() throws Exception {
        CamelContext camelContext = new DefaultCamelContext();
        Endpoint endpoint = camelContext.getEndpoint("amq:someQueue");

        LOG.info("Found endpoint: " + endpoint);
        AMQComponent amq = (AMQComponent) camelContext.getComponent("amq");
        LOG.info("BrokerURL: " + amq.getConfiguration().getBrokerURL());
    }

    @Test
    public void testComponentWithEnv() throws Exception {
        CamelContext camelContext = new DefaultCamelContext();
        Endpoint endpoint = camelContext.getEndpoint("amq:someQueue");

        // we can use JVM system properties
        System.setProperty("FABRIC8MQ_SERVICE_NAME", "myBroker");
        System.setProperty("MYBROKER_SERVICE_HOST", "superbroker");
        System.setProperty("MYBROKER_SERVICE_PORT", "1234");

        LOG.info("Found endpoint: " + endpoint);
        AMQComponent amq = (AMQComponent) camelContext.getComponent("amq");
        LOG.info("BrokerURL: " + amq.getConfiguration().getBrokerURL());
        Assert.assertEquals("failover:(tcp://superbroker:1234)", amq.getConfiguration().getBrokerURL());

        System.clearProperty("FABRIC8MQ_SERVICE_NAME");
        System.clearProperty("MYBROKER_SERVICE_HOST");
        System.clearProperty("MYBROKER_SERVICE_PORT");
    }

}
