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

public class UnexpectedTypeException extends AmqpEncodingError {

    private static final long serialVersionUID = 4306936382810257248L;

    public UnexpectedTypeException(String msg) {
        super(msg);
    }
}
