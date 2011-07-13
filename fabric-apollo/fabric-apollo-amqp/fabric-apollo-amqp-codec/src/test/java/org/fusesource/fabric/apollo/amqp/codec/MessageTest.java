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
import org.fusesource.fabric.apollo.amqp.codec.api.DataMessage;
import org.fusesource.fabric.apollo.amqp.codec.api.SequenceMessage;
import org.fusesource.fabric.apollo.amqp.codec.types.*;
import org.fusesource.hawtbuf.Buffer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static org.fusesource.fabric.apollo.amqp.codec.TestSupport.encodeDecode;
import static org.fusesource.fabric.apollo.amqp.codec.api.MessageFactory.createAnnotatedMessage;
import static org.fusesource.fabric.apollo.amqp.codec.api.MessageFactory.createDataMessage;
import static org.fusesource.fabric.apollo.amqp.codec.marshaller.MessageSupport.getFooter;
import static org.fusesource.fabric.apollo.amqp.codec.marshaller.MessageSupport.toBuffer;
import static org.fusesource.hawtbuf.Buffer.ascii;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class MessageTest {

    @Test
    public void testEncodeDecodeEmptyMessage() throws Exception {
        AnnotatedMessage in = createAnnotatedMessage();
        AnnotatedMessage out = encodeDecode(in);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testEncodeDecodeSimpleDataMessage() throws Exception {
        DataMessage message = createDataMessage();
        message.getData().add(new Data());
        message.getData().get(0).setValue(ascii("Hello world!").buffer());

        AnnotatedMessage in = createAnnotatedMessage();
        in.setMessage(message);

        AnnotatedMessage out = encodeDecode(in);

        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testEncodeDecodeLessSimpleMessage() throws Exception {
        AnnotatedMessage in = getMessage();
        AnnotatedMessage out = encodeDecode(in);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testScanMessageForSection() throws Exception {
        AnnotatedMessage in = getMessage();
        Buffer buffer = toBuffer(in);
        Footer footer = getFooter(buffer);
        System.out.printf("Got : %s\n", footer);
        assertNotNull(footer);
    }

    private static AnnotatedMessage getMessage() {
        SequenceMessage msg = new SequenceMessageImpl();

        ArrayList<AMQPString> payload1 = new ArrayList<AMQPString>();
        ArrayList<AMQPString> payload2 = new ArrayList<AMQPString>();

        for (int i = 0; i < 10; i++) {
            payload1.add(new AMQPString("payload item " + i));
            payload2.add(new AMQPString("and payload item " + (i + 10)));
        }
        AmqpSequence seq1 = new AmqpSequence();
        seq1.setValue(payload1);
        AmqpSequence seq2 = new AmqpSequence();
        seq2.setValue(payload2);
        msg.getData().add(seq1);
        msg.getData().add(seq2);
        msg.setProperties(new Properties());
        msg.getProperties().setAbsoluteExpiryTime(new Date());
        msg.getProperties().setUserID(ascii("foo").buffer());
        msg.getProperties().setTo(new AMQPString("nowhere"));
        msg.getProperties().setCreationTime(new Date());

        AnnotatedMessage in = createAnnotatedMessage();
        in.setMessage(msg);
        in.setDeliveryAnnotations(new DeliveryAnnotations());
        in.getDeliveryAnnotations().setValue(new HashMap<AMQPSymbol, AMQPString>());
        in.getDeliveryAnnotations().getValue().put(new AMQPSymbol(Footer.CONSTRUCTOR.getBuffer()), new AMQPString("Hi!"));
        in.setHeader(new Header());
        in.getHeader().setDurable(true);
        in.getHeader().setDeliveryCount(0L);
        in.setFooter(new Footer());
        in.getFooter().setValue(new HashMap<AMQPSymbol, AMQPString>());
        in.getFooter().getValue().put(new AMQPSymbol(Buffer.ascii("test").buffer()), new AMQPString("value"));
        return in;
    }


}
