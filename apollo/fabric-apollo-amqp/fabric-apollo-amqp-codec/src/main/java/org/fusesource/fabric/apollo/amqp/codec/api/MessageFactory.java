/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.codec.api;

import org.fusesource.fabric.apollo.amqp.codec.marshaller.MessageSupport;

/**
 *
 */
public class MessageFactory {

    public static DataMessage createDataMessage(Object... args) {
        return MessageSupport.createDataMessage(args);
    }

    public static SequenceMessage createSequenceMessage(Object... args) {
        return MessageSupport.createSequenceMessage(args);
    }

    public static ValueMessage createValueMessage(Object... args) {
        return MessageSupport.createValueMessage(args);
    }

    public static AnnotatedMessage createAnnotatedMessage(Object... args) {
        return MessageSupport.createAnnotatedMessage(args);
    }
}
