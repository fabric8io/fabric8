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

import org.fusesource.fabric.apollo.amqp.codec.interfaces.AmqpType;
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPSymbol;
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPULong;

import java.io.DataInput;

/**
 *
 */
public class TypeReader {

    public static AmqpType read(DataInput in) throws Exception {
        byte formatCode = in.readByte();
        if (formatCode == -1) {
            return null;
        }

        if (formatCode == TypeRegistry.DESCRIBED_FORMAT_CODE) {
            AmqpType descriptor = read(in);

            AmqpType rc = null;

            if (descriptor instanceof AMQPULong ) {
                rc = (AmqpType) TypeRegistry.instance().getFormatCodeMap().get(((AMQPULong) descriptor).getValue()).newInstance();
            } else if (descriptor instanceof AMQPSymbol) {
                rc = (AmqpType) TypeRegistry.instance().getSymbolicCodeMap().get(((AMQPSymbol) descriptor).getValue()).newInstance();
            } else {
                throw new IllegalArgumentException("Unknown AMQP descriptor type");
            }
            if (rc != null) {
                rc.read(formatCode, in);
            }
            return rc;
        } else if (formatCode == TypeRegistry.NULL_FORMAT_CODE) {
            return null;
        }

        AmqpType primitive = (AmqpType) TypeRegistry.instance().getPrimitiveFormatCodeMap().get(formatCode).newInstance();
        if (primitive != null) {
            primitive.read(formatCode, in);
        }
        return primitive;
    }
}
