/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.codec;

import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPType;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;

/**
 *
 */
public class Codec {

    public static Buffer toBuffer(AMQPType type) throws Exception {
        DataByteArrayOutputStream out = new DataByteArrayOutputStream((int) type.size());
        type.write(out);
        return out.toBuffer();
    }
}
