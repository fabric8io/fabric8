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
package io.fabric8.bridge.internal;

import org.apache.activemq.pool.PooledConnectionFactory;
import io.fabric8.utils.Base64Encoder;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.ConnectionFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class ConnectionFactoryAdapterTest  {

	private PooledConnectionFactory connectionFactory;
	private ConnectionFactoryAdapter adapter;
    private static final Logger LOG = LoggerFactory.getLogger(ConnectionFactoryAdapterTest.class);
    private static final String TEST_LOCAL_BROKER_URL = "vm://localhost?broker.persistent=false";

    @Before
	public void setUp() throws Exception {
		connectionFactory = new PooledConnectionFactory(TEST_LOCAL_BROKER_URL);
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
		String str = new String(Base64Encoder.encode(bytes));
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
