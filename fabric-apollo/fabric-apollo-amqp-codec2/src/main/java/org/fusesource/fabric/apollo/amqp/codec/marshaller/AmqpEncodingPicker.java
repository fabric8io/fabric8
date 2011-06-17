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
import org.fusesource.fabric.apollo.amqp.codec.interfaces.EncodingPicker;
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
public class AmqpEncodingPicker implements EncodingPicker {

    private static final AmqpEncodingPicker SINGLETON = new AmqpEncodingPicker();

    public static AmqpEncodingPicker instance() {
        return SINGLETON;
    }

    public byte chooseArrayEncoding(Object[] value) {
        if (value == null) {
            return TypeRegistry.NULL_FORMAT_CODE;
        }
        if (value.length > 255) {
            return AMQPArray.ARRAY_ARRAY32_CODE;
        }

        int size = 0;
        for (Object v : value) {
            AmqpType t = (AmqpType)v;
        }
        return AMQPArray.ARRAY_ARRAY8_CODE;
    }

    public byte chooseBinaryEncoding(Buffer value) {
        if (value == null) {
            return TypeRegistry.NULL_FORMAT_CODE;
        }
        if (value.length() > 255) {
            return AMQPBinary.BINARY_VBIN32_CODE;
        } else {
            return AMQPBinary.BINARY_VBIN8_CODE;
        }
    }

    public byte chooseBooleanEncoding(Boolean value) {
        if (value == null) {
            return TypeRegistry.NULL_FORMAT_CODE;
        }
        if (value == Boolean.TRUE) {
            return AMQPBoolean.BOOLEAN_TRUE_CODE;
        } else {
            return AMQPBoolean.BOOLEAN_FALSE_CODE;
        }
    }

    public byte chooseByteEncoding(Byte value) {
        if (value == null) {
            return TypeRegistry.NULL_FORMAT_CODE;
        }
        return AMQPByte.BYTE_CODE;
    }

    public byte chooseCharEncoding(Character value) {
        if (value == null) {
            return TypeRegistry.NULL_FORMAT_CODE;
        }
        return AMQPChar.CHAR_UTF32_CODE;
    }

    public byte chooseDecimal128Encoding(BigDecimal value) {
        if (value == null) {
            return TypeRegistry.NULL_FORMAT_CODE;
        }
        return AMQPDecimal128.DECIMAL128_IEEE_754_CODE;
    }

    public byte chooseDecimal32Encoding(BigDecimal value) {
        if (value == null) {
            return TypeRegistry.NULL_FORMAT_CODE;
        }
        return AMQPDecimal32.DECIMAL32_IEEE_754_CODE;
    }

    public byte chooseDecimal64Encoding(BigDecimal value) {
        if (value == null) {
            return TypeRegistry.NULL_FORMAT_CODE;
        }
        return AMQPDecimal64.DECIMAL64_IEEE_754_CODE;
    }

    public byte chooseDoubleEncoding(Double value) {
        if (value == null) {
            return TypeRegistry.NULL_FORMAT_CODE;
        }
        return AMQPDouble.DOUBLE_IEEE_754_CODE;
    }

    public byte chooseFloatEncoding(Float value) {
        if (value == null) {
            return TypeRegistry.NULL_FORMAT_CODE;
        }
        return AMQPFloat.FLOAT_IEEE_754_CODE;
    }

    public byte chooseIntEncoding(Integer value) {
        if (value == null) {
            return TypeRegistry.NULL_FORMAT_CODE;
        }
        if (value < 128 && value > -127) {
            return AMQPInt.INT_SMALLINT_CODE;
        }
        return AMQPInt.INT_CODE;
    }

    public byte chooseListEncoding(List value) {
        if (value == null) {
            return TypeRegistry.NULL_FORMAT_CODE;
        }
        if (value.size() > 255) {
            return AMQPList.LIST_LIST32_CODE;
        }
        int size = 0;
        for (Object obj : value) {
            size += ((AmqpType)obj).size();
            if (size > (255 - AMQPList.LIST_LIST8_WIDTH)) {
                return AMQPList.LIST_LIST32_CODE;
            }
        }
        return AMQPList.LIST_LIST8_CODE;
    }

    public byte chooseLongEncoding(Long value) {
        if (value == null) {
            return TypeRegistry.NULL_FORMAT_CODE;
        }
        if (value < 128 && value > -127) {
            return AMQPLong.LONG_SMALLLONG_CODE;
        }
        return AMQPLong.LONG_CODE;
    }

    public byte chooseMapEncoding(Map value) {
        if (value == null) {
            return TypeRegistry.NULL_FORMAT_CODE;
        }
        if (value.keySet().size() + value.values().size() > 255) {
            return AMQPMap.MAP_MAP32_CODE;
        }
        int size = 0;
        for (Object key : value.keySet()) {
            size += ((AmqpType)key).size();
            size += ((AmqpType)value.get(key)).size();
            if (size > (255 - AMQPMap.MAP_MAP8_WIDTH)) {
                return AMQPMap.MAP_MAP32_CODE;
            }
        }
        return AMQPMap.MAP_MAP8_CODE;
    }

    public byte chooseShortEncoding(Short value) {
        if (value == null) {
            return TypeRegistry.NULL_FORMAT_CODE;
        }
        return AMQPShort.SHORT_CODE;
    }

    public byte chooseStringEncoding(String value) {
        if (value == null) {
            return TypeRegistry.NULL_FORMAT_CODE;
        }
        int size = 0;
        try {
            size = value.getBytes("UTF-8").length;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding not available : " + e.getLocalizedMessage());
        }
        if (size < 255) {
            return AMQPString.STRING_STR8_UTF8_CODE;
        }
        return AMQPString.STRING_STR32_UTF8_CODE;
    }

    public byte chooseSymbolEncoding(Buffer value) {
        if (value == null) {
            return TypeRegistry.NULL_FORMAT_CODE;
        }
        if (value.length()  < 255) {
            return AMQPSymbol.SYMBOL_SYM8_CODE;
        }
        return AMQPSymbol.SYMBOL_SYM32_CODE;
    }

    public byte chooseTimestampEncoding(Date value) {
        if (value == null) {
            return TypeRegistry.NULL_FORMAT_CODE;
        }
        return AMQPTimestamp.TIMESTAMP_MS64_CODE;
    }

    public byte chooseUByteEncoding(Byte value) {
        if (value == null) {
            return TypeRegistry.NULL_FORMAT_CODE;
        }
        return AMQPUByte.UBYTE_CODE;
    }

    public byte chooseUIntEncoding(Integer value) {
        if (value == null) {
            return TypeRegistry.NULL_FORMAT_CODE;
        }
        if (value == 0) {
            return AMQPUInt.UINT_UINT0_CODE;
        }
        if (value < 255 && value > 0) {
            return AMQPUInt.UINT_SMALLUINT_CODE;
        }
        return AMQPUInt.UINT_CODE;
    }

    public byte chooseULongEncoding(Long value) {
        if (value == null) {
            return TypeRegistry.NULL_FORMAT_CODE;
        }
        if (value == 0) {
            return AMQPULong.ULONG_ULONG0_CODE;
        }
        if (value < 255 && value > 0) {
            return AMQPULong.ULONG_SMALLULONG_CODE;
        }
        return AMQPULong.ULONG_CODE;
    }

    public byte chooseUShortEncoding(Short value) {
        if (value == null) {
            return TypeRegistry.NULL_FORMAT_CODE;
        }
        return AMQPUShort.USHORT_CODE;
    }

    public byte chooseUUIDEncoding(UUID value) {
        if (value == null) {
            return TypeRegistry.NULL_FORMAT_CODE;
        }
        return AMQPUUID.UUID_CODE;
    }
}
