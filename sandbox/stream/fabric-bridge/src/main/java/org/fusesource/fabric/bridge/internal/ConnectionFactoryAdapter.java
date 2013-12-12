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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.jms.ConnectionFactory;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.spi.NamingManager;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * @author Dhiraj Bokde
 *
 */
public class ConnectionFactoryAdapter extends XmlAdapter<byte[], ConnectionFactory> {

	@Override
	public ConnectionFactory unmarshal(byte[] bytes) throws Exception {
		try {
			// create a reference from bytes
			ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(bytes));
			Reference reference = (Reference) stream.readObject();
			return (ConnectionFactory) NamingManager.getObjectInstance(reference, null, null, null);
		} catch (Exception e) {
			throw new JAXBException("Error unmarshalling connection factory: " + e.getMessage(), e);
		}
	}

	@Override
	public byte[] marshal(ConnectionFactory factory) throws Exception {
		// check if the connection factory is Referenceable
		if (factory instanceof Referenceable) {
			Referenceable referenceable = (Referenceable) factory;
			
			// get a JNDI reference for connection factory and write it to a byte[]
			Reference reference = referenceable.getReference();

			try {
				ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
				ObjectOutputStream stream = new ObjectOutputStream(byteStream);
				stream.writeObject(reference);
				stream.flush();
				byte[] bytes = byteStream.toByteArray();
				stream.close();
	
				return bytes;
			
			} catch (IOException e) {
				throw new JAXBException("Error marshalling connection factory " + factory + " : " + e.getMessage() ,e);
			}
		} else {
			throw new JAXBException("Not a JNDI Referenceable Connection factory " + factory);
		}
	}
	
}
