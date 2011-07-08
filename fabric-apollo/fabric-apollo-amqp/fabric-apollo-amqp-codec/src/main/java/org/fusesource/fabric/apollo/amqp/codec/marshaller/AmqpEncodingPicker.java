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
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.fusesource.fabric.apollo.amqp.codec.marshaller.ArraySupport.getArrayConstructorSize;
import static org.fusesource.fabric.apollo.amqp.codec.marshaller.TypeRegistry.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPArray.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPBinary.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPBoolean.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPByte.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPChar.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPDecimal128.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPDecimal32.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPDecimal64.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPDouble.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPFloat.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPInt.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPList.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPLong.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPMap.MAP_MAP32_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPShort.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPString.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPSymbol.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPTimestamp.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPUByte.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPUInt.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPULong.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPUShort.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPUUID.*;

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
            return NULL_FORMAT_CODE;
        }
        if (value.length > 255) {
            return ARRAY_ARRAY32_CODE;
        }

        int size = 0;
        size += getArrayConstructorSize(value);
        for (Object v : value) {
            AmqpType t = (AmqpType)v;
            size += t.sizeOfBody();
            if (size > 255 - ARRAY_ARRAY8_WIDTH) {
                return ARRAY_ARRAY32_CODE;
            }
        }
        return ARRAY_ARRAY8_CODE;
    }

    public byte chooseBinaryEncoding(Buffer value) {
        if (value == null) {
            return NULL_FORMAT_CODE;
        }
        if (value.length() > 255) {
            return BINARY_VBIN32_CODE;
        } else {
            return BINARY_VBIN8_CODE;
        }
    }

    public byte chooseBooleanEncoding(Boolean value) {
        if (value == null) {
            return NULL_FORMAT_CODE;
        }
        if (value == Boolean.TRUE) {
            return BOOLEAN_TRUE_CODE;
        } else {
            return BOOLEAN_FALSE_CODE;
        }
    }

    public byte chooseByteEncoding(Byte value) {
        if (value == null) {
            return NULL_FORMAT_CODE;
        }
        return BYTE_CODE;
    }

    public byte chooseCharEncoding(Character value) {
        if (value == null) {
            return NULL_FORMAT_CODE;
        }
        return CHAR_UTF32_CODE;
    }

    public byte chooseDecimal128Encoding(BigDecimal value) {
        if (value == null) {
            return NULL_FORMAT_CODE;
        }
        return DECIMAL128_IEEE_754_CODE;
    }

    public byte chooseDecimal32Encoding(BigDecimal value) {
        if (value == null) {
            return NULL_FORMAT_CODE;
        }
        return DECIMAL32_IEEE_754_CODE;
    }

    public byte chooseDecimal64Encoding(BigDecimal value) {
        if (value == null) {
            return NULL_FORMAT_CODE;
        }
        return DECIMAL64_IEEE_754_CODE;
    }

    public byte chooseDoubleEncoding(Double value) {
        if (value == null) {
            return NULL_FORMAT_CODE;
        }
        return DOUBLE_IEEE_754_CODE;
    }

    public byte chooseFloatEncoding(Float value) {
        if (value == null) {
            return NULL_FORMAT_CODE;
        }
        return FLOAT_IEEE_754_CODE;
    }

    public byte chooseIntEncoding(Integer value) {
        if (value == null) {
            return NULL_FORMAT_CODE;
        }
        if (value < 128 && value > -127) {
            return INT_SMALLINT_CODE;
        }
        return INT_CODE;
    }

    public byte chooseListEncoding(List value) {
        if (value == null) {
            return NULL_FORMAT_CODE;
        }
        if (value.size() == 0) {
            return LIST_LIST0_CODE;
        }
        if (value.size() > 255) {
            return LIST_LIST32_CODE;
        }
        int size = 0;
        for (Object obj : value) {
            size += ((AmqpType)obj).size();
            if (size > (255 - LIST_LIST8_WIDTH)) {
                return LIST_LIST32_CODE;
            }
        }
        return LIST_LIST8_CODE;
    }

    public byte chooseLongEncoding(Long value) {
        if (value == null) {
            return NULL_FORMAT_CODE;
        }
        if (value < 128 && value > -127) {
            return LONG_SMALLLONG_CODE;
        }
        return LONG_CODE;
    }

    public byte chooseMapEncoding(Map value) {
        if (value == null) {
            return NULL_FORMAT_CODE;
        }
        if (value.keySet().size() + value.values().size() > 255) {
            return MAP_MAP32_CODE;
        }
        int size = 0;
        for (Object key : value.keySet()) {
            size += ((AmqpType)key).size();
            size += ((AmqpType)value.get(key)).size();
            if (size > (255 - AMQPMap.MAP_MAP8_WIDTH)) {
                return MAP_MAP32_CODE;
            }
        }
        return AMQPMap.MAP_MAP8_CODE;
    }

    public byte chooseShortEncoding(Short value) {
        if (value == null) {
            return NULL_FORMAT_CODE;
        }
        return SHORT_CODE;
    }

    public byte chooseStringEncoding(String value) {
        if (value == null) {
            return NULL_FORMAT_CODE;
        }
        int size = 0;
        try {
            size = value.getBytes("UTF-8").length;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding not available : " + e.getLocalizedMessage());
        }
        if (size < 255) {
            return STRING_STR8_UTF8_CODE;
        }
        return STRING_STR32_UTF8_CODE;
    }

    public byte chooseSymbolEncoding(Buffer value) {
        if (value == null) {
            return NULL_FORMAT_CODE;
        }
        if (value.length() <= 255) {
            return SYMBOL_SYM8_CODE;
        }
        return SYMBOL_SYM32_CODE;
    }

    public byte chooseTimestampEncoding(Date value) {
        if (value == null) {
            return NULL_FORMAT_CODE;
        }
        return TIMESTAMP_MS64_CODE;
    }

    public byte chooseUByteEncoding(Short value) {
        if (value == null) {
            return NULL_FORMAT_CODE;
        }
        return UBYTE_CODE;
    }

    public byte chooseUIntEncoding(Long value) {
        if (value == null) {
            return NULL_FORMAT_CODE;
        }
        if (value == 0) {
            return UINT_UINT0_CODE;
        }
        if (value < 255 && value > 0) {
            return UINT_SMALLUINT_CODE;
        }
        return UINT_CODE;
    }

    public byte chooseULongEncoding(BigInteger value) {
        if (value == null) {
            return NULL_FORMAT_CODE;
        }
        if (value == BigInteger.ZERO) {
            return ULONG_ULONG0_CODE;
        }
        if ( value.toByteArray().length == 1) {
            return ULONG_SMALLULONG_CODE;
        }
        return ULONG_CODE;
    }

    public byte chooseUShortEncoding(Integer value) {
        if (value == null) {
            return NULL_FORMAT_CODE;
        }
        return USHORT_CODE;
    }

    public byte chooseUUIDEncoding(UUID value) {
        if (value == null) {
            return NULL_FORMAT_CODE;
        }
        return UUID_CODE;
    }
}
