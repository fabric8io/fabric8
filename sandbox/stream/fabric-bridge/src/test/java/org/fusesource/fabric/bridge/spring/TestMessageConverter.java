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
package io.fabric8.bridge.spring;

import javax.jms.JMSException;
import javax.jms.Message;

import io.fabric8.bridge.MessageConverter;
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
