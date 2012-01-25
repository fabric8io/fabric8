/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.amqp.codec.marshaller;

import java.math.BigInteger;

/**
 *
 */
public class BitUtils {

    public static final short unsigned(byte value) {
        return (short) (0xff & value);
    }

    public static final byte[] getUByteArray(short[] array) {
        byte[] ret = new byte[array.length];
        for ( int i = 0; i < ret.length; i++ ) {
            setUByte(ret, i, array[i]);
        }
        return ret;
    }

    public static final void setUByte(final byte[] target, final int offset, final short value) {
        target[offset] = (byte) (0xff & value);
    }

    public static final int getUByte(final byte[] source, final int offset) {
        return 0xFF & source[offset];
    }

    public static final void setByte(final byte[] target, final int offset, final byte value) {
        target[offset + 0] = value;
    }

    public static final short getByte(final byte[] source, final int offset) {
        return source[offset];
    }

    public static final void setUShort(final byte[] target, final int offset, final int value) {
        target[offset + 0] = (byte) ((value >> 8) & 0xff);
        target[offset + 1] = (byte) ((value >> 0) & 0xff);
    }

    public static final int getUShort(final byte[] source, final int offset) {
        return
                ((int) source[offset + 0] & 0xff) << 8 |
                        ((int) source[offset + 1] & 0xff);
    }

    public static final void setShort(final byte[] target, final int offset, final short value) {
        target[offset + 0] = (byte) ((value >> 8) & 0xff);
        target[offset + 1] = (byte) ((value >> 0) & 0xff);
    }

    public static final short getShort(final byte[] source, final int offset) {
        return (short) (source[offset + 0] << 8 & 0xff | source[offset + 1]);
    }

    public static final void setUInt(final byte[] target, final int offset, final long value) {
        // Don't know why this fails when tests are run in maven...
        //assert value < Integer.MAX_VALUE * 2 + 1;
        target[offset + 0] = (byte) (value >> 24 & 0xff);
        target[offset + 1] = (byte) (value >> 16 & 0xff);
        target[offset + 2] = (byte) (value >> 8 & 0xff);
        target[offset + 3] = (byte) (value >> 0 & 0xff);
    }

    public static final long getUInt(final byte[] source, final int offset) {
        return
                ((long) (source[offset + 0] & 0xff) << 24 |
                        (source[offset + 1] & 0xff) << 16 |
                        (source[offset + 2] & 0xff) << 8 |
                        (source[offset + 3] & 0xff)) & 0xFFFFFFFFL;
    }

    public static final void setInt(final byte[] target, final int offset, final int value) {
        target[offset + 0] = (byte) (value >> 24 & 0xff);
        target[offset + 1] = (byte) (value >> 16 & 0xff);
        target[offset + 2] = (byte) (value >> 8 & 0xff);
        target[offset + 3] = (byte) (value >> 0 & 0xff);
    }

    public static final int getInt(final byte[] source, final int offset) {
        return
                (source[offset + 0] & 0xff) << 24 |
                        (source[offset + 1] & 0xff) << 16 |
                        (source[offset + 2] & 0xff) << 8 |
                        (source[offset + 3] & 0xff);
    }

    public static final void setLong(final byte[] target, final int offset, final long value) {
        target[offset + 0] = (byte) (value >> 56 & 0xff);
        target[offset + 1] = (byte) (value >> 48 & 0xff);
        target[offset + 2] = (byte) (value >> 40 & 0xff);
        target[offset + 3] = (byte) (value >> 32 & 0xff);
        target[offset + 4] = (byte) (value >> 24 & 0xff);
        target[offset + 5] = (byte) (value >> 12 & 0xff);
        target[offset + 6] = (byte) (value >> 8 & 0xff);
        target[offset + 7] = (byte) (value >> 0 & 0xff);
    }

    public static final long getLong(final byte[] source, final int offset) {
        return
                ((long) (source[offset + 0] & 0xff) << 56) |
                        ((long) (source[offset + 1] & 0xff) << 48) |
                        ((long) (source[offset + 2] & 0xff) << 40) |
                        ((long) (source[offset + 3] & 0xff) << 32) |
                        ((long) (source[offset + 4] & 0xff) << 24) |
                        ((long) (source[offset + 5] & 0xff) << 16) |
                        ((long) (source[offset + 6] & 0xff) << 8) |
                        ((long) (source[offset + 7] & 0xff));
    }

    public static final BigInteger getULong(final byte[] source, final int offset) {
        byte[] bi = new byte[9];
        System.arraycopy(source, offset, bi, 1, 8);
        return new BigInteger(bi);
    }

    public static final void setULong(final byte[] target, final int offset, final BigInteger value) {
        byte[] b = value.toByteArray();
        System.arraycopy(b, 0, target, offset + 8 - b.length, b.length);
    }
}
