/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.codec.marshaller;

import org.fusesource.fabric.apollo.amqp.codec.types.AMQPSymbol;
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPULong;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;

import java.io.DataOutput;
import java.math.BigInteger;

/**
 *
 */
public class DescribedConstructor {

    protected Buffer buffer;

    public DescribedConstructor(BigInteger descriptor) {
        int size = (int) (1 + TypeRegistry.instance().sizer().sizeOfULong(descriptor));
        DataByteArrayOutputStream out = new DataByteArrayOutputStream(size);
        try {
            out.writeByte(0x0);
            AMQPULong.write(descriptor, out);
            buffer = out.toBuffer();
        } catch (Exception e) {
            throw new RuntimeException("Exception constructing DescribedConstructor instance for descriptor " + descriptor + " : " + e.getMessage());
        }
    }

    public DescribedConstructor(Buffer descriptor) {
        int size = (int) (1 + TypeRegistry.instance().sizer().sizeOfSymbol(descriptor));
        DataByteArrayOutputStream out = new DataByteArrayOutputStream(size);
        try {
            out.writeByte(0x0);
            AMQPSymbol.write(descriptor, out);
            buffer = out.toBuffer();
        } catch (Exception e) {
            throw new RuntimeException("Exception constructing DescribedConstructor instance for descriptor " + descriptor + " : " + e.getMessage());
        }
    }

    public Buffer getBuffer() {
        return buffer;
    }

    public void write(DataOutput out) throws Exception {
        buffer.writeTo(out);
    }

    public long size() {
        return buffer.length();
    }
}
