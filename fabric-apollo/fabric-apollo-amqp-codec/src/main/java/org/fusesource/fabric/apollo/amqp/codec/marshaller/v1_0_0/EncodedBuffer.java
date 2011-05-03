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
import org.fusesource.fabric.apollo.amqp.codec.marshaller.AmqpVersion;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
*
*/
public abstract class EncodedBuffer {

    protected final byte formatCode;
    protected final FormatSubCategory category;
    protected Buffer encoded;

    EncodedBuffer(byte formatCode, DataInput in) throws IOException {
        this.formatCode = formatCode;
        this.category = FormatSubCategory.getCategory(formatCode);
        this.encoded = unmarshal(in);
    }

    EncodedBuffer(AbstractEncoded<?> encodedType) throws AmqpEncodingError {
        this.formatCode = encodedType.getEncodingFormatCode();
        this.category = FormatSubCategory.getCategory(formatCode);
        this.encoded = fromEncoded(encodedType);
    }

    EncodedBuffer(Buffer source, int offset) throws AmqpEncodingError {
        this.formatCode = source.get(offset);
        this.category = FormatSubCategory.getCategory(formatCode);
        this.encoded = fromBuffer(source, offset);
    }

    public EncodedBuffer(Byte formatCode, Buffer source, int offset) {
        this.formatCode = formatCode;
        this.category = FormatSubCategory.getCategory(formatCode);
        this.encoded = fromBuffer(formatCode, source, offset);
    }

    public final int getEncodedSize() {
        return encoded.getLength();
    }

    public final Buffer getBuffer() {
        return encoded;
    }

    public final void marshal(DataOutput out) throws IOException {
        out.write(encoded.data, encoded.offset, encoded.length);
    }

    public final byte getEncodingFormatCode() {
        return formatCode;
    }

    public final AmqpVersion getEncodingVersion() {
        return AmqpMarshaller.VERSION;
    }

    protected abstract Buffer fromEncoded(AbstractEncoded<?> encodedType) throws AmqpEncodingError;

    protected abstract Buffer fromBuffer(Buffer buffer, int offset) throws AmqpEncodingError;

    protected abstract Buffer fromBuffer(Byte formatCode, Buffer source, int offset);

    protected abstract Buffer unmarshal(DataInput in) throws IOException;

    public abstract int getConstructorLength();

    public abstract int getDataOffset();

    public abstract int getDataSize() throws AmqpEncodingError;

    public abstract int getDataCount() throws AmqpEncodingError;

    public abstract void marshalConstructor(DataOutput out) throws IOException;

    public abstract void marshalData(DataOutput out) throws IOException;

    public boolean isFixed() {
        return false;
    }

    public FixedBuffer asFixed() {
        throw new AmqpEncodingError(FormatSubCategory.getCategory(formatCode).name());
    }

    public boolean isVariable() {
        return false;
    }

    public VariableBuffer asVariable() {
        throw new AmqpEncodingError(FormatSubCategory.getCategory(formatCode).name());
    }

    public boolean isArray() {
        return false;
    }

    public ArrayBuffer asArray() {
        throw new AmqpEncodingError(FormatSubCategory.getCategory(formatCode).name());
    }

    public boolean isCompound() {
        return false;
    }

    public CompoundBuffer asCompound() {
        throw new AmqpEncodingError(FormatSubCategory.getCategory(formatCode).name());
    }

    public boolean isDescribed() {
        return false;
    }

    public DescribedBuffer asDescribed() {
        throw new AmqpEncodingError(FormatSubCategory.getCategory(formatCode).name());
    }

    public void marshalFormatCode(DataOutput out) {
        //To change body of created methods use File | Settings | File Templates.
    }
}
