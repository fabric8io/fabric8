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

import org.fusesource.fabric.apollo.amqp.codec.api.AnnotatedMessage;
import org.fusesource.fabric.apollo.amqp.codec.interfaces.Frame;
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPFrame;
import org.fusesource.fabric.apollo.amqp.codec.types.AnnotatedMessageImpl;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;

import java.io.DataInput;

/**
 *
 */
public class FrameSupport {

    public static AMQPFrame createFrame() throws Exception {
        return createFrame(null, null);
    }

    public static AMQPFrame createFrame(Frame performative) throws Exception {
        return createFrame(performative, null);
    }

    public static AMQPFrame createFrame(Frame performative, AnnotatedMessage payload) throws Exception {
        if (performative == null) {
            return new AMQPFrame();
        } else if (payload == null) {
            DataByteArrayOutputStream out = new DataByteArrayOutputStream((int)performative.size());
            performative.write(out);
            return new AMQPFrame(out.toBuffer());
        } else {
            AnnotatedMessageImpl payloadInternal = (AnnotatedMessageImpl)payload;
            DataByteArrayOutputStream out = new DataByteArrayOutputStream((int)(performative.size() + payloadInternal.size()));
            performative.write(out);
            payloadInternal.write(out);
            return new AMQPFrame(out.toBuffer());
        }
    }

    public static Frame getPerformative(DataInput in) throws Exception {
        return (Frame)TypeReader.read(in);
    }

    public static AnnotatedMessage getPayload(DataInput in) throws Exception {
        return MessageSupport.readAnnotatedMessage(in);
    }
}
