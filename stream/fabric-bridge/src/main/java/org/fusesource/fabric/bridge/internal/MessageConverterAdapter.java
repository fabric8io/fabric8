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

import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.spi.NamingManager;
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
