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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.fusesource.fusemq.amqp.codec.AmqpTestSupport.*;
import static org.fusesource.fusemq.amqp.codec.marshaller.v1_0_0.AmqpListMarshaller.LIST_ENCODING.ARRAY8;
import static org.fusesource.fusemq.amqp.codec.types.TypeFactory.*;

/**
 *
 */
public class AmqpDescribedTypeTest {

    @Test
    public void testAmqpAccepted() throws Exception {
        AmqpAccepted in = createAmqpAccepted();
        AmqpAccepted out = CodecUtils.marshalUnmarshal(in);
        Assert.assertTrue(in.equals(out));
    }

    @Test
    public void testAmqpCoordinator() throws Exception {
        AmqpCoordinator in = createAmqpCoordinator();
        AmqpCoordinator out = CodecUtils.marshalUnmarshal(in);
        Assert.assertTrue(in.equals(out));
    }

    @Test
    public void testAmqpDeclare() throws Exception {
        AmqpDeclare in = createAmqpDeclare();
        AmqpDeclare out = CodecUtils.marshalUnmarshal(in);
        Assert.assertTrue(in.equals(out));
    }

    @Test
    public void testAmqpDeleteOnClose() throws Exception {
        AmqpDeleteOnClose in = createAmqpDeleteOnClose();
        AmqpDeleteOnClose out = CodecUtils.marshalUnmarshal(in);
        Assert.assertTrue(in.equals(out));
    }

    @Test
    public void testAmqpDeleteOnNoLinks() throws Exception {
        AmqpDeleteOnNoLinks in = createAmqpDeleteOnNoLinks();
        AmqpDeleteOnNoLinks out = CodecUtils.marshalUnmarshal(in);
        Assert.assertTrue(in.equals(out));
    }

    @Test
    public void testAmqpDeleteOnNoMessages() throws Exception {
        AmqpDeleteOnNoMessages in = createAmqpDeleteOnNoMessages();
        AmqpDeleteOnNoMessages out = CodecUtils.marshalUnmarshal(in);
        Assert.assertTrue(in.equals(out));
    }

    @Test
    public void testAmqpDeleteOnNoLinksOrMessages() throws Exception {
        AmqpDeleteOnNoLinksOrMessages in = createAmqpDeleteOnNoLinksOrMessages();
        AmqpDeleteOnNoLinksOrMessages out = CodecUtils.marshalUnmarshal(in);
        Assert.assertTrue(in.equals(out));
    }

    @Test
    public void testAmqpProperties() throws Exception {
        AmqpProperties in = createAmqpProperties();
        AmqpProperties out = CodecUtils.marshalUnmarshal(in);
        Assert.assertTrue(in.equals(out));
    }

    @Test
    public void testAmqpFilterSet() throws Exception {
        AmqpFilterSet in = createAmqpFilterSet();
        AmqpFilter filter = createAmqpFilter();
        filter.setType("Long");
        filter.setPredicate(createAmqpLong(1));
        in.put(createAmqpSymbol("Long"), filter);
        AmqpFilterSet out = unmarshalAmqpFilterSet(CodecUtils.marshal(in));
        Assert.assertTrue(in.equals(out));
    }

    @Test
    public void testAmqpFooter() throws Exception {
        AmqpFooter in = createAmqpFooter();
        AmqpFooter out = CodecUtils.marshalUnmarshal(in);
        Assert.assertTrue(in.equals(out));
    }

    @Test
    public void testAmqpHeader() throws Exception {
        AmqpHeader in = createAmqpHeader();
        in.setDeliveryFailures(0);
        in.setDurable(false);
        in.setPriority((short)5);
        in.setTransmitTime(new Date());
        in.setTtl(new BigInteger("0"));
        in.setFormerAcquirers(0);
        AmqpHeader out = CodecUtils.marshalUnmarshal(in);
        Assert.assertTrue(in.equals(out));
    }

    @Test
    public void testAmqpOptions() throws Exception {
        AmqpOptions in = createAmqpOptions();
        in.put(createAmqpString("StringValue"), createAmqpString("Hello World!"));
        in.put(createAmqpString("LongValue"), createAmqpLong(1));
        in.put(createAmqpString("BooleanValue"), createAmqpBoolean(true));
        in.put(createAmqpString("ByteValue"), createAmqpByte((byte)1));
        in.put(createAmqpString("CharValue"), createAmqpChar(45));
        in.put(createAmqpString("DoubleValue"), createAmqpDouble(45));
        in.put(createAmqpString("FloatValue"), createAmqpFloat((float)1.0));
        in.put(createAmqpString("IntValue"), createAmqpInt(1));
        in.put(createAmqpString("UByteValue"), createAmqpUbyte((short)1));
        in.put(createAmqpString("UIntValue"), createAmqpUint((long) 1));
        in.put(createAmqpString("ULongValue"), createAmqpUlong(new BigInteger("10")));
        in.put(createAmqpString("UShortValue"), createAmqpUshort(1));
        AmqpOptions out = unmarshalAmqpOptions(CodecUtils.marshal(in));
        Assert.assertTrue(in.equals(out));
    }

    @Test
    public void testAmqpFlow() throws Exception {
        AmqpFlow in = createAmqpFlow();
        in.setHandle(1);
        in.setOptions(createAmqpOptions());
        in.getOptions().put(createAmqpString("Hello"), createAmqpUint(20));
        AmqpFlow out = CodecUtils.marshalUnmarshal(in);
        Assert.assertTrue(in.equals(out));
    }

    /*
    // TODO - Pick a different type to do this test with, target is now list based
    // described types derived from an amqp map can be iterated through.
    @Test
    public void testMapDescribedTypeIterator() throws Exception {
        AmqpTarget target = createAmqpTarget();
        target.setAddress(createAmqpString("SOME_QUEUE"));
        target.setCapabilities(createMultiple());
        target.getCapabilities().setValue(createAmqpString("blah"));

        Map.Entry<AmqpType<?, ?>, AmqpType<?, ?>> address = null;
        Map.Entry<AmqpType<?, ?>, AmqpType<?, ?>> capabilities = null;

        for ( Map.Entry<AmqpType<?, ?>, AmqpType<?, ?>> me : target ) {
            print("key: %s, value: %s", me.getKey(), me.getValue());
            if ( me.getKey().equals(AmqpTarget.ADDRESS_KEY) ) {
                address = me;
            }
            if ( me.getKey().equals(AmqpTarget.CAPABILITIES_KEY) ) {
                capabilities = me;
            }
        }
        Assert.assertTrue(address.getKey().equals(AmqpTarget.ADDRESS_KEY));
        Assert.assertTrue(address.getValue().equals(createAmqpString("SOME_QUEUE")));
        Assert.assertTrue(capabilities.getKey().equals(AmqpTarget.CAPABILITIES_KEY));
        Multiple cap = createMultiple();
        cap.setValue(createAmqpString("blah"));
        Assert.assertTrue(capabilities.getValue().equals(cap));
    }
    */

    @Test
    public void testEmptyMultiple() throws Exception {
        Multiple multiple = createMultiple();
        byte marshalled[] = CodecUtils.marshal(multiple);
        print(marshalled);

        Assert.assertEquals("Should be a 1 element array", 1, marshalled.length);
        Assert.assertEquals("Array value should be NULL (0x40)", (byte) 0x40, BitUtils.getUByte(marshalled, 0));

        Multiple out = unmarshalMultiple(marshalled);
        print("Out: %s", out);
        Assert.assertTrue("Type should be null", out == null);

    }

    @Test
    public void testMultipleWithOneValue() throws Exception {
        Multiple multiple = createMultiple();
        multiple.setValue(createAmqpSymbol("ASCII"));
        byte marshalled[] = CodecUtils.marshal(multiple);

        print(marshalled);

        Assert.assertEquals("Byte 0 should indicate the type is a sym8 (0xA3)", 0xA3, BitUtils.getUByte(marshalled, 0));
        Assert.assertEquals("Byte 1 should indicate a count of 5 bytes", 0x05, BitUtils.getUByte(marshalled, 1));

        Multiple out = unmarshalMultiple(marshalled);

        print("Out: %s", out);
    }

    @Test
    public void testMultipleWithSeveralValues() throws Exception {
        System.setProperty("org.fusesource.fusemq.amqp.codec.Use8BitListEncodings", "true");
        System.setProperty("org.fusesource.fusemq.amqp.codec.NoArrayEncoding", "false");
        List<AmqpSymbol> list = new ArrayList<AmqpSymbol>();
        list.add(createAmqpSymbol("ASCII"));
        list.add(createAmqpSymbol("EBCDIC"));

        Multiple multiple = createMultiple();
        multiple.setValue(createAmqpList(new IAmqpList.AmqpWrapperList(list)));

        byte marshalled[] = CodecUtils.marshal(multiple);

        print(marshalled);

        Assert.assertEquals("Byte 0 should be the described type format code", 0, BitUtils.getUByte(marshalled, 0));
        Assert.assertEquals("Byte 1 should be True (0x41)", 0x41, BitUtils.getUByte(marshalled, 1));
        Assert.assertEquals("Byte 2 should be the array8 format code (0xE2)", 0xFF & ARRAY8.getEncodingFormatCode(), BitUtils.getUByte(marshalled, 2));
        Assert.assertEquals("Byte 3 should be the size of the array, 15 bytes", 0x0F, BitUtils.getUByte(marshalled, 3));
        Assert.assertEquals("Byte 4 should be the count of the array", 0x02, BitUtils.getUByte(marshalled, 4));
        Assert.assertEquals("Byte 5 should be sym8 format code (0xA3)", 0xA3, BitUtils.getUByte(marshalled, 5));
        Assert.assertEquals("Byte 6 should be the character count of the first element", 0x05, BitUtils.getUByte(marshalled, 6));
        Assert.assertEquals("Byte 12 should be the character count of the second element", 0x06, BitUtils.getUByte(marshalled, 12));

        Multiple out = unmarshalMultiple(marshalled);

        print("Out: %s", out);
        Assert.assertTrue("Type should be Multiple", out instanceof Multiple);
    }

    @Test
    public void testSaslMechanismsWithNoEntry() throws Exception {
        AmqpSaslMechanisms in = createAmqpSaslMechanisms();
        in.setSaslServerMechanisms(createMultiple());
        print("In: %s", in);
        byte marshalled[] = CodecUtils.marshal(in);
        print(marshalled);
        AmqpSaslMechanisms out = CodecUtils.unmarshal(marshalled);
        print("In: %s, Out: %s", in, out);
        Assert.assertTrue(out.getSaslServerMechanisms() == null);
    }

    @Test
    public void testSaslMechanismsWithOneEntry() throws Exception {
        AmqpSaslMechanisms in = createAmqpSaslMechanisms();
        in.setSaslServerMechanisms(createMultiple());
        in.getSaslServerMechanisms().setValue(createAmqpSymbol("blah"));
        AmqpSaslMechanisms out = CodecUtils.marshalUnmarshal(in);
        print("In: %s, Out: %s", in, out);
        Assert.assertTrue(in.equals(out));
    }

    @Test
    public void testSaslMechanismsWithTwoEntries() throws Exception {
        AmqpSaslMechanisms in = createAmqpSaslMechanisms();
        in.setSaslServerMechanisms(createMultiple());
        List<AmqpSymbol> list = new ArrayList<AmqpSymbol>();
        list.add(createAmqpSymbol("blah"));
        list.add(createAmqpSymbol("blah"));
        in.getSaslServerMechanisms().setValue(createAmqpList(new IAmqpList.AmqpWrapperList(list)));
        print("In: %s", in);
        byte marshalled[] = CodecUtils.marshal(in);
        print(marshalled);
        AmqpSaslMechanisms out = CodecUtils.unmarshal(marshalled);
        print("In: %s, Out: %s", in, out);
        Assert.assertTrue(in.equals(out));

    }
}
