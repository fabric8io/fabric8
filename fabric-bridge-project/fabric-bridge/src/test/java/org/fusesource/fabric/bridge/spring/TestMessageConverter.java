/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.bridge.spring;

import javax.jms.JMSException;
import javax.jms.Message;

import org.fusesource.fabric.bridge.MessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMessageConverter implements MessageConverter {

	private static final Logger LOG = LoggerFactory.getLogger(TestMessageConverter.class);

	@Override
	public Message convert(Message message) throws JMSException {
		LOG.info("Test message converter called");
		return message;
	}

    @Override
    public boolean equals(Object obj) {
        return true;
    }
}
