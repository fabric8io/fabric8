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
import org.fusesource.fabric.apollo.amqp.codec.marshaller.Encoded;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
*
*/
abstract class AbstractEncoded<V> implements Encoded<V> {
    private EncodedBuffer encoded;
    private byte formatCode;
    private FormatSubCategory category;
    protected V value = null;

    // TODO make configurable.
    // private boolean cacheEncoded = true;

    AbstractEncoded(EncodedBuffer encoded) {
        this.encoded = encoded;
        this.formatCode = encoded.formatCode;
        this.category = encoded.category;
    }

    AbstractEncoded(byte formatCode, V value) throws AmqpEncodingError {
        this.value = value;
        this.formatCode = formatCode;
        this.category = FormatSubCategory.getCategory(formatCode);
    }

    public final AmqpVersion getEncodingVersion() {
        return AmqpMarshaller.VERSION;
    }

    public final byte getEncodingFormatCode() {
        return formatCode;
    }

    public boolean isNull() {
        return formatCode == Encoder.NULL_FORMAT_CODE;
    }

    public final Buffer getBuffer() throws AmqpEncodingError {
        if (encoded == null) {
            encoded = FormatCategory.createBuffer(this);
        }
        return encoded.getBuffer();
    }

    public final V getValue() throws AmqpEncodingError {
        if (value != null || formatCode == AmqpNullMarshaller.FORMAT_CODE) {
            return value;
        }

        value = decode(encoded);
        return value;
    }

    public int getEncodedSize() throws AmqpEncodingError {
        if (encoded == null) {
            return 1 + getDataSize();
        } else {
            return encoded.getEncodedSize();
        }
    }

    public final int getDataSize() throws AmqpEncodingError {
        if (encoded != null) {
            return encoded.getDataSize();
        } else {
            switch (category.category) {
            case FIXED:
                return category.WIDTH;
            case COMPOUND:
            case VARIABLE:
            case ARRAY:
                return computeDataSize() + category.WIDTH;
            default:
                return computeDataSize();
            }
        }
    }

    public final int getDataCount() throws AmqpEncodingError {
        if (encoded != null) {
            return encoded.getDataCount();
        } else {
            return computeDataCount();
        }
    }

    public final void marshal(DataOutput out) throws IOException {
        if (encoded == null) {
            marshalFormatCode(out);
            marshalConstructor(out);
            marshalData(out);
        } else {
            encoded.marshal(out);
        }
    }

    private void marshalFormatCode(DataOutput out) throws IOException {
        if ( encoded == null ) {
            category.marshalFormatCode(this, out);
        } else {
            encoded.marshalFormatCode(out);
        }
    }

    public final void unmarshal(DataInput in) throws IOException {
        throw new UnsupportedOperationException();
    }

    public final void marshalConstructor(DataOutput out) throws IOException {
        if (encoded == null) {
            category.marshalPreData(this, out);
        } else {
            encoded.marshalConstructor(out);
        }
    }

    /**
     * Must be implemented by subclasses that have non fixed width encodings
     * to determine the size of encoded data.
     *
     * @return The size of the encoded data.
     */
    protected int computeDataSize() throws AmqpEncodingError {
        throw new IllegalStateException("unimplemented");
    }

    /**
     * Must be implemented by subclasses that have compound or array
     * encoding to determine the number of elements that are to be encoded.
     *
     * @return The number of encoded elements
     */
    protected int computeDataCount() throws AmqpEncodingError {
        throw new IllegalStateException("unimplemented");
    }

    public final void encode(Buffer encoded, int offset) throws AmqpEncodingError {
        encode(value, encoded, offset);
    }

    public abstract void encode(V decoded, Buffer encoded, int offset) throws AmqpEncodingError;

    public abstract V decode(EncodedBuffer buffer) throws AmqpEncodingError;

    abstract V unmarshalData(DataInput in) throws IOException;

    public abstract void marshalData(DataOutput out) throws IOException;
}
