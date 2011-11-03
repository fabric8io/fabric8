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

import org.fusesource.fabric.bridge.MessageConverter;
import org.fusesource.fabric.bridge.spring.TestMessageConverter;
import org.jasypt.contrib.org.apache.commons.codec_1_3.binary.Base64;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageConverterAdapterTest extends AbstractConnectorTestSupport {

    private MessageConverterAdapter adapter = new MessageConverterAdapter();
    private static final Logger LOG = LoggerFactory.getLogger(MessageConverterAdapterTest.class);

    @Test
	public void testMarshalMessageConverter() throws Exception {
		byte[] bytes = adapter.marshal(new TestMessageConverter());
        String str = new String(Base64.encodeBase64(bytes));
        LOG.info("Marshaled ConnectionFactory bytes base64 encoded" + System.getProperty("line.separator") + str);
		assertNotNull("Null marshaled bytes", bytes);
		assertFalse("Empty marshaled bytes", bytes.length == 0);
	}
	
	@Test
	public void testUnmarshalByteArray() throws Exception {
		byte[] bytes = adapter.marshal(new TestMessageConverter());
		MessageConverter newMessageConverter = adapter.unmarshal(bytes);
		assertNotNull("Message converter is null", newMessageConverter);
	}

}
