/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.bridge.zk.spring;

import io.fabric8.bridge.internal.AbstractConnector;
import io.fabric8.bridge.internal.AbstractConnectorTestSupport;
import io.fabric8.bridge.model.BridgeDestinationsConfig;
import io.fabric8.bridge.zk.ZkBridgeConnector;
import io.fabric8.bridge.zk.ZkGatewayConnector;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;

import javax.jms.JMSException;
import javax.jms.TextMessage;

public class AbstractZkConnectorTestSupport extends AbstractConnectorTestSupport {
    protected static AbstractApplicationContext applicationContext;
    private static final int TEST_CONNECT_TIMEOUT = 15000;
    private static final String OUTBOUND_SUFFIX = ".out";

    @Test
    public void testInboundDispatch() throws Exception {
        // get beans to make sure they are created
        applicationContext.getBean("myzkbridge", ZkBridgeConnector.class);
        applicationContext.getBean("myzkgateway", ZkGatewayConnector.class);

        // wait for gateway to connect to the bridge
        ZkGatewayConnector bean = applicationContext.getBean("myzkgateway", ZkGatewayConnector.class);
        if (bean.getRemoteBridges().isEmpty()) {
            Thread.sleep(TEST_CONNECT_TIMEOUT);
        }
        assertFalse("Bridge did not connect to Gateway", bean.getRemoteBridges().isEmpty());

        // send messages to gateway inbound destinations on bridge local broker
        for (String sourceName : TEST_SOURCES) {
            super.sendMessages(TEST_LOCAL_BROKER_URL, sourceName, TEST_NUM_MESSAGES, null);
        }

        // check if we received the expected number of messages on inbound destinations on remote broker
        for (final String sourceName : TEST_SOURCES) {
            super.receiveMessages(TEST_REMOTE_BROKER_URL,
                sourceName, TEST_NUM_MESSAGES, new BaseMatcher<TextMessage>() {

                @Override
                public boolean matches(Object message) {
                    boolean retVal = false;
                    try {
                        retVal = ((TextMessage) message).getStringProperty(
                            BridgeDestinationsConfig.DEFAULT_DESTINATION_NAME_HEADER).matches(sourceName);
                    } catch (JMSException e) {
                        fail(e.getMessage());
                    }
                    return retVal;
                }

                @Override
                public void describeTo(Description description) {
                    description.appendText("TextMessage containing " +
                        BridgeDestinationsConfig.DEFAULT_DESTINATION_NAME_HEADER + " property");
                }
            });
        }
    }

    @Test
    public void testOutboundDispatch() throws Exception {
        // wait for gateway to connect to the bridge
        ZkGatewayConnector bean = applicationContext.getBean("myzkgateway", ZkGatewayConnector.class);
        if (bean.getRemoteBridges().isEmpty()) {
            Thread.sleep(TEST_CONNECT_TIMEOUT);
        }
        assertFalse("Bridge did not connect to Gateway", bean.getRemoteBridges().isEmpty());

        // send messages to gateway outbound destinations
        for (String sourceName : TEST_SOURCES) {
            super.sendMessages(TEST_REMOTE_BROKER_URL, sourceName + OUTBOUND_SUFFIX, TEST_NUM_MESSAGES, null);
        }

        // check if we received the expected number of messages on outbound destinations on local broker
        for (final String sourceName : TEST_SOURCES) {
            super.receiveMessages(TEST_LOCAL_BROKER_URL,
                sourceName + OUTBOUND_SUFFIX, TEST_NUM_MESSAGES, new BaseMatcher<TextMessage>() {

                @Override
                public boolean matches(Object message) {
                    boolean retVal = false;
                    try {
                        retVal = ((TextMessage) message).getStringProperty(
                            BridgeDestinationsConfig.DEFAULT_DESTINATION_NAME_HEADER).matches(
                            sourceName + OUTBOUND_SUFFIX);
                    } catch (JMSException e) {
                        fail(e.getMessage());
                    }
                    return retVal;
                }

                @Override
                public void describeTo(Description description) {
                    description.appendText("TextMessage containing " +
                        BridgeDestinationsConfig.DEFAULT_DESTINATION_NAME_HEADER + " property");
                }
            });
        }

    }
}