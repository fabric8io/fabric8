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
import org.fusesource.fabric.apollo.amqp.codec.marshaller.Encoded;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
*
*/
public abstract class DescribedEncoded<V> extends AbstractEncoded<V> {

    Encoded<V> describedEncoded;
    EncodedBuffer descriptor;

    DescribedEncoded(DescribedBuffer encoded) {
        super(encoded);
    }

    DescribedEncoded(Encoded<V> value) {
        super((byte) 0x00, value.getValue());
        describedEncoded = value;
        descriptor = getDescriptor();
    }

    public final void encode(V decoded, Buffer encoded, int offset) throws AmqpEncodingError {
        System.arraycopy(descriptor.encoded.data, descriptor.encoded.offset, encoded.data, encoded.offset + offset, descriptor.encoded.length);
        describedEncoded.encode(encoded, offset + descriptor.encoded.length);
    }

    public final V decode(EncodedBuffer buffer) throws AmqpEncodingError {
        // TODO remove cast?
        describedEncoded = decodeDescribed(((DescribedBuffer) buffer).describedBuffer);
        return describedEncoded.getValue();
    }

    public final V unmarshalData(DataInput in) throws IOException {
        describedEncoded = unmarshalDescribed(in);
        return describedEncoded.getValue();
    }

    public final void marshalData(DataOutput out) throws IOException {
        descriptor.marshal(out);
        describedEncoded.marshal(out);
    }

    /**
     * Must be implemented by subclasses that have non fixed width encodings
     * to determine the size of encoded data.
     *
     * @return The size of the encoded data.
     */
    protected final int computeDataSize() throws AmqpEncodingError {
        int rc = descriptor.getEncodedSize();
        rc += describedEncoded.getEncodedSize();
        return rc;
    }

    /**
     * Must be implemented by subclasses that have compound or array
     * encoding to determine the number of elements that are to be encoded.
     *
     * @return The number of encoded elements
     */
    protected final int computeDataCount() throws AmqpEncodingError {
        return 1;
    }

    protected abstract EncodedBuffer getDescriptor();

    protected abstract Encoded<V> decodeDescribed(EncodedBuffer encoded) throws AmqpEncodingError;

    protected abstract Encoded<V> unmarshalDescribed(DataInput in) throws IOException, AmqpEncodingError;
}
