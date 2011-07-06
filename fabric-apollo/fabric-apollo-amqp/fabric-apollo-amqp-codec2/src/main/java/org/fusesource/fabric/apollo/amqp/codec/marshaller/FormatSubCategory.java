/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.codec.marshaller;

/**
*
*/
public enum FormatSubCategory {
    DESCRIBED((byte) 0x00, 0), FIXED_0((byte) 0x40, 0), FIXED_1((byte) 0x50, 1), FIXED_2((byte) 0x60, 2), FIXED_4((byte) 0x70, 4), FIXED_8((byte) 0x80, 8), FIXED_16((byte) 0x90, 16), VARIABLE_1(
            (byte) 0xA0, 1), VARIABLE_4((byte) 0xB0, 4), COMPOUND_1((byte) 0xC0, 1), COMPOUND_4((byte) 0xD0, 4), ARRAY_1((byte) 0xE0, 1), ARRAY_4((byte) 0xF0, 4);

    public final FormatCategory category;
    public final byte subCategory;
    public final int WIDTH;

    FormatSubCategory(byte subCategory, int width) {
        this.subCategory = subCategory;
        category = FormatCategory.getCategory(this.subCategory);
        this.WIDTH = width;

    }

    public static FormatSubCategory getCategory(byte formatCode) throws IllegalArgumentException {
        switch ((byte) (formatCode & 0xF0)) {
        case (byte) 0x00:
            return DESCRIBED;
        case (byte) 0x40:
            return FIXED_0;
        case (byte) 0x50:
            return FIXED_1;
        case (byte) 0x60:
            return FIXED_2;
        case (byte) 0x70:
            return FIXED_4;
        case (byte) 0x80:
            return FIXED_8;
        case (byte) 0x90:
            return FIXED_16;
        case (byte) 0xA0:
            return VARIABLE_1;
        case (byte) 0xB0:
            return VARIABLE_4;
        case (byte) 0xC0:
            return COMPOUND_1;
        case (byte) 0xD0:
            return COMPOUND_4;
        case (byte) 0xE0:
            return ARRAY_1;
        case (byte) 0xF0:
            return ARRAY_4;
        default:
            throw new IllegalArgumentException("" + formatCode);
        }
    }

    public final boolean encodesSize() {
        return category.encodesSize();
    }

    public final boolean encodesCount() {
        return category.encodesCount();
    }

    /*
    public final int getEncodedSize(AbstractEncoded<?> encoded) throws AmqpEncodingError {
        if (encoded.getValue() == null) {
            return 1;
        }
        switch (category) {
        case FIXED: {
            return 1 + WIDTH;
        }
        case VARIABLE:
        case COMPOUND: {
            return getDataOffset() + encoded.getDataSize();
        }
        case ARRAY: {
            throw new UnsupportedOperationException("Not implemented");
        }
        case DESCRIBED: {
            throw new UnsupportedOperationException("Not implemented");
        }
        default: {
            throw new IllegalArgumentException(category.name());
        }
        }
    }

    public final int getDataOffset() {
        switch (category) {
        case FIXED: {
            return 1;
        }
        case VARIABLE: {
            return 1 + WIDTH;
        }
        case COMPOUND: {
            return 1 + 2 * WIDTH;
        }
        case ARRAY: {
            throw new UnsupportedOperationException("Not implemented");
        }
        case DESCRIBED: {
            throw new UnsupportedOperationException("Not implemented");
        }
        default: {
            throw new IllegalArgumentException(category.name());
        }
        }
    }

    public void marshalPreData(AbstractEncoded<?> encoded, DataOutput out) throws IOException {
        if (encodesSize()) {
            if (WIDTH == 1) {
                Encoder.SINGLETON.writeUbyte((short) encoded.computeDataSize(), out);
            } else {
                Encoder.SINGLETON.writeUint((long) encoded.computeDataSize(), out);
            }
        }
        if (encodesCount()) {
            if (WIDTH == 1) {
                Encoder.SINGLETON.writeUbyte((short) encoded.computeDataCount(), out);
            } else {
                Encoder.SINGLETON.writeUint((long) encoded.computeDataCount(), out);
            }
        }
    }
*/
    public final boolean isFixed() {
        return category == FormatCategory.FIXED;
    }
/*
    public void marshalFormatCode(AbstractEncoded<?> encoded, DataOutput out) throws IOException {
        out.writeByte(encoded.getEncodingFormatCode());
    }
    */
}
