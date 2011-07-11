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

import org.fusesource.fabric.apollo.amqp.codec.api.AnnotatedMessage;
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AmqpType;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.TypeReader;
import org.fusesource.fabric.apollo.amqp.codec.types.AnnotatedMessageImpl;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.DataByteArrayInputStream;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;

import static org.fusesource.fabric.apollo.amqp.codec.marshaller.MessageSupport.decodeAnnotatedMessage;
import static org.fusesource.fabric.apollo.amqp.codec.marshaller.MessageSupport.encodeAnnotatedMessage;

/**
 *
 */
public class TestSupport {

    public static <T extends AmqpType> byte[] write(T value) throws Exception {
        DataByteArrayOutputStream out = new DataByteArrayOutputStream((int)value.size());
        value.write(out);
        return out.getData();
    }

    public static <T extends AmqpType> T read(byte[] b) throws Exception {
        DataByteArrayInputStream in = new DataByteArrayInputStream(b);
        return (T)TypeReader.read(in);
    }

    public static <T extends AmqpType> T writeRead(T value, boolean display) throws Exception {
        long size = value.size();
        if (size < 80 && display) {
            System.out.printf("%s -> ", value);
        }
        byte b[] = write(value);
        if (size < 80 && display) {
        System.out.printf("%s -> ", string(b));
        }
        T rc = (T)read(b);
        if (size < 80 && display) {
            System.out.printf("%s\n", rc);
        }
        return rc;
    }

    public static <T extends AmqpType> T writeRead(T value) throws Exception {
        return writeRead(value, true);
    }

    public static String string(byte[] b) {
        String rc = "length=" + b.length + " : ";
        for (byte l : b) {
            rc += String.format("[0x%x]", l);
        }
        return rc;
    }

    static AnnotatedMessage encodeDecode(AnnotatedMessage in) throws Exception {
        return encodeDecode(in, true);
    }

    static AnnotatedMessage encodeDecode(AnnotatedMessage in, boolean display) throws Exception {
        long size = ((AnnotatedMessageImpl)in).size();

        if (size < 80 && display) {
            System.out.printf("\n%s", in);
        }

        Buffer encoded = encodeAnnotatedMessage(in);
        if (size < 80 && display) {
            System.out.printf("\n%s", string(encoded.data));
        }

        AnnotatedMessage out = decodeAnnotatedMessage(encoded);
        if (size < 80 && display) {
            System.out.printf("\n%s\n", out);
        }
        return out;
    }
}
