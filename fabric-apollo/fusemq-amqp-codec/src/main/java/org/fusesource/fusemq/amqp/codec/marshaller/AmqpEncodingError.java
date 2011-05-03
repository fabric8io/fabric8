/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fusemq.amqp.codec.marshaller;

public class AmqpEncodingError extends Error {
    private static final long serialVersionUID = 1L;

    public AmqpEncodingError(String msg) {
        super(msg);
    }

    public AmqpEncodingError(String msg, Throwable cause) {
        super(msg, cause);
    }
}
