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

import junit.framework.TestCase;
import org.fusesource.fabric.apollo.amqp.codec.types.AmqpHeader;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.fusesource.fabric.apollo.amqp.codec.AmqpTestSupport.print;
import static org.fusesource.fabric.apollo.amqp.codec.CodecUtils.marshalUnmarshal;
import static org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory.createAmqpHeader;

/**
 *
 */
public class AmqpUsageTest {

    @Test
    public void testSimpleMarshalChangeMarshalAgain() throws IOException {
        AmqpHeader in = createAmqpHeader();
        in.setDurable(true);
        print("Before marshalling : %s", in);
        AmqpHeader out1 = marshalUnmarshal(in);
        print("\n\nAfter marshalling : %s", out1);
        Assert.assertTrue(out1.equals(in));
        out1.setDurable(false);
        print("\n\nAfter update : %s", out1);
        Assert.assertFalse(out1.equals(in));
        AmqpHeader out2 = marshalUnmarshal(out1);
        print("\n\nAfter marshalling : %s", out2);
        Assert.assertTrue(out2.equals(out1));
    }

    /*
    public void testChangeAndMarshalAgain() throws Exception {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(BitUtils.getUByteArray(AmqpRawFrameBytes.peer0_2)));
        AmqpTransfer transfer = new AmqpFrame(in).getBody(AmqpTransfer.class);

        print("Original : %s", transfer);
        assertNotNull(transfer);

        // now make a bunch of changes
        AmqpMessage message = new AmqpMessage((IAmqpList<AmqpFragment>)transfer.getFragments().getValue());
        Date transmitTime = new Date();
        transfer.setMore(true);
        transfer.setAborted(true);
        message.getHeader().setTransmitTime(createAmqpTimestamp(transmitTime));
        transfer.getFragments().setValue(message.construct());
        print("\n\nBefore marshalling : %s", transfer);

        AmqpTransfer out = marshalUnmarshal(transfer);

        print("\n\nGot back : %s", out);
        assertNotNull(out);
        AmqpMessage outMessage = new AmqpMessage((IAmqpList<AmqpFragment>)transfer.getFragments().getValue());
        // ensure our changes were saved
        assertTrue(transmitTime.equals(outMessage.getHeader().getTransmitTime()));
        assertTrue(out.getMore());
        assertTrue(out.getAborted());
    }
    */
}
