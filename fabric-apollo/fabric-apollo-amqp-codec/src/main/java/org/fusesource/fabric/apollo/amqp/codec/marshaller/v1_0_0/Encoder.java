/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.codec.marshaller.v1_0_0;

import org.fusesource.fabric.apollo.amqp.codec.marshaller.AmqpEncodingError;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.Encoded;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.UnexpectedTypeException;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.v1_0_0.AmqpBinaryMarshaller.BINARY_ENCODING;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.v1_0_0.AmqpBooleanMarshaller.BOOLEAN_ENCODING;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.v1_0_0.AmqpListMarshaller.LIST_ENCODING;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.v1_0_0.AmqpMapMarshaller.MAP_ENCODING;
import org.fusesource.fabric.apollo.amqp.codec.types.AmqpType;
import org.fusesource.fabric.apollo.amqp.codec.types.IAmqpList;
import org.fusesource.fabric.apollo.amqp.codec.types.IAmqpMap;
import org.fusesource.hawtbuf.Buffer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Map;

import static org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory.createAmqpNull;

public class Encoder extends BaseEncoder {

    public static final Encoder SINGLETON = new Encoder();
    public static final AmqpMarshaller MARSHALLER = AmqpMarshaller.getMarshaller();

    static final byte NULL_FORMAT_CODE = AmqpNullMarshaller.FORMAT_CODE;
    static final byte DESCRIBED_FORMAT_CODE = (byte) 0x00;

    static final ListDecoder<AmqpType<?, ?>> DEFAULT_LIST_DECODER = new ListDecoderImpl();
    static final MapDecoder<AmqpType<?, ?>, AmqpType<?, ?>> DEFAULT_MAP_DECODER = new MapDecoderImpl();

    private Encoder() {

    }

    // ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Encoding Helpers:
    // ///////////////////////////////////////////////////////////////////////////////////////////////////

    public static final AmqpType<?, ?> decode(Buffer source) throws AmqpEncodingError {
        EncodedBuffer buffer = FormatCategory.createBuffer(source, 0);
        return MARSHALLER.decodeType(buffer);
    }

    public static final AmqpType<?, ?> unmarshalType(DataInput in) throws IOException, AmqpEncodingError {
        return MARSHALLER.decodeType(FormatCategory.createBuffer(in.readByte(), in));
    }

    public final Boolean valueOfBoolean(AmqpBooleanMarshaller.BOOLEAN_ENCODING encoding) {
        return encoding == AmqpBooleanMarshaller.BOOLEAN_ENCODING.TRUE;
    }

    public final Boolean valueOfNull() {
        return null;
    }

    public static final AmqpBinaryMarshaller.BINARY_ENCODING chooseBinaryEncoding(Buffer val) throws AmqpEncodingError {
        if (val.length > 255) {
            return AmqpBinaryMarshaller.BINARY_ENCODING.VBIN32;
        }
        return AmqpBinaryMarshaller.BINARY_ENCODING.VBIN8;
    }

    public static final AmqpBooleanMarshaller.BOOLEAN_ENCODING chooseBooleanEncoding(boolean val) throws AmqpEncodingError {
        if (val) {
            return AmqpBooleanMarshaller.BOOLEAN_ENCODING.TRUE;
        }
        return AmqpBooleanMarshaller.BOOLEAN_ENCODING.FALSE;
    }

    public static final AmqpStringMarshaller.STRING_ENCODING chooseStringEncoding(String val) throws AmqpEncodingError {
        try {
            if (val.length() > 255 || val.getBytes("utf-8").length > 255) {
                return AmqpStringMarshaller.STRING_ENCODING.STR32_UTF8;
            }
        } catch (UnsupportedEncodingException uee) {
            throw new AmqpEncodingError(uee.getMessage(), uee);
        }

        return AmqpStringMarshaller.STRING_ENCODING.STR8_UTF8;
    }

    public static final AmqpSymbolMarshaller.SYMBOL_ENCODING chooseSymbolEncoding(String val) throws AmqpEncodingError {
        try {
            if (val.length() > 255 || val.getBytes("ascii").length > 255) {
                return AmqpSymbolMarshaller.SYMBOL_ENCODING.SYM32;
            }
        } catch (UnsupportedEncodingException uee) {
            throw new AmqpEncodingError(uee.getMessage(), uee);
        }
        return AmqpSymbolMarshaller.SYMBOL_ENCODING.SYM8;
    }

    public final int getEncodedSizeOfBinary(Buffer val, BINARY_ENCODING encoding) {
        return val.length;
    }

    public final int getEncodedSizeOfBoolean(boolean val, BOOLEAN_ENCODING encoding) {
        return 0;
    }

    // ///////////////////////////////////////////////////////////////////////////////////////////////////
    // LIST ENCODINGS
    // ///////////////////////////////////////////////////////////////////////////////////////////////////
    public static final <E extends AmqpType<?, ?>> AmqpListMarshaller.LIST_ENCODING chooseListEncoding(IAmqpList<E> val) throws AmqpEncodingError {
        boolean useArrayEncoding = useArrayEncoding(val);
        if (!Boolean.parseBoolean(System.getProperty("org.fusesource.fabric.apollo.amqp.codec.Use8BitListEncodings"))) {
            if ( useArrayEncoding ) {
                return AmqpListMarshaller.LIST_ENCODING.ARRAY32;
            } else {
                return AmqpListMarshaller.LIST_ENCODING.LIST32;
            }
        }
        int size = getEncodedSizeOfList(0, val, useArrayEncoding);

        if ( useArrayEncoding ) {
            size += 1;
            if ( size > 254) {
                return AmqpListMarshaller.LIST_ENCODING.ARRAY32;
            } else {
                return AmqpListMarshaller.LIST_ENCODING.ARRAY8;
            }
        } else {
            if (size > 254) {
                return AmqpListMarshaller.LIST_ENCODING.LIST32;
            } else {
                return AmqpListMarshaller.LIST_ENCODING.LIST8;
            }
        }
    }

    private static <E extends AmqpType<?, ?>> boolean useArrayEncoding(IAmqpList<E> val) {
        if ( Boolean.parseBoolean(System.getProperty("org.fusesource.fabric.apollo.amqp.codec.NoArrayEncoding")) ) {
            return false;
        }
        if (val.getListCount() == 1) {
            return false;
        }
        E lastElement = null;
        int currentElement = 0;
        int max = val.getListCount();
        for (int i = 0; i < max; i++) {
            E le = val.get(i);
            if ( lastElement != null && currentElement != 0 ) {
                if ( le == null || le.getClass() != lastElement.getClass() ) {
                    return false;
                }
            }
            if ( lastElement == null && currentElement != 0 ) {
                return false;
            }
            currentElement++;
            lastElement = le;
        }
        return true;
    }

    private static <E extends AmqpType<?, ?>> int getEncodedSizeOfList(int size, IAmqpList<E> val, boolean array) {
        int max = val.getListCount();
        for (int i = 0; i < max; i++) {
            E le = val.get(i);
            if ( le == null ) {
                size += 1;
            } else {
                Encoded<?> encoded = le.getBuffer(MARSHALLER).getEncoded();
                if (!array) {
                    size += encoded.getEncodedSize();
                } else {
                    size += encoded.getEncodedSize() - 1;
                }
            }
        }
        return size;
    }

    public final <E extends AmqpType<?, ?>> int getEncodedSizeOfList(IAmqpList<E> val, LIST_ENCODING encoding) throws AmqpEncodingError {
        switch (encoding) {
            // count + constructor
            case ARRAY32:
                return getEncodedSizeOfList(4 + 1, val, true);
            case ARRAY8: {
                return getEncodedSizeOfList(1 + 1, val, true);
            }
            case LIST32:
                return getEncodedSizeOfList(4, val, false);
            case LIST8: {
                return getEncodedSizeOfList(1, val, false);
            }
            default: {
                throw new IllegalArgumentException(encoding.name());
            }
        }
    }

    public final <E extends AmqpType<?, ?>> int getEncodedCountOfList(IAmqpList<E> val, LIST_ENCODING listENCODING) throws AmqpEncodingError {
        return val.getListCount();
    }

    // List 8 encoding
    public <E extends AmqpType<?, ?>> void encodeListList8(IAmqpList<E> value, Buffer encoded, int offset) throws AmqpEncodingError {
        encodeList(value, encoded, offset);
    }

    public <E extends AmqpType<?, ?>> void writeListList8(IAmqpList<E> val, DataOutput out) throws IOException, AmqpEncodingError {
        writeList(val, out);
    }

    // List 32 encoding:
    public <E extends AmqpType<?, ?>> void encodeListList32(IAmqpList<E> value, Buffer encoded, int offset) throws AmqpEncodingError {
        encodeList(value, encoded, offset);
    }

    public <E extends AmqpType<?, ?>> void writeListList32(IAmqpList<E> val, DataOutput out) throws IOException, AmqpEncodingError {
        writeList(val, out);
    }

    // Array 8 encoding
    public <E extends AmqpType<?, ?>> void encodeListArray8(IAmqpList<E> value, Buffer encoded, int offset) throws AmqpEncodingError {
        encodeArray(value, encoded, offset);
    }

    public <E extends AmqpType<?, ?>> void writeListArray8(IAmqpList<E> val, DataOutput out) throws IOException, AmqpEncodingError {
        writeArray(val, out);
    }

    // Array 32 encoding:
    public <E extends AmqpType<?, ?>> void encodeListArray32(IAmqpList<E> value, Buffer encoded, int offset) throws AmqpEncodingError {
        encodeArray(value, encoded, offset);
    }

    public <E extends AmqpType<?, ?>> void writeListArray32(IAmqpList<E> val, DataOutput out) throws IOException, AmqpEncodingError {
        writeArray(val, out);
    }

    // Generic versions:
    public static final <E extends AmqpType<?, ?>> void encodeList(IAmqpList<E> value, Buffer target, int offset) throws AmqpEncodingError {
        for (E le : value) {
            Encoded<?> encoded = le.getBuffer(MARSHALLER).getEncoded();
            encoded.encode(target, offset);
            offset += encoded.getDataSize();
        }
    }

    public static final <E extends AmqpType<?, ?>> void writeList(IAmqpList<E> val, DataOutput out) throws IOException, AmqpEncodingError {
        for (E le : val) {
            if (le == null) {
                out.writeByte(NULL_FORMAT_CODE);
            } else {
                le.marshal(out, MARSHALLER);
            }
        }
    }

    public static final <E extends AmqpType<?, ?>> void encodeArray(IAmqpList<E> value, Buffer target, int offset) throws AmqpEncodingError {
        target.data[offset] = value.get(0).getBuffer(MARSHALLER).getEncoded().getEncodingFormatCode();
        offset++;
        for ( E le : value ) {
            Encoded<?> encoded = le.getBuffer(MARSHALLER).getEncoded();
            encoded.encode(target, offset);
            offset += encoded.getDataSize();
        }
    }

    public static final <E extends AmqpType<?, ?>> void writeArray(IAmqpList<E> value, DataOutput out) throws IOException, AmqpEncodingError {
        if (value.getListCount() == 0) {
            out.writeByte(Encoder.NULL_FORMAT_CODE);
            return;
        }
        boolean first = true;
        for ( E le : value ) {
            Encoded<?> encoded = le.getBuffer(MARSHALLER).getEncoded();
            if ( first ) {
                encoded.marshal(out);
                first = false;
            } else {
                encoded.marshalConstructor(out);
                encoded.marshalData(out);
            }
        }
    }

    // ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Map ENCODINGS
    // ///////////////////////////////////////////////////////////////////////////////////////////////////
    public static final AmqpMapMarshaller.MAP_ENCODING chooseMapEncoding(IAmqpMap<?, ?> map) throws AmqpEncodingError {
        int size = 0;
        for (Map.Entry<? extends AmqpType<?, ?>, ? extends AmqpType<?, ?>> me : map) {
            if ( me.getKey() != null ) {
                size += me.getKey().getBuffer(MARSHALLER).getEncoded().getEncodedSize();
            }
            if ( me.getValue() != null ) {
                size += me.getValue().getBuffer(MARSHALLER).getEncoded().getEncodedSize();
            } else {
                size += createAmqpNull(null).getBuffer(MARSHALLER).getEncoded().getEncodedSize();
            }
            if (size > 254) {
                return AmqpMapMarshaller.MAP_ENCODING.MAP32;
            }
        }
        return AmqpMapMarshaller.MAP_ENCODING.MAP8;
    }

    public final int getEncodedSizeOfMap(IAmqpMap<?, ?> map, MAP_ENCODING encoding) throws AmqpEncodingError {
        int size = 0;

        switch (encoding) {
            case MAP32:
                size = 4;
                break;
            case MAP8:
                size = 1;
                break;
            default:
                throw new IllegalArgumentException(encoding.name());
        }
        for (Map.Entry<? extends AmqpType<?, ?>, ? extends AmqpType<?, ?>> me : map) {
            if ( me.getKey() != null ) {
                size += me.getKey().getBuffer(MARSHALLER).getEncoded().getEncodedSize();
            }
            if ( me.getValue() != null ) {
                size += me.getValue().getBuffer(MARSHALLER).getEncoded().getEncodedSize();
            } else {
                size += createAmqpNull(null).getBuffer(MARSHALLER).getEncoded().getEncodedSize();
            }
        }
        return size;
    }

    public final int getEncodedCountOfMap(IAmqpMap<?, ?> map, MAP_ENCODING mapENCODING) throws AmqpEncodingError {
        return map.getEntryCount() * 2;
    }

    public final void encodeMapMap32(IAmqpMap<?, ?> value, Buffer target, int offset) throws AmqpEncodingError {
        encodeMap(value, target, offset);
    }

    public final void writeMapMap32(IAmqpMap<?, ?> val, DataOutput out) throws AmqpEncodingError, IOException {
        writeMap(val, out);
    }

    public final void encodeMapMap8(IAmqpMap<?, ?> value, Buffer target, int offset) throws AmqpEncodingError {
        encodeMap(value, target, offset);
    }

    public final void writeMapMap8(IAmqpMap<?, ?> val, DataOutput out) throws AmqpEncodingError, IOException {
        writeMap(val, out);

    }

    public static final void encodeMap(IAmqpMap<?, ?> value, Buffer target, int offset) throws AmqpEncodingError {
        for (Map.Entry<? extends AmqpType<?, ?>, ? extends AmqpType<?, ?>> me : value) {
            Encoded<?> eKey = me.getKey().getBuffer(MARSHALLER).getEncoded();
            eKey.encode(target, offset);
            offset += eKey.getEncodedSize();

            Encoded<?> eVal = me.getValue().getBuffer(MARSHALLER).getEncoded();
            eVal.encode(target, offset);
            offset += eVal.getEncodedSize();
        }
    }

    public static final void writeMap(IAmqpMap<?, ?> val, DataOutput out) throws IOException, AmqpEncodingError {
        for (Map.Entry<? extends AmqpType<?, ?>, ? extends AmqpType<?, ?>> me : val) {
            if ( me.getKey() != null ) {
                me.getKey().marshal(out, MARSHALLER);
            }
            if ( me.getValue() != null ) {
                me.getValue().marshal(out, MARSHALLER);
            } else {
                createAmqpNull(null).marshal(out, MARSHALLER);
            }
        }
    }

    public int getEncodedSizeOfUint(Long value, AmqpUintMarshaller.UINT_ENCODING encoding) {
        switch (encoding) {
            case UINT:
                return 4;
            case SMALLUINT:
                return 1;
            default:
                throw new UnexpectedTypeException("Unknown UINT_ENCODING");
        }
    }

    public static AmqpUintMarshaller.UINT_ENCODING chooseUintEncoding(Long value) {
        if (value >= 0 && value <= 255) {
            return AmqpUintMarshaller.UINT_ENCODING.SMALLUINT;
        } else {
            return AmqpUintMarshaller.UINT_ENCODING.UINT;
        }
    }

    public int getEncodedSizeOfUlong(BigInteger value, AmqpUlongMarshaller.ULONG_ENCODING encoding) {
        switch (encoding) {
            case ULONG:
                return 8;
            case SMALLULONG:
                return 1;
            default:
                throw new UnexpectedTypeException("Unknown ULONG_ENCODING");
        }
    }

    public static AmqpUlongMarshaller.ULONG_ENCODING chooseUlongEncoding(BigInteger value) {
        if (value.longValue() >= 0 && value.longValue() <= 255) {
            return AmqpUlongMarshaller.ULONG_ENCODING.SMALLULONG;
        } else {
            return AmqpUlongMarshaller.ULONG_ENCODING.ULONG;
        }
    }

    public int getEncodedSizeOfLong(Long value, AmqpLongMarshaller.LONG_ENCODING encoding) {
        switch (encoding) {
            case LONG:
                return 8;
            case SMALLLONG:
                return 1;
            default:
                throw new UnexpectedTypeException("Unknown LONG_ENCODING");
        }
    }

    public static AmqpLongMarshaller.LONG_ENCODING chooseLongEncoding(Long value) {
        if (value >= 0 && value <= 255) {
            return AmqpLongMarshaller.LONG_ENCODING.SMALLLONG;
        } else {
            return AmqpLongMarshaller.LONG_ENCODING.LONG;
        }
    }

    public int getEncodedSizeOfInt(Integer value, AmqpIntMarshaller.INT_ENCODING encoding) {
        switch (encoding) {
            case INT:
                return 4;
            case SMALLINT:
                return 1;
            default:
                throw new UnexpectedTypeException("Unknown INT_ENCODING");
        }
    }

    public static AmqpIntMarshaller.INT_ENCODING chooseIntEncoding(Integer value) {
        if (value >= 0 && value <= 255) {
            return AmqpIntMarshaller.INT_ENCODING.SMALLINT;
        } else {
            return AmqpIntMarshaller.INT_ENCODING.INT;
        }
    }
}
