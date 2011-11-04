/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.bridge.model;

import junit.framework.Assert;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.fusesource.fabric.bridge.internal.AbstractConnectorTestSupport;
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


public class BrokerConfigJaxbTest extends Assert {

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
