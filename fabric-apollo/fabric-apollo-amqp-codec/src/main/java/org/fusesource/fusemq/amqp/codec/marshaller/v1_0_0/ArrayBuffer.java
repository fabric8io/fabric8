/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fusemq.amqp.codec.marshaller.v1_0_0;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.fusemq.amqp.codec.marshaller.AmqpEncodingError;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
*
*/
public class ArrayBuffer extends CompoundBuffer {
    private byte constructor;

    ArrayBuffer(byte formatCode, DataInput in) throws IOException {
        super(formatCode, in);
    }

    ArrayBuffer(Buffer source, int offset) throws AmqpEncodingError {
        super(source, offset);
    }

    ArrayBuffer(AbstractEncoded<?> encodedType) throws AmqpEncodingError {
        super(encodedType);
    }

    public ArrayBuffer(Byte formatCode, Buffer source, int offset) {
        super(formatCode, source, offset);
    }

    public final boolean isArray() {
        return true;
    }

    public final ArrayBuffer asArray() {
        return this;
    }

    public int getConstructorLength() {
        return 1;
    }

    public int getDataOffset() {
        return 1 + 2 * category.WIDTH;
    }

    public int getDataSize() throws AmqpEncodingError {
        return dataSize;
    }

    public int getDataCount() throws AmqpEncodingError {
        return dataCount;
    }

    public void marshalConstructor(DataOutput out) throws IOException {
        out.write(encoded.data, encoded.offset, getConstructorLength());
    }

    public void marshalData(DataOutput out) throws IOException {
        if (getDataSize() > 0) {
            out.write(encoded.data, encoded.offset + getDataOffset(), getDataSize());
        }
    }

    @Override
    public Buffer unmarshal(DataInput in) throws IOException {
        Buffer rc = super.unmarshal(in);
        constructor = (byte)(0xff & rc.data[1 + category.WIDTH * 2 ]);
        return rc;
    }

    @Override
    public Buffer fromEncoded(AbstractEncoded<?> encodedType) throws AmqpEncodingError {
        Buffer rc = new Buffer(encodedType.getEncodedSize());
        encodedType.encode(rc, 0);
        return rc;
    }

    @Override
    public Buffer fromBuffer(Byte formatCode, Buffer source, int offset) {
        Buffer rc = super.fromBuffer(formatCode, source, offset);
        constructor = (byte)(0xff & rc.data[1 + category.WIDTH * 2]);
        return rc;
    }

    @Override
    public Buffer fromBuffer(Buffer source, int offset) {
        return fromBuffer(null, source, offset);
    }

    @Override
    EncodedBuffer[] constituents() {
        if (constituents == null) {
            EncodedBuffer[] cb = new EncodedBuffer[getDataCount()];
            Buffer b = getBuffer();
            int offset = getDataOffset();
            byte formatCode = b.data[offset];
            offset++;
            for (int i = 0; i < cb.length; i++) {
                cb[i] = FormatCategory.createBuffer(formatCode, b, offset);
                offset += cb[i].getEncodedSize() - 1;
            }
            this.constituents = cb;
        }
        return constituents;

    }
}
