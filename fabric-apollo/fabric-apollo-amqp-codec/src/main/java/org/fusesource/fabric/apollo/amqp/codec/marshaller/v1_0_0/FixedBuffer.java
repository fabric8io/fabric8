/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.codec.marshaller.v1_0_0;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.AmqpEncodingError;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
*
*/
public class FixedBuffer extends EncodedBuffer {

    FixedBuffer(byte formatCode, DataInput in) throws IOException {
        super(formatCode, in);
    }

    FixedBuffer(Buffer source, int offset) throws AmqpEncodingError {
        super(source, offset);
    }

    FixedBuffer(AbstractEncoded<?> encodedType) throws AmqpEncodingError {
        super(encodedType);
    }

    public FixedBuffer(Byte formatCode, Buffer source, int offset) {
        super(formatCode, source, offset);
    }

    public final boolean isFixed() {
        return true;
    }

    public final FixedBuffer asFixed() {
        return this;
    }

    public final int getConstructorLength() {
        return 1;
    }

    public final int getDataOffset() {
        return 1;
    }

    public final int getDataSize() throws AmqpEncodingError {
        return category.WIDTH;
    }

    public final int getDataCount() throws AmqpEncodingError {
        return 1;
    }

    public final void marshalConstructor(DataOutput out) throws IOException {
        out.writeByte(formatCode);
    }

    public final void marshalData(DataOutput out) throws IOException {
        if (getDataSize() > 0) {
            out.write(encoded.data, 1, category.WIDTH);
        }
    }

    protected final Buffer unmarshal(DataInput in) throws IOException {
        Buffer rc = null;
        if (category.WIDTH > 0) {
            rc = new Buffer(1 + category.WIDTH);
            in.readFully(rc.data, 1, category.WIDTH);
        } else {
            rc = new Buffer(1);
        }
        rc.data[0] = formatCode;
        return rc;
    }

    @Override
    protected final Buffer fromBuffer(Buffer source, int offset) {
        return fromBuffer(null, source, offset);
    }

    @Override
    protected Buffer fromBuffer(Byte formatCode, Buffer source, int offset) {
        int formatCodeOffset = 1;
        if ( formatCode == null ) {
            formatCodeOffset = 0;
        }
        Buffer rc = new Buffer(1 + category.WIDTH);
        if ( formatCode != null ) {
            System.arraycopy(source.data, source.offset + offset, rc.data, 1, rc.length - 1);
            rc.data[0] = formatCode;
        } else {
            System.arraycopy(source.data, source.offset + offset, rc.data, 0, rc.length);
        }
        return rc;
    }

    @Override
    protected final Buffer fromEncoded(AbstractEncoded<?> encodedType) throws AmqpEncodingError {
        Buffer rc = new Buffer(1 + category.WIDTH);
        rc.data[0] = formatCode;
        encodedType.encode(rc, 1);
        return rc;
    }
}
