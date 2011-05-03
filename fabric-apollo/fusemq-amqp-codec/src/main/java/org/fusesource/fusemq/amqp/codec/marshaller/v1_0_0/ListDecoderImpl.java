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

import org.fusesource.fusemq.amqp.codec.marshaller.AmqpEncodingError;
import org.fusesource.fusemq.amqp.codec.types.AmqpType;
import org.fusesource.fusemq.amqp.codec.types.IAmqpList;

import java.io.DataInput;
import java.io.IOException;

/**
*
*/
class ListDecoderImpl implements ListDecoder<AmqpType<?, ?>> {

    public final IAmqpList<AmqpType<?, ?>> decode(EncodedBuffer[] constituents) {
        IAmqpList<AmqpType<?, ?>> rc = new IAmqpList.ArrayBackedList<AmqpType<?, ?>>(new AmqpType<?, ?>[constituents.length]);
        for (int i = 0; i < constituents.length; i++) {
            rc.set(i, Encoder.MARSHALLER.decodeType(constituents[i]));
        }
        return rc;
    }

    public final IAmqpList<AmqpType<?, ?>> unmarshalType(int dataCount, int dataSize, DataInput in) throws IOException, AmqpEncodingError {
        IAmqpList<AmqpType<?, ?>> rc = new IAmqpList.ArrayBackedList<AmqpType<?, ?>>(new AmqpType<?, ?>[dataCount]);
        for (int i = 0; i < dataCount; i++) {
            rc.set(i, Encoder.MARSHALLER.unmarshalType(in));
        }
        return rc;
    }
}
