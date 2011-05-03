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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.fusesource.fabric.apollo.amqp.codec.CodecUtils.marshalUnmarshal;
import static org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory.*;

/**
 *
 */
public class AmqpVariableWidthTypeTest {

    @Test
    public void testAmqpSymbol() throws Exception {
        String str = "";
        for ( int i=0; i<8192; i++ ) {
            str += "B";
            AmqpSymbol in = createAmqpSymbol(str);
            AmqpSymbol out = marshalUnmarshal(in);
            Assert.assertTrue(in + " != " + out, in.equals(out));
        }
    }

    @Test
    public void testAmqpBinary() throws Exception {
        String str = "";
        for ( int i=0; i<8192; i++ ) {
            str += "B";
            AmqpBinary in = createAmqpBinary(new Buffer(str.getBytes()));
            AmqpBinary out = marshalUnmarshal(in);
            Assert.assertTrue(in + " != " + out, in.equals(out));
        }

    }

    @Test
    public void testAmqpString() throws Exception {
        String str = "";
        for ( int i=0; i<8192; i++ ) {
            str += "B";
            AmqpString in = createAmqpString(str);
            AmqpString out = marshalUnmarshal(in);
            Assert.assertTrue(in + " != " + out, in.equals(out));
        }
    }

    @Test
    public void testAmqpList32() throws Exception {
        List<AmqpType<?, ?>> list = new ArrayList<AmqpType<?, ?>>();
        for ( int i=0; i<50; i++) {
            list.add(createAmqpString(String.valueOf(i)));
        }
        for ( int i=0; i<50; i++) {
            list.add(createAmqpLong((long)i));
        }
        AmqpList in = createAmqpList(new IAmqpList.AmqpWrapperList(list));
        AmqpList out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }

    @Test
    public void testAmqpList8() throws Exception {
        List<AmqpType<?, ?>> list = new ArrayList<AmqpType<?, ?>>();
        for ( int i=0; i<3; i++) {
            list.add(createAmqpString(String.valueOf(i)));
        }
        for ( int i=0; i<3; i++) {
            list.add(createAmqpLong((long)i));
        }
        AmqpList in = createAmqpList(new IAmqpList.AmqpWrapperList(list));
        AmqpList out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }

    @Test
    public void testAmqpArray32() throws Exception {
        List<AmqpString> list = new ArrayList<AmqpString>();
        for ( int i=0; i<100; i++) {
            list.add(createAmqpString(String.valueOf(i)));
        }
        AmqpList in = createAmqpList(new IAmqpList.AmqpWrapperList(list));
        AmqpList out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }

    @Test
    public void testAmqpArray8() throws Exception {
        List<AmqpString> list = new ArrayList<AmqpString>();
        for ( int i=0; i<5; i++) {
            list.add(createAmqpString(String.valueOf(i)));
        }
        AmqpList in = createAmqpList(new IAmqpList.AmqpWrapperList(list));
        AmqpList out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }

    @Test
    public void testAmqpMap32() throws Exception {
        AmqpMap in = createAmqpMap();
        in.put(createAmqpString("StringValue"), createAmqpString("Hello World!"));
        in.put(createAmqpString("LongValue"), createAmqpLong(1));
        in.put(createAmqpString("BooleanValue"), createAmqpBoolean(true));
        in.put(createAmqpString("ByteValue"), createAmqpByte((byte)1));
        in.put(createAmqpString("CharValue"), createAmqpChar(45));
        in.put(createAmqpString("DoubleValue"), createAmqpDouble(45));
        in.put(createAmqpString("FloatValue"), createAmqpFloat((float)1.0));
        in.put(createAmqpString("IntValue"), createAmqpInt(1));
        in.put(createAmqpString("UByteValue"), createAmqpUbyte((short)1));
        in.put(createAmqpString("UIntValue"), createAmqpUint((long)1));
        in.put(createAmqpString("ULongValue"), createAmqpUlong(new BigInteger("10")));
        in.put(createAmqpString("UShortValue"), createAmqpUshort(1));
        AmqpMap out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }

    @Test
    public void testAmqpMap8() throws Exception {
        AmqpMap in = createAmqpMap();
        in.put(createAmqpString("StringValue"), createAmqpString("Hello World!"));
        in.put(createAmqpString("LongValue"), createAmqpLong(1));
        AmqpMap out = marshalUnmarshal(in);
        Assert.assertTrue(in + " != " + out, in.equals(out));
    }
}
