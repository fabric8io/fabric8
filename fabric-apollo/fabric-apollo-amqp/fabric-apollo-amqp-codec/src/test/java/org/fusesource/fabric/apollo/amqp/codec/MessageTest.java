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

import org.fusesource.fabric.apollo.amqp.codec.marshaller.MessageDecoder;
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPString;
import org.fusesource.fabric.apollo.amqp.codec.types.AmqpSequence;
import org.fusesource.fabric.apollo.amqp.codec.types.Header;
import org.fusesource.fabric.apollo.amqp.codec.types.Properties;
import org.fusesource.hawtbuf.Buffer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;

import static org.fusesource.fabric.apollo.amqp.codec.TestSupport.string;
import static org.fusesource.hawtbuf.Buffer.ascii;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class MessageTest {

    @Test
    public void testEncodeDecodeEmptyMessage() throws Exception {
        AnnotatedMessage in = new AnnotatedMessage();
        Buffer encoded = in.encode();
        AnnotatedMessage out = MessageDecoder.decodeAnnotatedMessage(encoded);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testEncodeDecodeSimpleMessage() throws Exception {

        AnnotatedMessage in = new AnnotatedMessage();
        in.setMessage(new DataMessage(ascii("Hello world!").buffer()));

        System.out.printf("\n%s", in);

        Buffer encoded = in.encode();
        System.out.printf("\n%s", string(encoded.data));

        AnnotatedMessage out = MessageDecoder.decodeAnnotatedMessage(encoded);
        System.out.printf("\n%s\n", out);

        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testEncodeDecodeLessSimpleMessage() throws Exception {
        AmqpSequenceMessage msg = new AmqpSequenceMessage();

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

        AnnotatedMessage in = new AnnotatedMessage();
        in.setMessage(msg);
        in.setHeader(new Header());
        in.getHeader().setDurable(true);
        in.getHeader().setDeliveryCount(0L);

        System.out.printf("\n%s", in);

        Buffer encoded = in.encode();
        System.out.printf("\n%s", string(encoded.data));

        AnnotatedMessage out = MessageDecoder.decodeAnnotatedMessage(encoded);
        System.out.printf("\n%s\n", out);

        assertEquals(in.toString(), out.toString());
    }


}
