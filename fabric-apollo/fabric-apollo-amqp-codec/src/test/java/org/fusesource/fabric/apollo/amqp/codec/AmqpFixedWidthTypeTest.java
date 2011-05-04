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
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Date;
import java.util.UUID;

import static org.fusesource.fabric.apollo.amqp.codec.CodecUtils.marshalUnmarshal;
import static org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory.*;

public class AmqpFixedWidthTypeTest {

    @Test
    public void testAmqpSequenceNo() throws Exception {
        AmqpSequenceNo val1 = createAmqpSequenceNo(10);
        AmqpSequenceNo val2 = createAmqpSequenceNo(10);
        Assert.assertTrue(val1 + " != " + val2, val1.equals(val2));
    }

    @Test
    public void testAmqpShort() throws Exception {
        AmqpShort in = createAmqpShort((short)23);
        AmqpShort out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }

    @Test
    public void testAmqpNull() throws Exception {
        AmqpNull in = createAmqpNull(new Object());
        AmqpNull out = marshalUnmarshal(in);
        Assert.assertNull(in + " != " + out, out);
    }

    @Test
    public void testAmqpUuid() throws Exception {
        AmqpUuid in = createAmqpUuid(UUID.randomUUID());
        AmqpUuid out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }

    @Test
    public void testAmqpTimestamp() throws Exception {
        AmqpTimestamp in = createAmqpTimestamp(new Date());
        AmqpTimestamp out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }

    @Test
    public void testAmqpLong() throws Exception {
        for (long i=-2048; i <= 2048; i++) {
            AmqpLong in = createAmqpLong(i);
            AmqpLong out = marshalUnmarshal(in);
            Assert.assertTrue(in + " != " + out, in.equals(out));
        }
    }

    @Test
    public void testSmallAmqpLong() throws Exception {
        AmqpLong in = createAmqpLong(23);
        AmqpLong out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }

    @Test
    public void testAmqpBoolean() throws Exception {
        AmqpBoolean in = createAmqpBoolean(true);
        AmqpBoolean out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }

    @Test
    public void testAmqpByte() throws Exception {
        AmqpByte in = createAmqpByte((byte)1);
        AmqpByte out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }

    @Test
    public void testAmqpChar() throws Exception {
        AmqpChar in = createAmqpChar((char)1);
        AmqpChar out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }

    @Test
    public void testAmqpDouble() throws Exception {
        AmqpDouble in = createAmqpDouble(1.0);
        AmqpDouble out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }

    @Test
    public void testAmqpFloat() throws Exception {
        AmqpFloat in = createAmqpFloat((float)1.0);
        AmqpFloat out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }
    @Test
    public void testAmqpDecimal32() throws Exception {
        AmqpDecimal32 in = createAmqpDecimal32(new BigDecimal("1.53234", MathContext.DECIMAL32));
        AmqpDecimal32 out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }


    @Test
    public void testAmqpDecimal64() throws Exception {
        AmqpDecimal64 in = createAmqpDecimal64(new BigDecimal("1.53234", MathContext.DECIMAL64));
        AmqpDecimal64 out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }


    @Test
    public void testAmqpInt() throws Exception {
        for (int i=-2048; i<=2048; i++) {
            AmqpInt in = createAmqpInt(i);
            AmqpInt out = marshalUnmarshal(in);
            Assert.assertTrue(in + " != " + out, in.equals(out));
        }
    }

    @Test
    public void testAmqpUbyte() throws Exception {
        AmqpUbyte in = createAmqpUbyte((short)1);
        AmqpUbyte out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }

    @Test
    public void testAmqpUint() throws Exception {
        for (long i=0; i<=2048; i++) {
            AmqpUint in = createAmqpUint(i);
            AmqpUint out = marshalUnmarshal(in);
            Assert.assertTrue(in + " != " + out, in.equals(out));
        }
    }

    @Test
    public void testAmqpUlong() throws Exception {
        for (long i=0; i<=2048; i++) {
            AmqpUlong in = createAmqpUlong(BigInteger.valueOf(253));
            AmqpUlong out = marshalUnmarshal(in);
            Assert.assertTrue(in + " != " + out, in.equals(out));
        }
    }

    @Test
    public void testAmqpUshort() throws Exception {
        for (int i=0; i<=2048; i++) {
            AmqpUshort in = createAmqpUshort(i);
            AmqpUshort out = marshalUnmarshal(in);
            Assert.assertTrue(in + " != " + out, in.equals(out));
        }
    }

}
