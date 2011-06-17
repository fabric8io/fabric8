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
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.fusesource.fabric.apollo.amqp.codec.TestSupport.writeRead;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class CompoundTypeTest {

    @Test
    public void testList8() throws Exception {
        List in = new ArrayList();
        in.add(new AMQPByte((byte)0x3));
        in.add(new AMQPShort((short)5));
        in.add(new AMQPInt(10));
        in.add(new AMQPString("hi"));

        List out = writeRead(new AMQPList(in)).getValue();

        assertEquals(in, out);
    }

    @Test
    public void testList32() throws Exception {
        List in = new ArrayList();

        for (int i = 0; i < 128; i++) {
            in.add(new AMQPByte((byte)i));
        }
        for (int i = 0; i < 128; i++) {
            in.add(new AMQPInt((int)i));
        }
        for (int i = 0; i < 128; i++) {
            in.add(new AMQPLong((long)i));
        }
        for (int i = 0; i < 128; i++) {
            in.add(new AMQPULong((long)i));
        }
        for (int i = 0; i < 128; i++) {
            in.add(new AMQPString("String number " + i));
        }

        List out = writeRead(new AMQPList(in)).getValue();

        assertEquals(in, out);
    }

}
