/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
