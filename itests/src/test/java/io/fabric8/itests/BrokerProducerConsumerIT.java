/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.itests;

import io.fabric8.arquillian.kubernetes.Constants;
import io.fabric8.arquillian.kubernetes.Session;
import io.fabric8.kubernetes.model.KubernetesClient;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.jolokia.JolokiaClients;
import io.fabric8.utils.Asserts;
import io.fabric8.utils.Block;
import org.assertj.core.api.Condition;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jolokia.client.J4pClient;
import org.junit.Test;
import org.junit.runner.RunWith;

import static io.fabric8.kubernetes.assertions.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static io.fabric8.jolokia.assertions.Assertions.assertThat;

/**
 * Integration test that broker, producer, consumer all works
 */
@RunWith(Arquillian.class)
public class BrokerProducerConsumerIT {

    @ArquillianResource
    KubernetesClient client;

    @ArquillianResource
    Session session;

    @ArquillianResource
    JolokiaClients jolokiaClients;

    String brokerReplicationControllerId = "fabric8mq-controller";
    String consumerReplicationControllerId = "fabric8mq-consumer-controller";

    @Test
    public void testMQConsumer() throws Exception {
        assertThat(client).replicationController(brokerReplicationControllerId).isNotNull();
        assertThat(client).replicationController(consumerReplicationControllerId).isNotNull();

        assertThat(client).pods()
                .runningStatus()
                .filterNamespace(session.getNamespace())
                .haveAtLeast(1, new Condition<Pod>() {
                    @Override
                    public boolean matches(Pod podSchema) {
                        return true;
                    }
                });

        Asserts.assertWaitFor(10 * 60 * 1000, new Block() {
            @Override
            public void invoke() throws Exception {
                J4pClient brokerClient = jolokiaClients.assertClientForReplicationController(brokerReplicationControllerId);
                J4pClient consumerClient = jolokiaClients.assertClientForReplicationController(consumerReplicationControllerId);

                assertThat(consumerClient).stringAttribute("org.apache.camel:context=camel-1,type=context,name=\"camel-1\"", "State").isEqualTo("Started");
                assertThat(brokerClient).longAttribute("org.apache.activemq:type=Broker,brokerName=default,destinationType=Queue,destinationName=TEST.FOO", "EnqueueCount").isGreaterThan(1000);
                assertThat(brokerClient).longAttribute("org.apache.activemq:type=Broker,brokerName=default,destinationType=Queue,destinationName=TEST.FOO", "DequeueCount").isGreaterThan(1000);
            }
        });
    }

}
