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

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.AmqpMarshaller;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.Encoded;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.v1_0_0.MultipleMarshaller;
import org.fusesource.fabric.apollo.amqp.codec.types.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory.*;

public class AmqpTestSupport {

    static public void dumpMap(AmqpMap map) throws Exception {
        System.out.println("Number of entries : " + map.getEntryCount());
        Iterator<Map.Entry<AmqpType<?, ?>, AmqpType<?, ?>>> iter = map.iterator();
        int entryNumber = 1;
        while ( iter.hasNext() ) {
            Map.Entry<AmqpType<?, ?>, AmqpType<?, ?>> entry = iter.next();
            System.out.println("[" + entryNumber + "] : " + entry.getKey() + " : " + entry.getValue());
            entryNumber++;
        }
    }

    static public void print(String str, Object... args) {
        System.out.format(str + "\n", args);
    }

    static public void print(byte[] bytes) {
        for ( byte aByte : bytes ) {
            char character = (char) aByte;
            System.out.print("0x" + Integer.toHexString(aByte) + ":(" + character + ")");
        }
        System.out.println();
    }

    static public int validateAmqpFrameWithList32(int offset, byte[] b) {
        int oldOffset = offset;
        long frameSize = BitUtils.getUInt(b, offset);

        // header
        offset += 8;

        // described type format code
        offset += 1;

        // descriptor type format code
        offset += 1;

        // skip to the next type, is 1 + length as we need to move past the encoded length
        int typeLength = 0xff & BitUtils.getUByte(b, offset);
        offset += 1 + typeLength;

        // skip list type format code
        offset += 1;

        // get the data size header
        typeLength = (int)BitUtils.getUInt(b, offset);
        offset += 4;

        // skip to the end of the composite:
        offset += typeLength;

        assertEquals(frameSize, offset - oldOffset);

        return offset;
    }

    static public int validateAmqpFrameWithList8(int offset, byte[] b) {
        int oldOffset = offset;
        long frameSize = BitUtils.getUInt(b, offset);

        // header
        offset += 8;

        // described type format code
        offset += 1;

        // descriptor type format code
        offset += 1;

        // skip to the next type, is 1 + length as we need to move past the encoded length
        int typeLength = 0xff & BitUtils.getUByte(b, offset);
        offset += 1 + typeLength;

        // skip list type format code
        offset += 1;

        // get the data size header
        typeLength = 0xff & BitUtils.getUByte(b, offset);
        offset += 1;

        // skip to the end of the composite:
        offset += typeLength;

        assertEquals(frameSize, offset - oldOffset);

        return offset;
    }

    static public AmqpFrame marshalUnmarshal(AmqpFrame frame) throws IOException {
        return CodecUtils.unmarshalAmqpFrame(CodecUtils.marshalAmqpFrame(frame));
    }

    // needed to unmarshal types that would normally be unmarshalled from a containing type
    static public AmqpFilterSet unmarshalAmqpFilterSet(byte[] b) throws IOException {
        AmqpMarshaller marshaller = org.fusesource.fabric.apollo.amqp.codec.marshaller.v1_0_0.AmqpMarshaller.getMarshaller();
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(b));
        return AmqpFilterSet.AmqpFilterSetBuffer.create(in, marshaller);
    }

    static public AmqpOptions unmarshalAmqpOptions(byte[] b) throws IOException {
        AmqpMarshaller marshaller = org.fusesource.fabric.apollo.amqp.codec.marshaller.v1_0_0.AmqpMarshaller.getMarshaller();
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(b));
        return AmqpOptions.AmqpOptionsBuffer.create(in, marshaller);
    }

    static public AmqpMessage createMessage(long elementCount) throws Exception {
        AmqpMessage in = new AmqpMessage();
        AmqpString str = createAmqpString("AAAA");
        for ( int i=0; i < 10; i++ ) {
            in.add(str);
        }
        print ("Message : %1$s", in);
        return in;
    }

    static public AmqpMessage createMultiTypeAmqpMessage() throws Exception {
        AmqpMessage in = new AmqpMessage();
        in.add(new Buffer("create a fragment via buffer".getBytes()));
        in.add("create a fragment via byte array".getBytes());
        in.add(createAmqpLong(27));
        in.add(createAmqpString("a fragment with a string in it"));
        AmqpMap someMap = createAmqpMap();
        someMap.put(createAmqpString("A map"), createAmqpString("with a value"));
        in.add(someMap);
        ArrayList<AmqpString> list = new ArrayList<AmqpString>();
        list.add(createAmqpString("A list with a string in it"));
        AmqpList someList = createAmqpList(new IAmqpList.AmqpWrapperList(list));
        in.add(someList);
        in.setHeader(createAmqpHeader());
        in.getHeader().setDurable(false);
        //print("Message : %1$s", in);
        return in;
    }

    static Multiple unmarshalMultiple(byte[] marshalled) throws IOException {
        // have to explicitly decode as a Multiple type, otherwise we'd get an AmqpSymbol
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(marshalled));
        Encoded<IAmqpList<AmqpType<?, ?>>> encoded = MultipleMarshaller.createEncoded(in);
        return Multiple.MultipleBuffer.create(encoded);
    }
}
