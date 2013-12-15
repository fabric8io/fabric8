/*
 * Copyright 2010 Red Hat, Inc.
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

package io.fabric8.activemq.facade;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.region.policy.PolicyEntry;
import org.apache.activemq.broker.region.policy.PolicyMap;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Session;

public class EmbeddedBrokerTestSupport {

    protected String bindAddress = "vm://localhost";
    protected BrokerService broker;
    protected ConnectionFactory connectionFactory;
    protected Connection connection;
    protected Session session;

    public EmbeddedBrokerTestSupport() {
    }

    protected BrokerService createBroker() throws Exception {
        BrokerService answer = new BrokerService();
        answer.setPersistent(false);
        answer.setDeleteAllMessagesOnStartup(true);
        answer.setUseJmx(true);

        // apply memory limit so that %usage is visible
        PolicyMap policyMap = new PolicyMap();
        PolicyEntry defaultEntry = new PolicyEntry();
        defaultEntry.setMemoryLimit(1024 * 1024 * 4);
        policyMap.setDefaultEntry(defaultEntry);
        answer.setDestinationPolicy(policyMap);

        answer.addConnector(bindAddress);
        return answer;
    }

    protected ConnectionFactory createConnectionFactory() throws Exception {
        return new ActiveMQConnectionFactory(bindAddress);
    }

    protected Connection createConnection() throws Exception {
        return connectionFactory.createConnection();
    }

    protected void startBroker() throws Exception {
        broker.start();
        broker.waitUntilStarted();
    }
}
