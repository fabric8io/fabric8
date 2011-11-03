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
