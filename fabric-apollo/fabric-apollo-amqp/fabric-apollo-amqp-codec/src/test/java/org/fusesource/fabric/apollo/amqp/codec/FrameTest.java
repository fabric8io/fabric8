/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.codec;

import org.fusesource.fabric.apollo.amqp.codec.api.AnnotatedMessage;
import org.fusesource.fabric.apollo.amqp.codec.api.MessageFactory;
import org.fusesource.fabric.apollo.amqp.codec.api.ValueMessage;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.FrameSupport;
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPString;
import org.fusesource.fabric.apollo.amqp.codec.types.AmqpFrame;
import org.fusesource.fabric.apollo.amqp.codec.types.Begin;
import org.fusesource.fabric.apollo.amqp.codec.types.Transfer;
import org.fusesource.hawtbuf.Buffer;
import org.junit.Test;

import java.io.DataInput;

import static org.fusesource.fabric.apollo.amqp.codec.TestSupport.encodeDecode;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class FrameTest {

    @Test
    public void testBeginFrame() throws Exception {
        Begin in = new Begin();
        in.setIncomingWindow(10L);
        in.setOutgoingWindow(10L);
        in.setNextOutgoingID(0L);

        Begin out = encodeDecode(in);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testTransferFrame() throws Exception {
        ValueMessage message = MessageFactory.createValueMessage(new AMQPString("HelloWorld!"));
        Transfer transfer = new Transfer(0L, 0L, Buffer.ascii("0").buffer());
        AnnotatedMessage annotatedMessage = MessageFactory.createAnnotatedMessage(message);

        AmqpFrame frame = FrameSupport.createFrame(transfer, annotatedMessage);
        DataInput in = frame.dataInput();

        Transfer outTransfer = (Transfer)FrameSupport.getPerformative(in);
        AnnotatedMessage out = FrameSupport.getPayload(in);
        assertEquals(transfer.toString(), outTransfer.toString());
        assertEquals(annotatedMessage.toString(), out.toString());
    }
}
