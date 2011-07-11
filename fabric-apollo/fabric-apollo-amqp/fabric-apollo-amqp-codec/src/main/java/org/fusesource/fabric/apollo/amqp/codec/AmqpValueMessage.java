/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.codec;

import org.fusesource.fabric.apollo.amqp.codec.types.AmqpValue;

import java.io.DataOutput;

/**
 *
 */
public class AmqpValueMessage extends BareMessage<AmqpValue> {

    public AmqpValueMessage() {
        data = new AmqpValue();
    }

    @Override
    public long dataSize() {
        return data.size();
    }

    @Override
    public void dataWrite(DataOutput out) throws Exception {
        data.write(out);
    }
}
