/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.codec;

import org.fusesource.fabric.apollo.amqp.codec.marshaller.AmqpEncodingError;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.AmqpMarshaller;
import org.fusesource.fabric.apollo.amqp.codec.types.AmqpType;

import java.io.*;

import static org.fusesource.fabric.apollo.amqp.codec.marshaller.v1_0_0.AmqpMarshaller.getMarshaller;

/**
 *
 */
public class CodecUtils {

    public static long getSize(AmqpType<?, ?> type) {
        return type.getBuffer(getMarshaller()).getEncoded().getEncodedSize();
    }

    static public AmqpFrame unmarshalAmqpFrame(byte[] b) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(b));
        return new AmqpFrame(in);
    }

    static byte[] marshalAmqpFrame(AmqpFrame frame) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);
        frame.write(out);
        return bos.toByteArray();
    }

    // typical marshal/unmarshal cases
    static public <T extends AmqpType<?, ?>> byte[] marshal(T type) throws IOException, AmqpEncodingError {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);

        AmqpMarshaller marshaller = getMarshaller();
        type.marshal(out, marshaller);
        out.flush();
        return bos.toByteArray();
    }

    static public <T extends AmqpType<?, ?>> T unmarshal(byte[] b) throws IOException, AmqpEncodingError {
        AmqpMarshaller marshaller = getMarshaller();
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(b));
        return (T) marshaller.unmarshalType(in);
    }

    static public <T extends AmqpType<?, ?>> T marshalUnmarshal(T type) throws IOException, AmqpEncodingError {
        //print("Received : %s", type);
        byte b[] = marshal(type);
        AmqpType<?, ?> ret = unmarshal(b);
        //print("Returning : %s", ret);
        return (T)ret;
    }
}
