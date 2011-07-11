/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.codec.api;

import org.fusesource.fabric.apollo.amqp.codec.marshaller.MessageSupport;

/**
 *
 */
public class MessageFactory {

    public static DataMessage createDataMessage() {
        return MessageSupport.createDataMessage();
    }

    public static SequenceMessage createSequenceMessage() {
        return MessageSupport.createSequenceMessage();
    }

    public static ValueMessage createValueMessage() {
        return MessageSupport.createValueMessage();
    }

    public static AnnotatedMessage createAnnotatedMessage() {
        return MessageSupport.createAnnotatedMessage();
    }
}
