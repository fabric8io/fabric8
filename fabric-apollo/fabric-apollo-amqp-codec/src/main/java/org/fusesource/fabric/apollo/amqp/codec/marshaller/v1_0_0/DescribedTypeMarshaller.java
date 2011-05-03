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

public interface DescribedTypeMarshaller<D extends AmqpType<?, ?>> {

    public D decodeDescribedType(AmqpType<?, ?> descriptor, DescribedBuffer buffer) throws AmqpEncodingError;
}
