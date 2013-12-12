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
package io.fabric8.bridge.model;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import static org.junit.Assert.assertEquals;

public class BrokerConfigJaxbTest {

	private static final String TEST_CLIENT_ID = "testClient";
	private static final String TEST_USER_NAME = "testUser";
	private static final String TEST_PASSWORD = "testPassword";
    private static final Logger LOG = LoggerFactory.getLogger(BrokerConfigJaxbTest.class);
    private static final String TEST_LOCAL_BROKER_URL = "vm://localhost?broker.persistent=false";
    private static final int TEST_BATCH_SIZE = 10;

    @Test
	public void testJaxbMarshalUnmarshal() throws JAXBException, UnsupportedEncodingException {
		BrokerConfig config = new BrokerConfig();
		config.setBrokerUrl(TEST_LOCAL_BROKER_URL);
		config.setClientId(TEST_CLIENT_ID);
		config.setConnectionFactory(new ActiveMQConnectionFactory(TEST_LOCAL_BROKER_URL));
		config.setMaxConnections(TEST_BATCH_SIZE);
		config.setUserName(TEST_USER_NAME);
		config.setPassword(TEST_PASSWORD);
		
		JAXBContext context = JAXBContext.newInstance(BrokerConfig.class);
		
		Marshaller marshaller = context.createMarshaller();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		marshaller.marshal(config, stream);
		String str = stream.toString("UTF-8");
        LOG.info("Marshaled broker config is " + System.getProperty("line.separator") + str);
		
		Unmarshaller unmarshaller = context.createUnmarshaller();
		ByteArrayInputStream stream2 = new ByteArrayInputStream(str.getBytes());
		BrokerConfig config2 = (BrokerConfig) unmarshaller.unmarshal(stream2);
		
		assertEquals(config, config2);
	}
}
