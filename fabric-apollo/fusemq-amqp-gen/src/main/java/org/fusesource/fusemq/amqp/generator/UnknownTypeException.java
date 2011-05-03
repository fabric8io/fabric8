/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fusemq.amqp.generator;

public class UnknownTypeException extends Exception {

    private static final long serialVersionUID = 4106181403332534392L;

    public UnknownTypeException(String message) {
        super(message);
    }
}
