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

import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPType;
import org.fusesource.fabric.apollo.amqp.codec.interfaces.Sizer;
import org.fusesource.hawtbuf.Buffer;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.fusesource.fabric.apollo.amqp.codec.marshaller.ArraySupport.getArrayBodySize;
import static org.fusesource.fabric.apollo.amqp.codec.marshaller.ArraySupport.getArrayConstructorSize;
import static org.fusesource.fabric.apollo.amqp.codec.marshaller.TypeRegistry.NULL_FORMAT_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPArray.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPBinary.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPBoolean.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPByte.BYTE_WIDTH;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPChar.CHAR_UTF32_WIDTH;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPDecimal128.DECIMAL128_IEEE_754_WIDTH;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPDecimal32.DECIMAL32_IEEE_754_WIDTH;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPDecimal64.DECIMAL64_IEEE_754_WIDTH;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPDouble.DOUBLE_IEEE_754_WIDTH;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPFloat.FLOAT_IEEE_754_WIDTH;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPInt.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPList.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPLong.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPMap.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPShort.SHORT_WIDTH;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPString.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPSymbol.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPTimestamp.TIMESTAMP_MS64_WIDTH;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPUByte.UBYTE_WIDTH;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPUInt.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPULong.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPUShort.USHORT_WIDTH;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPUUID.UUID_WIDTH;

/**
 *
 */
public class AMQPSizer implements Sizer {

    protected static final AMQPSizer SINGLETON = new AMQPSizer();

    public static AMQPSizer instance() {
        return SINGLETON;
    }

    public long sizeOfArray(Object[] value) {
        int size = 1;
        byte formatCode = TypeRegistry.instance().picker().chooseArrayEncoding(value);
        switch (formatCode) {
            case NULL_FORMAT_CODE:
                return size;
            case ARRAY_ARRAY8_CODE:
                size += ARRAY_ARRAY8_WIDTH * 2;
                break;
            case ARRAY_ARRAY32_WIDTH:
                size += ARRAY_ARRAY32_WIDTH * 2;
        }

        size += getArrayConstructorSize(value);
        size += getArrayBodySize(value);

        return size;
    }

    public long sizeOfBinary(Buffer value) {
        byte formatCode = TypeRegistry.instance().picker().chooseBinaryEncoding(value);
        switch(formatCode) {
            case NULL_FORMAT_CODE:
                return 1;
            case BINARY_VBIN8_CODE:
                return 1 + BINARY_VBIN8_WIDTH + value.length();
            case BINARY_VBIN32_CODE:
                return 1 + BINARY_VBIN32_WIDTH + value.length();
            default:
                throw new RuntimeException("Unknown format code 0x" + String.format("%x", formatCode));
        }
    }

    public long sizeOfBoolean(Boolean value) {
        byte formatCode = TypeRegistry.instance().picker().chooseBooleanEncoding(value);
        switch(formatCode) {
            case NULL_FORMAT_CODE:
                return 1;
            case BOOLEAN_CODE:
                return 1 + BOOLEAN_WIDTH;
            case BOOLEAN_FALSE_CODE:
            case BOOLEAN_TRUE_CODE:
                    return 1;
            default:
                throw new RuntimeException("Unknown format code 0x" + String.format("%x", formatCode));
        }
    }

    public long sizeOfByte(Byte value) {
        if (value == null) {
            return 1;
        }
        return 1 + BYTE_WIDTH;
    }

    public long sizeOfChar(Character value) {
        if (value == null) {
            return 1;
        }
        return 1 + CHAR_UTF32_WIDTH;
    }

    public long sizeOfDecimal128(BigDecimal value) {
        if (value == null) {
            return 1;
        }
        return 1 + DECIMAL128_IEEE_754_WIDTH;
    }

    public long sizeOfDecimal32(BigDecimal value) {
        if (value == null) {
            return 1;
        }
        return 1 + DECIMAL32_IEEE_754_WIDTH;
    }

    public long sizeOfDecimal64(BigDecimal value) {
        if (value == null) {
            return 1;
        }
        return 1 + DECIMAL64_IEEE_754_WIDTH;
    }

    public long sizeOfDouble(Double value) {
        if (value == null) {
            return 1;
        }
        return 1 + DOUBLE_IEEE_754_WIDTH;
    }

    public long sizeOfFloat(Float value) {
        if (value == null) {
            return 1;
        }
        return 1 + FLOAT_IEEE_754_WIDTH;
    }

    public long sizeOfInt(Integer value) {
        byte formatCode = TypeRegistry.instance().picker().chooseIntEncoding(value);
        switch (formatCode) {
            case NULL_FORMAT_CODE:
                return 1;
            case INT_CODE:
                return 1 + INT_WIDTH;
            case INT_SMALLINT_CODE:
                return 1 + INT_SMALLINT_WIDTH;
            default:
                throw new RuntimeException("Unknown format code 0x" + String.format("%x", formatCode));

        }
    }

    public long sizeOfList(List value) {
        int size = 1;
        byte formatCode = TypeRegistry.instance().picker().chooseListEncoding(value);
        switch (formatCode) {
            case NULL_FORMAT_CODE:
                return size;
            case LIST_LIST0_CODE:
                return size;
            case LIST_LIST8_CODE:
                size += LIST_LIST8_WIDTH * 2;
                break;
            case LIST_LIST32_CODE:
                size += LIST_LIST32_WIDTH * 2;
                break;
            default:
                throw new RuntimeException("Unknown format code 0x" + String.format("%x", formatCode));
        }

        for (Object obj : value) {
            if (obj == null) {
                size += 1;
            } else {
                size += ((AMQPType)obj).size();
            }
        }
        return size;
    }

    public long sizeOfLong(Long value) {
        byte formatCode = TypeRegistry.instance().picker().chooseLongEncoding(value);
        switch (formatCode) {
            case NULL_FORMAT_CODE:
                return 1;
            case LONG_CODE:
                return 1 + LONG_WIDTH;
            case LONG_SMALLLONG_CODE:
                return 1 + LONG_SMALLLONG_WIDTH;
            default:
                throw new RuntimeException("Unknown format code 0x" + String.format("%x", formatCode));
        }
    }

    public long sizeOfMap(Map value) {
        int size = 1;
        byte formatCode = TypeRegistry.instance().picker().chooseMapEncoding(value);
        switch (formatCode) {
            case NULL_FORMAT_CODE:
                return size;
            case MAP_MAP8_CODE:
                size += MAP_MAP8_WIDTH * 2;
                break;
            case MAP_MAP32_CODE:
                size += MAP_MAP32_WIDTH * 2;
                break;
            default:
                throw new RuntimeException("Unknown format code 0x" + String.format("%x", formatCode));
        }

        for (Object key : value.keySet()) {
            size += ((AMQPType)key).size();
            Object obj = value.get(key);
            if (obj == null) {
                size += 1;
            } else {
                size += ((AMQPType)obj).size();
            }
        }
        return size;
    }

    public long sizeOfShort(Short value) {
        if (value == null) {
            return 1;
        }
        return 1 + SHORT_WIDTH;
    }

    public long sizeOfString(String value) {
        byte formatCode = TypeRegistry.instance().picker().chooseStringEncoding(value);
        try {
            switch (formatCode) {
                case NULL_FORMAT_CODE:
                    return 1;
                case STRING_STR8_UTF8_CODE:
                    return 1 + STRING_STR8_UTF8_WIDTH + value.getBytes("UTF-8").length;
                case STRING_STR32_UTF8_CODE:
                    return 1 + STRING_STR32_UTF8_WIDTH + value.getBytes("UTF-8").length;
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
            case NULL_FORMAT_CODE:
                return 1;
            case SYMBOL_SYM8_CODE:
                return 1 + SYMBOL_SYM8_WIDTH + value.length();
            case SYMBOL_SYM32_CODE:
                return 1 + SYMBOL_SYM32_CODE + value.length();
            default:
                throw new RuntimeException("Unknown format code 0x" + String.format("%x", formatCode));
        }
    }

    public long sizeOfTimestamp(Date value) {
        if (value == null) {
            return 1;
        }
        return 1 + TIMESTAMP_MS64_WIDTH;
    }

    public long sizeOfUByte(Short value) {
        if (value == null) {
            return 1;
        }
        return 1 + UBYTE_WIDTH;
    }

    public long sizeOfUInt(Long value) {
        byte formatCode = TypeRegistry.instance().picker().chooseUIntEncoding(value);
        switch(formatCode) {
            case NULL_FORMAT_CODE:
                return 1;
            case UINT_UINT0_CODE:
                return 1;
            case UINT_SMALLUINT_CODE:
                return 1 + UINT_SMALLUINT_WIDTH;
            case UINT_CODE:
                return 1 + UINT_WIDTH;
            default:
                throw new RuntimeException("Unknown format code 0x" + String.format("%x", formatCode));
        }
    }

    public long sizeOfULong(BigInteger value) {
        byte formatCode = TypeRegistry.instance().picker().chooseULongEncoding(value);
        switch (formatCode) {
            case NULL_FORMAT_CODE:
                return 1;
            case ULONG_ULONG0_CODE:
                return 1;
            case ULONG_SMALLULONG_CODE:
                return 1 + ULONG_SMALLULONG_WIDTH;
            case ULONG_CODE:
                return 1 + ULONG_WIDTH;
            default:
                throw new RuntimeException("Unknown format code 0x" + String.format("%x", formatCode));
        }
    }

    public long sizeOfUShort(Integer value) {
        if (value == null) {
            return 1;
        }
        return 1 + USHORT_WIDTH;
    }

    public long sizeOfUUID(UUID value) {
        if (value == null) {
            return 1;
        }
        return 1 + UUID_WIDTH;
    }
}
