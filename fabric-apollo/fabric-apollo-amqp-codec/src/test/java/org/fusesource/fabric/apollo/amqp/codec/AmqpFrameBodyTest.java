/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fusemq.amqp.codec;

import org.fusesource.fusemq.amqp.codec.types.*;
import org.junit.Assert;
import org.junit.Test;

import static org.fusesource.fusemq.amqp.codec.AmqpTestSupport.print;
import static org.fusesource.fusemq.amqp.codec.CodecUtils.marshalUnmarshal;
import static org.fusesource.fusemq.amqp.codec.types.TypeFactory.*;

/**
 *
 */
public class AmqpFrameBodyTest {

    @Test
    public void testAmqpOpen() throws Exception {
        AmqpOpen in = createAmqpOpen();
        AmqpOpen out = marshalUnmarshal(in);
        Assert.assertTrue(in.equals(out));
    }

    @Test
    public void testAmqpBegin() throws Exception {
        AmqpBegin in = createAmqpBegin();
        AmqpBegin out = marshalUnmarshal(in);
        Assert.assertTrue(in.equals(out));
    }

    @Test
    public void testAmqpAttach() throws Exception {
        AmqpAttach in = createAmqpAttach();
        in.setName("7c89c4c1-ada9-4b71-85ae-780e8dd75b5");
        in.setHandle(0);
        in.setRole(AmqpRole.SENDER);
        AmqpTarget target = createAmqpTarget();
        target.setAddress(createAmqpString("QUEUE1"));
        in.setTarget(target);
        AmqpAttach out = marshalUnmarshal(in);
        print("In : %1$s\nOut : %1$s", in, out);
        Assert.assertTrue(in.equals(out));
    }

    @Test
    public void testAmqpTransfer() throws Exception {
        AmqpTransfer in = createAmqpTransfer();
        in.setMore(false);
        in.setAborted(false);
        AmqpTransfer out = marshalUnmarshal(in);
        Assert.assertTrue(in.equals(out));
    }

    @Test
    public void testAmqpDisposition() throws Exception {
        AmqpDisposition in = createAmqpDisposition();
        AmqpDisposition out = marshalUnmarshal(in);
        Assert.assertTrue(in.equals(out));
    }

    @Test
    public void testAmqpDetach() throws Exception {
        AmqpDetach in = createAmqpDetach();
        AmqpDetach out = marshalUnmarshal(in);
        Assert.assertTrue(in.equals(out));
    }

    @Test
    public void testAmqpEnd() throws Exception {
        AmqpEnd in = createAmqpEnd();
        AmqpEnd out = marshalUnmarshal(in);
        Assert.assertTrue(in.equals(out));
    }

    @Test
    public void testAmqpClose() throws Exception {
        AmqpClose in = createAmqpClose();
        AmqpClose out = marshalUnmarshal(in);
        Assert.assertTrue(in.equals(out));
    }
}
