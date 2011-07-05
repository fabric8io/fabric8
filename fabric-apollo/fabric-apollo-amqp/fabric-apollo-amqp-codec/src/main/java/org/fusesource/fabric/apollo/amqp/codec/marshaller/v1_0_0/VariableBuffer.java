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
public class VariableBuffer extends EncodedBuffer {

    int dataSize;

    VariableBuffer(byte formatCode, DataInput in) throws IOException {
        super(formatCode, in);
    }

    VariableBuffer(Buffer source, int offset) throws AmqpEncodingError {
        super(source, offset);
    }

    VariableBuffer(AbstractEncoded<?> encodedType) throws AmqpEncodingError {
        super(encodedType);
    }

    public VariableBuffer(Byte formatCode, Buffer source, int offset) {
        super(formatCode, source, offset);
    }

    public boolean isVariable() {
        return true;
    }

    public VariableBuffer asVariable() {
        return this;
    }

    public int getConstructorLength() {
        return 1;
    }

    public int getDataOffset() {
        return 1 + category.WIDTH;
    }

    public int getDataSize() {
        return dataSize;
    }

    public int getDataCount() {
        return 1;
    }

    public void marshalConstructor(DataOutput out) throws IOException {
        out.writeByte(formatCode);
    }

    public void marshalData(DataOutput out) throws IOException {
        out.write(encoded.data, 1 + encoded.offset, getEncodedSize() - 1);
    }

    @Override
    public Buffer fromEncoded(AbstractEncoded<?> encodedType) throws AmqpEncodingError {
        dataSize = encodedType.computeDataSize();
        Buffer rc = new Buffer(1 + category.WIDTH + dataSize);
        rc.data[0] = formatCode;
        if (category.WIDTH == 1) {
            BitUtils.setUByte(rc.data, 1, (short) dataSize);
        } else {
            BitUtils.setUInt(rc.data, 1, dataSize);
        }
        encodedType.encode(rc, getDataOffset());
        return rc;
    }

    @Override
    public Buffer fromBuffer(Buffer source, int offset) {
        return fromBuffer(null, source, offset);
    }

    @Override
    protected Buffer fromBuffer(Byte formatCode, Buffer source, int offset) {
        int formatCodeOffset = 0;
        if ( formatCode == null ) {
            formatCodeOffset = 1;
        }
        offset += source.offset;
        if (category.WIDTH == 1) {
            dataSize = 0xff & source.data[offset + formatCodeOffset];
        } else {
            dataSize = (int) BitUtils.getUInt(source.data, offset + formatCodeOffset);
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

    public Buffer unmarshal(DataInput in) throws IOException {
        Buffer rc = null;
        byte[] header = new byte[category.WIDTH];
        in.readFully(header);
        if (category.WIDTH == 1) {
            dataSize = 0xff & header[0];
        } else {
            dataSize = (int) BitUtils.getUInt(header, 0);
        }
        rc = new Buffer(1 + header.length + dataSize);
        rc.data[0] = formatCode;
        System.arraycopy(header, 0, rc.data, 1, header.length);
        if (getDataSize() > 0) {
            in.readFully(rc.data, getDataOffset(), dataSize);
        }
        return rc;
    }
}
