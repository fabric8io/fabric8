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
import org.fusesource.fabric.apollo.amqp.codec.types.IAmqpList;

import java.io.DataInput;
import java.io.IOException;

/**
*
*/
public interface ListDecoder<E extends AmqpType<?, ?>> {
    public IAmqpList<E> decode(EncodedBuffer[] constituents) throws AmqpEncodingError;

    public IAmqpList<E> unmarshalType(int dataCount, int dataSize, DataInput in) throws IOException, AmqpEncodingError;
}
