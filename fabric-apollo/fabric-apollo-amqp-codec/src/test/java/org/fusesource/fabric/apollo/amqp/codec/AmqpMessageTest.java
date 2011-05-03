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

import org.fusesource.fabric.apollo.amqp.codec.types.AmqpList;
import org.fusesource.fabric.apollo.amqp.codec.types.AmqpType;
import org.fusesource.hawtbuf.Buffer;
import org.junit.Ignore;
import org.junit.Test;

import java.util.LinkedList;

import static org.fusesource.fabric.apollo.amqp.codec.AmqpTestSupport.*;
import static org.fusesource.fabric.apollo.amqp.codec.CodecUtils.getSize;
import static org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory.*;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class AmqpMessageTest {

     private long getMessageSize(LinkedList<AmqpList> message) {
        long rc = 0;
        for ( AmqpList list : message ) {
            for ( AmqpType<?, ?> type : list ) {
                rc += getSize(type);
            }
        }
        return rc;
    }

    private void printMessage(LinkedList<AmqpList> message) {
        for ( AmqpList list : message ) {
            print("\n%s\n", list);
        }
    }

    @Test
    public void testAmqpMessageWithLargeMaxSize() throws Exception {
        AmqpMessage in = createMessage(10);
        LinkedList<AmqpList> out = in.construct(1024);
        long encodedSize = getMessageSize(out);
        printMessage(out);
        assertEquals(1, out.size());
        assertEquals(out.get(0).getListCount(), in.getCount());
        assertEquals(in.getEncodedSize(), encodedSize);
    }

    @Test
    public void testAmqpMessageWithSmallMaxSize() throws Exception {
        AmqpMessage in = AmqpTestSupport.createMessage(10);
        LinkedList<AmqpList> out = in.construct(300);
        long encodedSize = getMessageSize(out);
        printMessage(out);
        assertEquals(2, out.size());
        assertEquals(out.get(0).getListCount() + out.get(1).getListCount(), in.getCount());
        assertEquals(in.getEncodedSize(), encodedSize);
    }

    @Test
    public void testAmqpMessageMultipleTypesLargeMaxSize() throws Exception {
        AmqpMessage in = createMultiTypeAmqpMessage();
        print("Created message %s", in);
        LinkedList<AmqpList> out = in.construct(1024);
        print("Constructed fragment list %s", out);
        printMessage(out);
        assertEquals(1, out.size());
        assertEquals(out.get(0).getListCount(), in.getCount() + 1);
    }

    @Test
    @Ignore
    public void testAmqpMessageMultipletypesSmallMaxSize() throws Exception {
        AmqpMessage in = createMultiTypeAmqpMessage();
        LinkedList<AmqpList> out = in.construct(150);
        printMessage(out);
        assertEquals(3, out.size());
    }

    @Test
    public void checkPayload() throws Exception {
        AmqpMessage in = new AmqpMessage();

        in.add("Hello World!".getBytes());
        in.add(createAmqpUint(0));
        in.add(createAmqpList());
        in.add(createAmqpMap());

        assertEquals(4, in.getCount());

        assertEquals(in.getPayload(0), new Buffer("Hello World!".getBytes()));
        assertEquals(in.getPayload(1), createAmqpUint(0));
        assertEquals(in.getPayload(2), createAmqpList());
        assertEquals(in.getPayload(3), createAmqpMap());

    }

}
