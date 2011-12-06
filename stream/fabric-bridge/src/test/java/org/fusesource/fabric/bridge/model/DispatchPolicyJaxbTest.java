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
import org.fusesource.fabric.bridge.internal.AbstractConnectorTestSupport;
import org.fusesource.fabric.bridge.spring.TestMessageConverter;
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


public class DispatchPolicyJaxbTest extends Assert {

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
