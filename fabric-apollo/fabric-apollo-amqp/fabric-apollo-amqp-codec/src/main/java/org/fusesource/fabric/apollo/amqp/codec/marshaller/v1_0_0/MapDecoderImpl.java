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

import org.fusesource.fabric.apollo.amqp.codec.marshaller.AmqpEncodingError;
import org.fusesource.fabric.apollo.amqp.codec.types.AmqpType;
import org.fusesource.fabric.apollo.amqp.codec.types.IAmqpMap;

import java.io.DataInput;
import java.io.IOException;
import java.util.HashMap;

/**
*
*/
class MapDecoderImpl implements MapDecoder<AmqpType<?, ?>, AmqpType<?, ?>> {

    public final IAmqpMap<AmqpType<?, ?>, AmqpType<?, ?>> decode(EncodedBuffer[] constituents) {
        IAmqpMap.AmqpWrapperMap<AmqpType<?, ?>, AmqpType<?, ?>> rc = new IAmqpMap.AmqpWrapperMap<AmqpType<?, ?>, AmqpType<?, ?>>(new HashMap<AmqpType<?, ?>, AmqpType<?, ?>>());
        if (constituents.length % 2 != 0) {
            throw new AmqpEncodingError("Invalid number of compound constituents: " + constituents.length);
        }

        for (int i = 0; i < constituents.length; i += 2) {
            rc.put(Encoder.MARSHALLER.decodeType(constituents[i]), Encoder.MARSHALLER.decodeType(constituents[i + 1]));
        }
        return rc;
    }

    public final IAmqpMap<AmqpType<?, ?>, AmqpType<?, ?>> unmarshalType(int dataCount, int dataSize, DataInput in) throws IOException, AmqpEncodingError {
        IAmqpMap.AmqpWrapperMap<AmqpType<?, ?>, AmqpType<?, ?>> rc = new IAmqpMap.AmqpWrapperMap<AmqpType<?, ?>, AmqpType<?, ?>>(new HashMap<AmqpType<?, ?>, AmqpType<?, ?>>());
        if (dataCount % 2 != 0) {
            throw new AmqpEncodingError("Invalid number of compound constituents: " + dataCount);
        }

        for (int i = 0; i < dataCount; i += 2) {
            rc.put(Encoder.MARSHALLER.unmarshalType(in), Encoder.MARSHALLER.unmarshalType(in));
        }
        return rc;
    }
}
