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

import org.fusesource.fusemq.amqp.jaxb.schema.Error;

public class AmqpError {

    String type;
    String name;
    String value;

    public void parseFromError(Error error)
    {
        type = error.getType();
        name = error.getName();
        value = error.getValue();
        //TODO error.getDoc()
    }
}
