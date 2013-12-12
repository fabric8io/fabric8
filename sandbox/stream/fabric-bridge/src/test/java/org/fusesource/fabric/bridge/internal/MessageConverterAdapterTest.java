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

import io.fabric8.bridge.MessageConverter;
import io.fabric8.bridge.spring.TestMessageConverter;
import io.fabric8.utils.Base64Encoder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageConverterAdapterTest extends AbstractConnectorTestSupport {

    private MessageConverterAdapter adapter = new MessageConverterAdapter();
    private static final Logger LOG = LoggerFactory.getLogger(MessageConverterAdapterTest.class);

    @Test
	public void testMarshalMessageConverter() throws Exception {
		byte[] bytes = adapter.marshal(new TestMessageConverter());
        String str = new String(Base64Encoder.encode(bytes));
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
