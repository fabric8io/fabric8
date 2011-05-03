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
import org.fusesource.fabric.apollo.amqp.codec.BitUtils;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.AmqpEncodingError;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
*
*/
public class CompoundBuffer extends EncodedBuffer {
    protected int dataSize;
    protected int dataCount;
    protected EncodedBuffer[] constituents;

    CompoundBuffer(byte formatCode, DataInput in) throws IOException {
        super(formatCode, in);
    }

    CompoundBuffer(Buffer source, int offset) throws AmqpEncodingError {
        super(source, offset);
    }

    CompoundBuffer(AbstractEncoded<?> encodedType) throws AmqpEncodingError {
        super(encodedType);
    }

    public CompoundBuffer(Byte formatCode, Buffer source, int offset) {
        super(formatCode, source, offset);
    }

    public boolean isCompound() {
        return true;
    }

    public CompoundBuffer asCompound() {
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
        out.writeByte(formatCode);
    }

    public void marshalData(DataOutput out) throws IOException {
        out.write(encoded.data, 1 + encoded.offset, getEncodedSize() - 1);
    }

    public Buffer unmarshal(DataInput in) throws IOException {
        Buffer header = new Buffer(category.WIDTH);
        header.readFrom(in);
        if (category.WIDTH == 1) {
            dataSize = 0xff & header.data[0];
        } else {
            dataSize = (int) BitUtils.getUInt(header.data, 0);
        }
        Buffer rc = new Buffer(1 + header.length + dataSize);
        rc.data[0] = formatCode;
        System.arraycopy(header.data, 0, rc.data, 1, header.length);
        in.readFully(rc.data, getDataOffset() - category.WIDTH, getDataSize());
        if (category.WIDTH == 1) {
            dataCount = 0xff & rc.data[1 + category.WIDTH];
        } else {
            dataCount = (int) BitUtils.getUInt(rc.data, 1 + category.WIDTH);
        }
        return rc;
    }

    @Override
    public Buffer fromEncoded(AbstractEncoded<?> encodedType) throws AmqpEncodingError {
        dataSize = encodedType.computeDataSize();
        dataCount = encodedType.computeDataCount();

        Buffer rc = new Buffer(1 + category.WIDTH + dataSize);
        rc.data[1] = formatCode;
        if (category.WIDTH == 1) {
            BitUtils.setUByte(rc.data, 1, (short) dataSize);
            BitUtils.setUByte(rc.data, 2, (short) dataCount);
        } else {
            BitUtils.setUInt(rc.data, 1, dataSize);
            BitUtils.setUInt(rc.data, 1 + category.WIDTH, dataCount);
        }
        encodedType.encode(rc, 0);
        return rc;
    }

    @Override
    public Buffer fromBuffer(Buffer source, int offset) {
        return fromBuffer(null, source, offset);
    }

    @Override
    protected Buffer fromBuffer(Byte formatCode, Buffer source, int offset) {
        int formatCodeOffset = 0;
        if (formatCode == null ) {
            formatCodeOffset = 1;
        }
        offset = offset + source.offset;
        if (category.WIDTH == 1) {
            dataSize = 0xff & source.data[formatCodeOffset + offset];
            dataCount = 0xff & source.data[formatCodeOffset + offset + 1];
        } else {
            dataSize = (int) BitUtils.getUInt(source.data, formatCodeOffset + offset);
            dataCount = (int) BitUtils.getUInt(source.data, formatCodeOffset + offset + category.WIDTH);
        }
        Buffer rc = new Buffer(1 + category.WIDTH + dataSize);
        if ( formatCode != null ) {
            System.arraycopy(source.data, offset, rc.data, 1, rc.length - 1);
            rc.data[0] = formatCode;
        } else {
            System.arraycopy(source.data, offset, rc.data, 0, rc.length);
        }
        return rc;
    }

    EncodedBuffer[] constituents() {
        if (constituents == null) {
            EncodedBuffer[] cb = new EncodedBuffer[getDataCount()];
            Buffer b = getBuffer();
            int offset = getDataOffset();
            for (int i = 0; i < cb.length; i++) {
                cb[i] = FormatCategory.createBuffer(b, offset);
                offset += cb[i].getEncodedSize();
            }
            this.constituents = cb;
        }
        return constituents;
    }
}
