/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.amqp.codec;

import org.fusesource.fabric.apollo.amqp.codec.api.*;
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPType;
import org.fusesource.fabric.apollo.amqp.codec.types.*;
import org.fusesource.hawtbuf.Buffer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.fusesource.fabric.apollo.amqp.codec.TestSupport.encodeDecode;
import static org.fusesource.fabric.apollo.amqp.codec.api.MessageFactory.createAnnotatedMessage;
import static org.fusesource.fabric.apollo.amqp.codec.api.MessageFactory.createDataMessage;
import static org.fusesource.fabric.apollo.amqp.codec.api.MessageFactory.createValueMessage;
import static org.fusesource.fabric.apollo.amqp.codec.marshaller.MessageSupport.createSequenceMessage;
import static org.fusesource.fabric.apollo.amqp.codec.marshaller.MessageSupport.*;
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
        DataMessage message = createDataMessage(ascii("Hello world!").buffer());
        AnnotatedMessage in = createAnnotatedMessage(message);
        AnnotatedMessage out = encodeDecode(in);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testEncodeDecodeMultipartDataMessage() throws Exception {
        BareMessage msg = createDataMessage(Buffer.ascii("Hello").buffer(),
                ascii("World").buffer(),
                ascii("!").buffer());
        AnnotatedMessage in = createAnnotatedMessage(msg);
        AnnotatedMessage out = encodeDecode(in);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testEncodeDecodeSimpleValueMessage() throws Exception {
        BareMessage msg = createValueMessage(new AMQPString("Hello World!"));
        AnnotatedMessage in = createAnnotatedMessage(msg);
        AnnotatedMessage out = encodeDecode(in);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testEncodeDecodeSimpleSequenceMessage() throws Exception {
        List<AMQPType> list = new ArrayList<AMQPType>();
        list.add(new AMQPString("Hello"));
        list.add(new AMQPString("World"));
        list.add(new AMQPChar('!'));
        BareMessage msg = createSequenceMessage(new AMQPSequence(list));
        AnnotatedMessage in = createAnnotatedMessage(msg);
        AnnotatedMessage out = encodeDecode(in);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testEncodeDecodeSequenceMessageWithList() throws Exception {
        List<AMQPSequence> list = new ArrayList<AMQPSequence>();

        for ( int i = 0; i < 10; i++ ) {
            List<AMQPInt> inner = new ArrayList<AMQPInt>();
            list.add(new AMQPSequence(inner));
            for ( int j = 0; j < 10; j++ ) {
                inner.add(new AMQPInt(i + j));
            }
        }

        BareMessage msg = MessageFactory.createSequenceMessage(list);
        AnnotatedMessage in = createAnnotatedMessage(msg);
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

        ArrayList<AMQPString> payload1 = new ArrayList<AMQPString>();
        ArrayList<AMQPString> payload2 = new ArrayList<AMQPString>();

        for ( int i = 0; i < 10; i++ ) {
            payload1.add(new AMQPString("payload item " + i));
            payload2.add(new AMQPString("and payload item " + (i + 10)));
        }
        SequenceMessage msg = MessageFactory.createSequenceMessage(
                new AMQPSequence(payload1),
                new AMQPSequence(payload2),
                new Properties(null,
                        ascii("foo").buffer(),
                        new AMQPString("nowhere"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        new Date()));

        AnnotatedMessage in = createAnnotatedMessage(msg);
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
