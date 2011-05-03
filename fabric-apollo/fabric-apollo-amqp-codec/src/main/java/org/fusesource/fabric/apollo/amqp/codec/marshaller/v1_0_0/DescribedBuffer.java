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
public class DescribedBuffer extends EncodedBuffer {

    EncodedBuffer descriptor;
    EncodedBuffer describedBuffer;

    DescribedBuffer(byte formatCode, DataInput in) throws IOException {
        super(formatCode, in);
    }

    DescribedBuffer(Buffer source, int offset) throws AmqpEncodingError {
        super(source, offset);
    }

    DescribedBuffer(AbstractEncoded<?> encodedType) throws AmqpEncodingError {
        super(encodedType);
    }

    public DescribedBuffer(Byte formatCode, Buffer source, int offset) {
        super(formatCode, source, offset);
    }

    public final boolean isDescribed() {
        return true;
    }

    public final DescribedBuffer asDescribed() {
        return this;
    }

    public EncodedBuffer getDescriptorBuffer() {
        return descriptor;
    }

    public EncodedBuffer getDescribedBuffer() {
        return describedBuffer;
    }

    public int getConstructorLength() {
        return 1 + descriptor.getEncodedSize() + describedBuffer.getConstructorLength();
    }

    public int getDataOffset() {
        return 1 + descriptor.getEncodedSize() + describedBuffer.getDataOffset();
    }

    public int getDataSize() throws AmqpEncodingError {
        return describedBuffer.getDataSize();
    }

    public int getDataCount() throws AmqpEncodingError {
        return describedBuffer.getDataCount();
    }

    public void marshalConstructor(DataOutput out) throws IOException {
        out.write(encoded.data, 0, getConstructorLength());
    }

    public void marshalData(DataOutput out) throws IOException {
        if (getDataSize() > 0) {
            out.write(encoded.data, getDataOffset(), getDataSize());
        }
    }

    protected final Buffer unmarshal(DataInput in) throws IOException {
        descriptor = FormatCategory.createBuffer(in.readByte(), in);
        describedBuffer = FormatCategory.createBuffer(in.readByte(), in);
        return initializeEncoded();
    }

    @Override
    protected final Buffer fromEncoded(AbstractEncoded<?> encodedType) throws AmqpEncodingError {
        throw new UnsupportedOperationException();
    }

    @Override
    protected final Buffer fromBuffer(Buffer buffer, int offset) throws AmqpEncodingError {
        return fromBuffer(null, buffer, offset);
    }

    @Override
    protected final Buffer fromBuffer(Byte formatCode, Buffer buffer, int offset) throws AmqpEncodingError {
        if ( formatCode == null ) {
            descriptor = FormatCategory.createBuffer(buffer, 1 + offset);
            describedBuffer = FormatCategory.createBuffer(buffer, 1 + offset + descriptor.getEncodedSize());
        } else {
            descriptor = FormatCategory.createBuffer(buffer.get(offset), buffer, 1 + offset);
            describedBuffer = FormatCategory.createBuffer(buffer.get(offset + descriptor.getEncodedSize()), buffer, 1 + offset + descriptor.getEncodedSize());
        }
        return initializeEncoded();
    }

    private Buffer initializeEncoded() {
        Buffer rc = new Buffer(1 + descriptor.getEncodedSize() + describedBuffer.getEncodedSize());
        rc.data[0] = Encoder.DESCRIBED_FORMAT_CODE;
        // TODO we should be able to let the described type decode into our
        // buffer
        // which would save a potoentially large copy.
        System.arraycopy(descriptor.getBuffer().data, 0, rc.data, 1, descriptor.getEncodedSize());
        System.arraycopy(describedBuffer.getBuffer().data, 0, rc.data, 1 + descriptor.getEncodedSize(), describedBuffer.getEncodedSize());
        return rc;
    }
}
