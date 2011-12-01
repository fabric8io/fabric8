/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.api;

import org.fusesource.fabric.apollo.amqp.codec.types.Source;
import org.fusesource.fabric.apollo.amqp.codec.types.Target;

/**
 *
 */
public class AMQPSupport {

    public Source toSource(org.fusesource.fabric.apollo.amqp.codec.interfaces.Source source) {
        return (Source) source;
    }

    public Target toTarget(org.fusesource.fabric.apollo.amqp.codec.interfaces.Target target) {
        return (Target) target;
    }

}
