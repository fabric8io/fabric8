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

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.io.*;

/**
 * @author Dhiraj Bokde
 *
 */
public class MessageConverterAdapter extends XmlAdapter<byte[], MessageConverter> {

	@Override
	public MessageConverter unmarshal(byte[] bytes) throws Exception {
		try {
			// read from bytes
			ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(bytes));
            return (MessageConverter) stream.readObject();
		} catch (Exception e) {
			throw new JAXBException("Error unmarshalling message converter: " + e.getMessage(), e);
		}
	}

	@Override
	public byte[] marshal(MessageConverter messageConverter) throws Exception {
        // write the message converter to a byte[]
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream stream = new ObjectOutputStream(byteStream);
            stream.writeObject(messageConverter);
            stream.flush();
            byte[] bytes = byteStream.toByteArray();
            stream.close();

            return bytes;

        } catch (IOException e) {
            throw new JAXBException("Error marshaling connection factory " + messageConverter + " : " + e.getMessage() ,e);
        }
	}
	
}
