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

import org.fusesource.fabric.apollo.amqp.codec.interfaces.AmqpType;
import org.fusesource.fabric.apollo.amqp.codec.interfaces.Sizer;
import org.fusesource.fabric.apollo.amqp.codec.types.*;
import org.fusesource.hawtbuf.Buffer;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 */
public class AmqpSizer implements Sizer {

    protected static final AmqpSizer SINGLETON = new AmqpSizer();

    public static AmqpSizer instance() {
        return SINGLETON;
    }

    public long sizeOfArray(Object[] value) {
        if (value == null) {
            return 1;
        }
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public long sizeOfBinary(Buffer value) {
        byte formatCode = TypeRegistry.instance().picker().chooseBinaryEncoding(value);
        switch(formatCode) {
            case TypeRegistry.NULL_FORMAT_CODE:
                return 1;
            case AMQPBinary.BINARY_VBIN8_CODE:
                return 1 + AMQPBinary.BINARY_VBIN8_WIDTH + value.length();
            case AMQPBinary.BINARY_VBIN32_CODE:
                return 1 + AMQPBinary.BINARY_VBIN32_WIDTH + value.length();
            default:
                throw new RuntimeException("Unknown format code 0x" + String.format("%x", formatCode));
        }
    }

    public long sizeOfBoolean(Boolean value) {
        byte formatCode = TypeRegistry.instance().picker().chooseBooleanEncoding(value);
        switch(formatCode) {
            case TypeRegistry.NULL_FORMAT_CODE:
                return 1;
            case AMQPBoolean.BOOLEAN_CODE:
                return 1 + AMQPBoolean.BOOLEAN_WIDTH;
            case AMQPBoolean.BOOLEAN_FALSE_CODE:
            case AMQPBoolean.BOOLEAN_TRUE_CODE:
                    return 1;
            default:
                throw new RuntimeException("Unknown format code 0x" + String.format("%x", formatCode));
        }
    }

    public long sizeOfByte(Byte value) {
        if (value == null) {
            return 1;
        }
        return 1 + AMQPByte.BYTE_WIDTH;
    }

    public long sizeOfChar(Character value) {
        if (value == null) {
            return 1;
        }
        return 1 + AMQPChar.CHAR_UTF32_WIDTH;
    }

    public long sizeOfDecimal128(BigDecimal value) {
        if (value == null) {
            return 1;
        }
        return 1 + AMQPDecimal128.DECIMAL128_IEEE_754_WIDTH;
    }

    public long sizeOfDecimal32(BigDecimal value) {
        if (value == null) {
            return 1;
        }
        return 1 + AMQPDecimal32.DECIMAL32_IEEE_754_WIDTH;
    }

    public long sizeOfDecimal64(BigDecimal value) {
        if (value == null) {
            return 1;
        }
        return 1 + AMQPDecimal64.DECIMAL64_IEEE_754_WIDTH;
    }

    public long sizeOfDouble(Double value) {
        if (value == null) {
            return 1;
        }
        return 1 + AMQPDouble.DOUBLE_IEEE_754_WIDTH;
    }

    public long sizeOfFloat(Float value) {
        if (value == null) {
            return 1;
        }
        return 1 + AMQPFloat.FLOAT_IEEE_754_WIDTH;
    }

    public long sizeOfInt(Integer value) {
        byte formatCode = TypeRegistry.instance().picker().chooseIntEncoding(value);
        switch (formatCode) {
            case TypeRegistry.NULL_FORMAT_CODE:
                return 1;
            case AMQPInt.INT_CODE:
                return 1 + AMQPInt.INT_WIDTH;
            case AMQPInt.INT_SMALLINT_CODE:
                return 1 + AMQPInt.INT_SMALLINT_WIDTH;
            default:
                throw new RuntimeException("Unknown format code 0x" + String.format("%x", formatCode));

        }
    }

    public long sizeOfList(List value) {
        int size = 1;
        byte formatCode = TypeRegistry.instance().picker().chooseListEncoding(value);
        switch (formatCode) {
            case TypeRegistry.NULL_FORMAT_CODE:
                return size;
            case AMQPList.LIST_LIST8_CODE:
                size += AMQPList.LIST_LIST8_WIDTH * 2;
                break;
            case AMQPList.LIST_LIST32_CODE:
                size += AMQPList.LIST_LIST32_WIDTH * 2;
                break;
            default:
                throw new RuntimeException("Unknown format code 0x" + String.format("%x", formatCode));
        }

        for (Object obj : value) {
            size += ((AmqpType)obj).size();
        }
        return size;
    }

    public long sizeOfLong(Long value) {
        byte formatCode = TypeRegistry.instance().picker().chooseLongEncoding(value);
        switch (formatCode) {
            case TypeRegistry.NULL_FORMAT_CODE:
                return 1;
            case AMQPLong.LONG_CODE:
                return 1 + AMQPLong.LONG_WIDTH;
            case AMQPLong.LONG_SMALLLONG_CODE:
                return 1 + AMQPLong.LONG_SMALLLONG_WIDTH;
            default:
                throw new RuntimeException("Unknown format code 0x" + String.format("%x", formatCode));
        }
    }

    public long sizeOfMap(Map value) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public long sizeOfShort(Short value) {
        if (value == null) {
            return 1;
        }
        return 1 + AMQPShort.SHORT_WIDTH;
    }

    public long sizeOfString(String value) {
        byte formatCode = TypeRegistry.instance().picker().chooseStringEncoding(value);
        try {
            switch (formatCode) {
                case TypeRegistry.NULL_FORMAT_CODE:
                    return 1;
                case AMQPString.STRING_STR8_UTF8_CODE:
                    return 1 + AMQPString.STRING_STR8_UTF8_WIDTH + value.getBytes("UTF-8").length;
                case AMQPString.STRING_STR32_UTF8_CODE:
                    return 1 + AMQPString.STRING_STR32_UTF8_WIDTH + value.getBytes("UTF-8").length;
                default:
                    throw new RuntimeException("Unknown format code 0x" + String.format("%x", formatCode));
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding not available : " + e.getMessage());
        }
    }

    public long sizeOfSymbol(Buffer value) {
        byte formatCode = TypeRegistry.instance().picker().chooseSymbolEncoding(value);
        switch (formatCode) {
            case TypeRegistry.NULL_FORMAT_CODE:
                return 1;
            case AMQPSymbol.SYMBOL_SYM8_CODE:
                return 1 + AMQPSymbol.SYMBOL_SYM8_WIDTH + value.length();
            case AMQPSymbol.SYMBOL_SYM32_CODE:
                return 1 + AMQPSymbol.SYMBOL_SYM32_CODE + value.length();
            default:
                throw new RuntimeException("Unknown format code 0x" + String.format("%x", formatCode));
        }
    }

    public long sizeOfTimestamp(Date value) {
        if (value == null) {
            return 1;
        }
        return 1 + AMQPTimestamp.TIMESTAMP_MS64_WIDTH;
    }

    public long sizeOfUByte(Byte value) {
        if (value == null) {
            return 1;
        }
        return 1 + AMQPUByte.UBYTE_WIDTH;
    }

    public long sizeOfUInt(Integer value) {
        byte formatCode = TypeRegistry.instance().picker().chooseUIntEncoding(value);
        switch(formatCode) {
            case TypeRegistry.NULL_FORMAT_CODE:
                return 1;
            case AMQPUInt.UINT_UINT0_CODE:
                return 1;
            case AMQPUInt.UINT_SMALLUINT_CODE:
                return 1 + AMQPUInt.UINT_SMALLUINT_WIDTH;
            case AMQPUInt.UINT_CODE:
                return 1 + AMQPUInt.UINT_WIDTH;
            default:
                throw new RuntimeException("Unknown format code 0x" + String.format("%x", formatCode));
        }
    }

    public long sizeOfULong(Long value) {
        byte formatCode = TypeRegistry.instance().picker().chooseULongEncoding(value);
        switch (formatCode) {
            case TypeRegistry.NULL_FORMAT_CODE:
                return 1;
            case AMQPULong.ULONG_ULONG0_CODE:
                return 1;
            case AMQPULong.ULONG_SMALLULONG_CODE:
                return 1 + AMQPULong.ULONG_SMALLULONG_WIDTH;
            case AMQPULong.ULONG_CODE:
                return 1 + AMQPULong.ULONG_WIDTH;
            default:
                throw new RuntimeException("Unknown format code 0x" + String.format("%x", formatCode));
        }
    }

    public long sizeOfUShort(Short value) {
        if (value == null) {
            return 1;
        }
        return 1 + AMQPUShort.USHORT_WIDTH;
    }

    public long sizeOfUUID(UUID value) {
        if (value == null) {
            return 1;
        }
        return 1 + AMQPUUID.UUID_WIDTH;
    }
}
