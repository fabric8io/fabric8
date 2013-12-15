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


import io.fabric8.bridge.spring.TestMessageConverter;
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

public class DispatchPolicyJaxbTest {

    private static final String TEST_MESSAGE_SELECTOR = "test1 = test2";
    private static final Logger LOG = LoggerFactory.getLogger(DispatchPolicyJaxbTest.class);

    @Test
	public void testJaxbMarshalUnmarshal() throws JAXBException, UnsupportedEncodingException {
		DispatchPolicy dispatchPolicy = new DispatchPolicy();
        // only set message converter and selector
        dispatchPolicy.setMessageConverter(new TestMessageConverter());
        dispatchPolicy.setMessageSelector(TEST_MESSAGE_SELECTOR);
		
		JAXBContext context = JAXBContext.newInstance(DispatchPolicy.class);
		
		Marshaller marshaller = context.createMarshaller();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		marshaller.marshal(dispatchPolicy, stream);
		String str = stream.toString("UTF-8");
		LOG.info("Marshaled dispatch policy is " + System.getProperty("line.separator") + str);
		
		Unmarshaller unmarshaller = context.createUnmarshaller();
		ByteArrayInputStream stream2 = new ByteArrayInputStream(str.getBytes());
		DispatchPolicy dispatchPolicy2 = (DispatchPolicy) unmarshaller.unmarshal(stream2);
		
		assertEquals(dispatchPolicy, dispatchPolicy2);
	}
}
