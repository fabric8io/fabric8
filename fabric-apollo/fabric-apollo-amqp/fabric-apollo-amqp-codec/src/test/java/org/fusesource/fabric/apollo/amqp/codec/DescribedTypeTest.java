/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.codec;

import org.fusesource.fabric.apollo.amqp.codec.types.*;
import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.hawtbuf.Buffer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.fusesource.fabric.apollo.amqp.codec.TestSupport.writeRead;
import static org.fusesource.hawtbuf.Buffer.ascii;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class DescribedTypeTest {

    @Test
    public void testApplicationProperties() throws Exception {
        ApplicationProperties in = new ApplicationProperties();
        in.setValue(new HashMap());
        in.getValue().put(new AMQPSymbol(ascii("one").buffer()), new AMQPString("two"));
        in.getValue().put(new AMQPSymbol(ascii("three").buffer()), new AMQPString("four"));
        ApplicationProperties out = writeRead(in);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testAMQPValue() throws Exception {
        AMQPValue in = new AMQPValue();
        in.setValue(new AMQPString("Hello world!"));
        AMQPValue out = writeRead(in);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testAMQPSequence() throws Exception {
        AMQPSequence in = new AMQPSequence();
        in.setValue(new ArrayList());
        in.getValue().add(new AMQPString("Hello world!"));
        in.getValue().add(new AMQPString("and stuff"));
        in.getValue().add(new AMQPLong(123L));
        AMQPSequence out = writeRead(in);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testOpen() throws Exception {
        Open in = new Open();
        in.setChannelMax((int) Short.MAX_VALUE);
        in.setContainerID("foo");
        in.setHostname("localhost");
        in.setOfferedCapabilities(new AMQPSymbol[]{new AMQPSymbol(new Buffer("blah".getBytes()))});
        Open out = writeRead(in);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testBegin() throws Exception {
        Begin in = new Begin();
        in.setHandleMax((long) Integer.MAX_VALUE);
        in.setNextOutgoingID(0L);
        in.setIncomingWindow(10L);
        in.setOutgoingWindow(10L);
        Begin out = writeRead(in);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testEnd() throws Exception {
        End in = new End();
        End out = writeRead(in);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testTransfer() throws Exception {
        Transfer in = new Transfer();
        in.setHandle(0L);
        in.setDeliveryTag(new AsciiBuffer("0").buffer());
        in.setDeliveryID(0L);
        in.setSettled(false);
        in.setAborted(false);
        in.setMessageFormat(0L);
        in.setMore(false);
        in.setResume(false);
        in.setRcvSettleMode(ReceiverSettleMode.FIRST.getValue());
        in.setState(new Accepted());
        Transfer out = writeRead(in);
        System.out.printf("\n\n%s\n%s\n\n", in, out);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testAttach() throws Exception {
        Attach in = new Attach();
        in.setName("TEST");
        in.setHandle(0L);
        in.setRole(Role.SENDER.getValue());
        in.setInitialDeliveryCount(0L);
        Source source = new Source();
        Target target = new Target();
        target.setAddress(new AddressString("Foo"));
        in.setSource(source);
        in.setTarget(target);

        Attach out = writeRead(in);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testDetach() throws Exception {
        Detach in = new Detach();
        in.setHandle(0L);
        in.setClosed(true);
        Detach out = writeRead(in);
        assertEquals(in.toString(), out.toString());
    }
}
