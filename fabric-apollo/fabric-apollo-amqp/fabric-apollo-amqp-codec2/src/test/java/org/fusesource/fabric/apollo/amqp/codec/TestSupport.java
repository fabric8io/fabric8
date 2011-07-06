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

import org.fusesource.fabric.apollo.amqp.codec.interfaces.AmqpType;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.TypeReader;
import org.fusesource.fabric.apollo.amqp.codec.types.Transfer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 *
 */
public class TestSupport {

    public static <T extends AmqpType> byte[] write(T value) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);
        value.write(out);
        return bos.toByteArray();
    }

    public static <T extends AmqpType> T read(byte[] b) throws Exception {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(b));
        return (T)TypeReader.read(in);
    }

    public static <T extends AmqpType> T writeRead(T value) throws Exception {
        byte b[] = write(value);
        T rc = (T)read(b);
        if (value.size() < 32 && !(value instanceof Transfer)) {
            System.out.printf("%s : %s -> %s -> %s\n", value.getClass().getSimpleName(), value, string(b), rc);
        }
        return rc;
    }

    public static String string(byte[] b) {
        String rc = "";
        for (byte l : b) {
            rc += String.format("[0x%x]", l);
        }
        return rc;
    }
}
