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

import org.fusesource.fabric.apollo.amqp.codec.types.*;
import org.fusesource.hawtbuf.Buffer;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

import static org.fusesource.fabric.apollo.amqp.codec.AmqpTestSupport.*;
import static org.fusesource.fabric.apollo.amqp.codec.CodecUtils.marshalAmqpFrame;
import static org.fusesource.fabric.apollo.amqp.codec.CodecUtils.unmarshalAmqpFrame;
import static org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory.*;

/**
 *
 */
public class AmqpFrameTest {

    // test an empty frame
    @Test
    public void testEmptyFrame() throws Exception {
        AmqpFrame in = new AmqpFrame((AmqpCommand)null);
        AmqpFrame out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }

    @Test
    public void testAmqpOpen() throws Exception {
        AmqpOpen open = createAmqpOpen();
        open.setContainerId("me");
        open.setChannelMax(256);

        AmqpFrame in = new AmqpFrame(open);

        byte b[] = CodecUtils.marshal(open);

        long expectedLength = b.length;
        long length = in.getDataSize();
        Assert.assertEquals(expectedLength + " != " + length, expectedLength, length);

        AmqpFrame out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
        Assert.assertTrue(open + " != " + out.getBody(), open.equals(out.getBody()));
    }

    @Test
    public void testAmqpBegin() throws Exception {
        AmqpBegin begin = createAmqpBegin();
        AmqpFrame in = new AmqpFrame(begin);
        AmqpFrame out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }

    @Test
    public void testEmptyAmqpTransfer() throws Exception {
        AmqpTransfer transfer = createAmqpTransfer();
        //transfer.setDeliveryTag(new Buffer("This is my delivery tag".getBytes()));
        transfer.setDeliveryTag(new Buffer("".getBytes()));
        transfer.setMore(false);
        transfer.setHandle(0);

        AmqpMessage message = new AmqpMessage();
        transfer.setFragments(createMultiple());
        transfer.getFragments().setValue(message.construct());
        AmqpFrame in = new AmqpFrame(transfer);
        AmqpFrame out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }

    @Test
    public void testAmqpTransferWithData() throws Exception {
        AmqpTransfer transfer = createAmqpTransfer();
        transfer.setDeliveryTag(new Buffer("This is my delivery tag".getBytes()));
        transfer.setMore(false);
        transfer.setHandle(0);
        transfer.setFragments(createMultiple());
        transfer.getFragments().setValue(createMessage(10).construct());

        AmqpFrame in = new AmqpFrame(transfer);
        AmqpFrame out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }

    @Test
    public void testAmqpTransferWithVariousFragmentTypes() throws Exception {
        AmqpTransfer transfer = createAmqpTransfer();
        transfer.setDeliveryTag(new Buffer("This is my delivery tag".getBytes()));
        transfer.setMore(false);
        transfer.setHandle(0);
        transfer.setFragments(createMultiple());
        transfer.getFragments().setValue(createMultiTypeAmqpMessage().construct());

        AmqpFrame in = new AmqpFrame(transfer);
        AmqpFrame out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }

    @Test
    public void testAmqpAttach() throws Exception {
        AmqpAttach attach = createAmqpAttach();
        AmqpTarget target = createAmqpTarget();
        target.setAddress(createAmqpString("localhost"));
        attach.setTarget(target);
        AmqpFrame in = new AmqpFrame(attach);
        AmqpFrame out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }

    @Test
    public void testAmqpDetach() throws Exception {
        AmqpDetach detach = createAmqpDetach();
        AmqpFrame in = new AmqpFrame(detach);
        AmqpFrame out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }

    @Test
    public void testAmqpFlow() throws Exception {
        AmqpFlow flow = createAmqpFlow();
        flow.setIncomingWindow(1);
        flow.setOutgoingWindow(1);
        flow.setNextIncomingId(10);
        flow.setNextOutgoingId(11);
        flow.setTransferCount(1);
        flow.setLinkCredit(0);
        flow.setAvailable(0);
        flow.setHandle(0);

        AmqpFrame in = new AmqpFrame(flow);
        byte b[] = marshalAmqpFrame(in);

        AmqpFrame out = unmarshalAmqpFrame(b);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }

    @Test
    public void testRealLifeTransferFrame() throws Exception {
        for (int i=0; i<500; i++) {
            AmqpTransfer transfer = createAmqpTransfer();
            transfer.setDeliveryTag(new Buffer(UUID.randomUUID().toString().getBytes()));
            transfer.setHandle(0);
            transfer.setTransferId(i + 1);
            transfer.setSettled(true);

            AmqpMessage message = new AmqpMessage();
            message.setHeader(createAmqpHeader());
            message.setProperties(createAmqpProperties());
            message.add(new Buffer(("Hello World Message #" + i + "!").getBytes()));
            message.setFooter(createAmqpFooter());

            transfer.setFragments(createMultiple());
            transfer.getFragments().setValue(message.construct());

            AmqpFrame in = new AmqpFrame(transfer);
            byte b[] = marshalAmqpFrame(in);
            AmqpFrame out = unmarshalAmqpFrame(b);
            Assert.assertTrue(in + " != " + out, in.equals(out));
        }
    }
}
