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
class NullEncoded<V> extends AbstractEncoded<V> {

    private static FixedBuffer nb = new FixedBuffer(new Buffer(new byte[] { Encoder.NULL_FORMAT_CODE }), 0);

    NullEncoded() {
        super(nb);
    }

    @Override
    public V decode(EncodedBuffer buffer) throws AmqpEncodingError {
        return null;
    }

    @Override
    public void encode(V decoded, Buffer encoded, int offset) throws AmqpEncodingError {
    }

    @Override
    public void marshalData(DataOutput out) throws IOException {
    }

    @Override
    V unmarshalData(DataInput in) throws IOException {
        return null;
    }
}
