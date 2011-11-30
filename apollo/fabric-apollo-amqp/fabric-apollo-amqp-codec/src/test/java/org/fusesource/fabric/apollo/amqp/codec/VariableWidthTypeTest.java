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

import org.fusesource.fabric.apollo.amqp.codec.types.AMQPBinary;
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPString;
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPSymbol;
import org.fusesource.hawtbuf.Buffer;
import org.junit.Test;

import static org.fusesource.fabric.apollo.amqp.codec.TestSupport.writeRead;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class VariableWidthTypeTest {

    @Test
    public void testVBin8() throws Exception {
        Buffer in = new Buffer("Hello World!".getBytes());
        Buffer out = writeRead(new AMQPBinary(in)).getValue();
        assertEquals(in, out);
    }

    @Test
    public void testVBin32() throws Exception {
        Buffer in = new Buffer(1024);
        Buffer out = writeRead(new AMQPBinary(in)).getValue();
        assertEquals(in, out);
    }

    @Test
    public void testString8() throws Exception {
        String in = "Hello world!";
        String out = writeRead(new AMQPString(in)).getValue();
        assertEquals(in, out);
    }

    @Test
    public void testString32() throws Exception {
        StringBuilder builder = new StringBuilder();
        for ( int i = 0; i < 2048; i++ ) {
            builder.append((char) ((Math.random() * 52) + 65));
        }
        String in = builder.toString();
        String out = writeRead(new AMQPString(in)).getValue();
        assertEquals(in, out);
    }

    @Test
    public void testSym8() throws Exception {
        Buffer in = new Buffer("Hello world!".getBytes());
        Buffer out = writeRead(new AMQPSymbol(in)).getValue();
        assertEquals(in, out);
    }

    @Test
    public void testSym32() throws Exception {
        Buffer in = new Buffer(2048);
        Buffer out = writeRead(new AMQPSymbol(in)).getValue();
        assertEquals(in, out);
    }
}
