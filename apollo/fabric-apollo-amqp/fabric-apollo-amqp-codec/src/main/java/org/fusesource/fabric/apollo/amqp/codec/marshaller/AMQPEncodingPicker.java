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

import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPType;
import org.fusesource.fabric.apollo.amqp.codec.interfaces.EncodingPicker;
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPMap;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.fusesource.fabric.apollo.amqp.codec.marshaller.ArraySupport.getArrayConstructorSize;
import static org.fusesource.fabric.apollo.amqp.codec.marshaller.TypeRegistry.NULL_FORMAT_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPArray.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPBinary.BINARY_VBIN32_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPBinary.BINARY_VBIN8_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPBoolean.BOOLEAN_FALSE_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPBoolean.BOOLEAN_TRUE_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPByte.BYTE_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPChar.CHAR_UTF32_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPDecimal128.DECIMAL128_IEEE_754_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPDecimal32.DECIMAL32_IEEE_754_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPDecimal64.DECIMAL64_IEEE_754_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPDouble.DOUBLE_IEEE_754_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPFloat.FLOAT_IEEE_754_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPInt.INT_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPInt.INT_SMALLINT_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPList.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPLong.LONG_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPLong.LONG_SMALLLONG_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPMap.MAP_MAP32_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPShort.SHORT_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPString.STRING_STR32_UTF8_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPString.STRING_STR8_UTF8_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPSymbol.SYMBOL_SYM32_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPSymbol.SYMBOL_SYM8_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPTimestamp.TIMESTAMP_MS64_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPUByte.UBYTE_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPUInt.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPULong.*;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPUShort.USHORT_CODE;
import static org.fusesource.fabric.apollo.amqp.codec.types.AMQPUUID.UUID_CODE;

/**
 *
 */
public class AMQPEncodingPicker implements EncodingPicker {

    private static final AMQPEncodingPicker SINGLETON = new AMQPEncodingPicker();

    public static AMQPEncodingPicker instance() {
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
            AMQPType t = (AMQPType)v;
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
            size += ((AMQPType)obj).size();
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
            size += ((AMQPType)key).size();
            size += ((AMQPType)value.get(key)).size();
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
        int size = new UTF8Buffer(value).buffer().length();
        if (size < 254) {
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
