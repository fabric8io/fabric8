/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.codec.marshaller;

import org.fusesource.fabric.apollo.amqp.codec.AmqpDefinitions;
import org.fusesource.fabric.apollo.amqp.codec.AmqpProtocolHeader;
import org.fusesource.hawtbuf.codec.Codec;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class AmqpProtocolHeaderCodec implements Codec<AmqpProtocolHeader> {

    public static final AmqpProtocolHeaderCodec INSTANCE = new AmqpProtocolHeaderCodec();

    public AmqpProtocolHeader decode(DataInput in) throws IOException {
        byte magic[] = new byte[4];
        in.readFully(magic);
        if( !Arrays.equals(magic, AmqpDefinitions.MAGIC) ) {
            throw new IOException("Invalid magic");
        }
        AmqpProtocolHeader rc = new AmqpProtocolHeader();
        rc.protocolId = (short) (in.readByte() & 0xFF);
        rc.major = (short) (in.readByte() & 0xFF);
        rc.minor = (short) (in.readByte() & 0xFF);
        rc.revision = (short) (in.readByte() & 0xFF);
        return rc;
    }

    public void encode(AmqpProtocolHeader value, DataOutput out) throws IOException {
        out.write(AmqpDefinitions.MAGIC);
        out.write(value.protocolId);
        out.writeByte(value.major);
        out.write(value.minor);
        out.write(value.revision);
    }

    public int getFixedSize() {
        return 8;
    }

    public boolean isEstimatedSizeSupported() {
        return true;
    }

    public int estimatedSize(AmqpProtocolHeader value) {
        return 8;
    }

    public boolean isDeepCopySupported() {
        return true;
    }

    public AmqpProtocolHeader deepCopy(AmqpProtocolHeader value) {
        return new AmqpProtocolHeader(value);
    }
}
