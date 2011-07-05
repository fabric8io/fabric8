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

import org.fusesource.fabric.apollo.amqp.codec.types.AMQPULong;

import java.io.DataOutput;
import java.math.BigInteger;

/**
 *
 */
public class DescribedConstructor {

    protected BigInteger descriptor;

    public DescribedConstructor(BigInteger descriptor) {
        this.descriptor = descriptor;
    }

    public void write(DataOutput out) throws Exception {
        out.writeByte(0x0);
        AMQPULong.write(descriptor, out);
    }

    public long size() {
        return 1 + TypeRegistry.instance().sizer().sizeOfULong(descriptor);
    }
}
