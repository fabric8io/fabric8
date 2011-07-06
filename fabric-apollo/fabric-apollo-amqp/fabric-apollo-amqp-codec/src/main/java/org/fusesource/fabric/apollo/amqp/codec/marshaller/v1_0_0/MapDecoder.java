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

/**
*
*/
public interface MapDecoder<K extends AmqpType<?, ?>, V extends AmqpType<?, ?>> {
    public IAmqpMap<K, V> decode(EncodedBuffer[] constituents) throws AmqpEncodingError;

    public IAmqpMap<K, V> unmarshalType(int dataCount, int dataSize, DataInput in) throws IOException, AmqpEncodingError;
}
