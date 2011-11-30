/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.codec.marshaller;

import java.io.DataInput;
import java.io.DataOutput;

import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPList.*;

/**
 *
 */
public class DescribedTypeSupport {

    public static byte getListEncoding(long fieldSize, long count) {
        if ( count == 0 ) {
            return LIST_LIST0_CODE;
        }
        if ( fieldSize <= (255 - LIST_LIST8_WIDTH) ) {
            return LIST_LIST8_CODE;
        }
        return LIST_LIST32_CODE;
    }

    public static int getListWidth(byte formatCode) {
        if ( formatCode == LIST_LIST0_CODE ) {
            return LIST_LIST0_WIDTH;
        }
        if ( formatCode == LIST_LIST8_CODE ) {
            return LIST_LIST8_WIDTH;
        }
        return LIST_LIST32_CODE;
    }

    public static long fullSizeOfList(long size, long count) {
        return 1 + getListWidth(getListEncoding(size, count)) * 2 + size;
    }

    public static long encodedListSize(long size, long count) {
        return getListWidth(getListEncoding(size, count)) + size;
    }

    public static void writeListHeader(long size, long count, DataOutput out) throws Exception {
        byte formatCode = getListEncoding(size, count);
        out.writeByte(formatCode);
        if ( formatCode == LIST_LIST0_CODE ) {
            // do nothing...
        } else if ( formatCode == LIST_LIST8_CODE ) {
            out.writeByte((byte) encodedListSize(size, count));
            out.writeByte((byte) count);
        } else {
            TypeRegistry.instance().encoder().writeUInt(encodedListSize(size, count), out);
            TypeRegistry.instance().encoder().writeUInt((long) count, out);
        }
    }

    public static long readListHeader(DataInput in) throws Exception {
        byte formatCode = (byte) in.readUnsignedByte();

        if ( formatCode == LIST_LIST0_CODE ) {
            return 0;
        } else if ( formatCode == LIST_LIST8_CODE ) {
            in.readUnsignedByte();
            return in.readUnsignedByte();
        } else if ( formatCode == LIST_LIST32_CODE ) {
            TypeRegistry.instance().encoder().readUInt(in);
            return TypeRegistry.instance().encoder().readUInt(in);
        } else if ( formatCode == TypeRegistry.NULL_FORMAT_CODE ) {
            return 0;
        } else {
            throw new RuntimeException(String.format("Unknown format code (0x%x) for list type", formatCode));
        }

    }


}
