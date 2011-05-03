/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.codec.marshaller;

import java.io.DataOutput;
import java.io.IOException;

import org.fusesource.fabric.apollo.amqp.codec.marshaller.AmqpEncodingError;
import org.fusesource.hawtbuf.Buffer;

public interface Encoded<E> extends Encoding {

    public boolean isNull();

    public int getEncodedSize() throws AmqpEncodingError;

    public int getDataSize() throws AmqpEncodingError;

    public int getDataCount() throws AmqpEncodingError;

    public E getValue() throws AmqpEncodingError;

    public Buffer getBuffer() throws AmqpEncodingError;

    public void encode(Buffer target, int offset) throws AmqpEncodingError;

    public void marshal(DataOutput out) throws IOException;

    public void marshalData(DataOutput out) throws IOException;

    public void marshalConstructor(DataOutput out) throws IOException;
}
