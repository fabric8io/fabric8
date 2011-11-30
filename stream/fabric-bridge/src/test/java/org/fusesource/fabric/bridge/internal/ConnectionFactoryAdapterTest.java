/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.bridge.internal;

import javax.jms.ConnectionFactory;

import junit.framework.Assert;
import org.apache.activemq.pool.AmqJNDIPooledConnectionFactory;
import org.jasypt.contrib.org.apache.commons.codec_1_3.binary.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class ConnectionFactoryAdapterTest extends Assert {
	
	private AmqJNDIPooledConnectionFactory connectionFactory;
	private ConnectionFactoryAdapter adapter;
    private static final Logger LOG = LoggerFactory.getLogger(ConnectionFactoryAdapterTest.class);
    private static final String TEST_LOCAL_BROKER_URL = "vm://localhost?broker.persistent=false";

    @Before
	public void setUp() throws Exception {
		connectionFactory = new AmqJNDIPooledConnectionFactory(TEST_LOCAL_BROKER_URL);
		adapter = new ConnectionFactoryAdapter();
	}

	@After
	public void tearDown() throws Exception {
		connectionFactory.stop();
		connectionFactory = null;
		adapter = null;
	}

	@Test
	public void testMarshalConnectionFactory() throws Exception {
		byte[] bytes = adapter.marshal(connectionFactory);
		String str = new String(Base64.encodeBase64(bytes));
		LOG.info("Marshaled ConnectionFactory bytes base64 encoded" + System.getProperty("line.separator") + str);
		assertNotNull("Null marshaled bytes", bytes);
		assertFalse("Empty marshaled bytes", bytes.length == 0);
	}
	
	@Test
	public void testUnmarshalByteArray() throws Exception {
		byte[] bytes = adapter.marshal(connectionFactory);
		ConnectionFactory newConnectionFactory = adapter.unmarshal(bytes);
		assertNotNull("Connection factory is null", newConnectionFactory);
	}

}
