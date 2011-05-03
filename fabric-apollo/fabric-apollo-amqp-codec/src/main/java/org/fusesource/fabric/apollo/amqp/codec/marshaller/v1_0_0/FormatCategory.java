/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fusemq.amqp.codec.marshaller.v1_0_0;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.fusemq.amqp.codec.marshaller.AmqpEncodingError;

import java.io.DataInput;
import java.io.IOException;

/**
*
*/
public enum FormatCategory {
    DESCRIBED(false, false), FIXED(false, false), VARIABLE(true, false), COMPOUND(true, true), ARRAY(true, true);

    private final boolean encodesSize;
    private final boolean encodesCount;

    FormatCategory(boolean encodesSize, boolean encodesCount) {
        this.encodesSize = encodesSize;
        this.encodesCount = encodesCount;
    }

    public static FormatCategory getCategory(byte formatCode) throws IllegalArgumentException {
        switch ((byte) (formatCode & 0xF0)) {
        case (byte) 0x00:
            return DESCRIBED;
        case (byte) 0x40:
        case (byte) 0x50:
        case (byte) 0x60:
        case (byte) 0x70:
        case (byte) 0x80:
        case (byte) 0x90:
            return FIXED;
        case (byte) 0xA0:
        case (byte) 0xB0:
            return VARIABLE;
        case (byte) 0xC0:
        case (byte) 0xD0:
            return COMPOUND;
        case (byte) 0xE0:
        case (byte) 0xF0:
            return ARRAY;
        default:
            throw new IllegalArgumentException("" + formatCode);
        }
    }

    public final boolean encodesSize() {
        return encodesSize;
    }

    public final boolean encodesCount() {
        return encodesCount;
    }

    public static final EncodedBuffer createBuffer(byte formatCode, DataInput in) throws IOException, AmqpEncodingError {
        switch ((byte) (formatCode & 0xF0)) {
        case (byte) 0x00:
            return new DescribedBuffer(formatCode, in);
        case (byte) 0x40:
        case (byte) 0x50:
        case (byte) 0x60:
        case (byte) 0x70:
        case (byte) 0x80:
        case (byte) 0x90:
            return new FixedBuffer(formatCode, in);
        case (byte) 0xA0:
        case (byte) 0xB0:
            return new VariableBuffer(formatCode, in);
        case (byte) 0xC0:
        case (byte) 0xD0:
            return new CompoundBuffer(formatCode, in);
        case (byte) 0xE0:
        case (byte) 0xF0:
            return new ArrayBuffer(formatCode, in);
        default:
            throw new AmqpEncodingError("Invalid format code: " + formatCode);
        }
    }

    public static final EncodedBuffer createBuffer(AbstractEncoded<?> encoded) throws AmqpEncodingError {
        switch ((byte) (encoded.getEncodingFormatCode() & 0xF0)) {
        case (byte) 0x00:
            return new DescribedBuffer(encoded);
        case (byte) 0x40:
        case (byte) 0x50:
        case (byte) 0x60:
        case (byte) 0x70:
        case (byte) 0x80:
        case (byte) 0x90:
            return new FixedBuffer(encoded);
        case (byte) 0xA0:
        case (byte) 0xB0:
            return new VariableBuffer(encoded);
        case (byte) 0xC0:
        case (byte) 0xD0:
            return new CompoundBuffer(encoded);
        case (byte) 0xE0:
        case (byte) 0xF0:
            return new ArrayBuffer(encoded);
        default:
            throw new AmqpEncodingError("Invalid format code: " + encoded.getEncodingFormatCode());
        }
    }

    public static EncodedBuffer createBuffer(Buffer source, int offset) throws AmqpEncodingError {
        return createBuffer(null, source, offset);
    }

    public static EncodedBuffer createBuffer(Byte formatCode, Buffer source, int offset) throws AmqpEncodingError {
        if ( formatCode == null ) {
            formatCode = source.get(offset);
            offset += 1;
        }
        switch ((byte) (formatCode & 0xF0)) {
        case (byte) 0x00:
            return new DescribedBuffer(formatCode, source, offset);
        case (byte) 0x40:
        case (byte) 0x50:
        case (byte) 0x60:
        case (byte) 0x70:
        case (byte) 0x80:
        case (byte) 0x90:
            return new FixedBuffer(formatCode, source, offset);
        case (byte) 0xA0:
        case (byte) 0xB0:
            return new VariableBuffer(formatCode, source, offset);
        case (byte) 0xC0:
        case (byte) 0xD0:
            return new CompoundBuffer(formatCode, source, offset);
        case (byte) 0xE0:
        case (byte) 0xF0:
            return new ArrayBuffer(formatCode, source, offset);
        default:
            throw new AmqpEncodingError("Invalid format code: " + formatCode);
        }
    }
}
