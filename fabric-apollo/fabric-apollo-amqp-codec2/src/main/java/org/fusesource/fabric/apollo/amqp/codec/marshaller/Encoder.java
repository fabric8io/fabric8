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

import org.fusesource.fabric.apollo.amqp.codec.interfaces.BaseEncoder;
import org.fusesource.fabric.apollo.amqp.codec.interfaces.PrimitiveEncoder;
import org.fusesource.hawtbuf.Buffer;

import java.io.DataInput;
import java.io.DataOutput;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 */
public class Encoder implements BaseEncoder, PrimitiveEncoder {

    protected static final Encoder SINGLETON = new Encoder();

    public static Encoder instance() {
        return SINGLETON;
    }

    public void encodeAny(Object value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object decodeAny(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object readAny(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeAny(Object value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeArray(Object[] value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object[] decodeArray(byte formatCode, Buffer buffer, int offset) {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object[] readArray(byte formatCode, DataInput in) {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeArray(Object[] value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeBinary(Buffer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer decodeBinary(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer readBinary(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeBinary(Buffer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeBoolean(Boolean value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean decodeBoolean(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean readBoolean(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeBoolean(Boolean value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeByte(Byte value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Byte decodeByte(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Byte readByte(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeByte(Byte value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeChar(Character value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Character decodeChar(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Character readChar(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeChar(Character value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeDecimal128(BigDecimal value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal decodeDecimal128(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal readDecimal128(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeDecimal128(BigDecimal value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeDecimal32(BigDecimal value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal decodeDecimal32(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal readDecimal32(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeDecimal32(BigDecimal value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeDecimal64(BigDecimal value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal decodeDecimal64(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal readDecimal64(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeDecimal64(BigDecimal value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeDouble(Double value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Double decodeDouble(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Double readDouble(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeDouble(Double value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeFloat(Float value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Float decodeFloat(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Float readFloat(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeFloat(Float value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeInt(Integer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer decodeInt(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer readInt(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeInt(Integer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeList(List value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public List decodeList(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List readList(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeList(List value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeLong(Long value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeLong(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readLong(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeLong(Long value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeMap(Map value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map decodeMap(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map readMap(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeMap(Map value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeShort(Short value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Short decodeShort(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Short readShort(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeShort(Short value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeString(String value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String decodeString(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String readString(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeString(String value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeSymbol(Buffer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer decodeSymbol(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer readSymbol(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeSymbol(Buffer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeTimestamp(Date value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Date decodeTimestamp(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Date readTimestamp(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeTimestamp(Date value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUbyte(Byte value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Byte decodeUbyte(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Byte readUbyte(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUbyte(Byte value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUint(Integer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer decodeUint(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer readUint(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUint(Integer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUlong(Long value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeUlong(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readUlong(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUlong(Long value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUshort(Short value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Short decodeUshort(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Short readUshort(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUshort(Short value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUuid(UUID value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public UUID decodeUuid(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public UUID readUuid(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUuid(UUID value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeArrayArray8(Object[] value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object[] decodeArrayArray8(byte formatCode, Buffer buffer, int offset) {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object[] readArrayArray8(byte formatCode, DataInput in) {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeArrayArray8(Object[] value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeArrayArray32(Object[] value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object[] decodeArrayArray32(byte formatCode, Buffer buffer, int offset) {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object[] readArrayArray32(byte formatCode, DataInput in) {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeArrayArray32(Object[] value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeBinaryVbin8(Buffer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer decodeBinaryVbin8(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer readBinaryVbin8(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeBinaryVbin8(Buffer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeBinaryVbin32(Buffer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer decodeBinaryVbin32(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer readBinaryVbin32(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeBinaryVbin32(Buffer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeBooleanTrue(Boolean value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean decodeBooleanTrue(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean readBooleanTrue(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeBooleanTrue(Boolean value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeBooleanFalse(Boolean value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean decodeBooleanFalse(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean readBooleanFalse(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeBooleanFalse(Boolean value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeCharUtf32(Character value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Character decodeCharUtf32(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Character readCharUtf32(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeCharUtf32(Character value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeDecimal128Ieee754(BigDecimal value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal decodeDecimal128Ieee754(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal readDecimal128Ieee754(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeDecimal128Ieee754(BigDecimal value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeDecimal32Ieee754(BigDecimal value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal decodeDecimal32Ieee754(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal readDecimal32Ieee754(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeDecimal32Ieee754(BigDecimal value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeDecimal64Ieee754(BigDecimal value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal decodeDecimal64Ieee754(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal readDecimal64Ieee754(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeDecimal64Ieee754(BigDecimal value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeDoubleIeee754(Double value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Double decodeDoubleIeee754(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Double readDoubleIeee754(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeDoubleIeee754(Double value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeFloatIeee754(Float value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Float decodeFloatIeee754(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Float readFloatIeee754(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeFloatIeee754(Float value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeIntSmallint(Integer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer decodeIntSmallint(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer readIntSmallint(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeIntSmallint(Integer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeListList8(List value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public List decodeListList8(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List readListList8(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeListList8(List value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeListList32(List value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public List decodeListList32(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List readListList32(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeListList32(List value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeLongSmalllong(Long value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeLongSmalllong(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readLongSmalllong(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeLongSmalllong(Long value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeMapMap8(Map value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map decodeMapMap8(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map readMapMap8(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeMapMap8(Map value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeMapMap32(Map value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map decodeMapMap32(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map readMapMap32(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeMapMap32(Map value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeStringStr8Utf8(String value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String decodeStringStr8Utf8(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String readStringStr8Utf8(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeStringStr8Utf8(String value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeStringStr32Utf8(String value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String decodeStringStr32Utf8(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String readStringStr32Utf8(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeStringStr32Utf8(String value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeSymbolSym8(Buffer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer decodeSymbolSym8(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer readSymbolSym8(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeSymbolSym8(Buffer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeSymbolSym32(Buffer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer decodeSymbolSym32(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer readSymbolSym32(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeSymbolSym32(Buffer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeTimestampMs64(Date value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Date decodeTimestampMs64(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Date readTimestampMs64(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeTimestampMs64(Date value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUintSmalluint(Integer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer decodeUintSmalluint(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer readUintSmalluint(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUintSmalluint(Integer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUintUint0(Integer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer decodeUintUint0(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer readUintUint0(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUintUint0(Integer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUlongSmallulong(Long value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeUlongSmallulong(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readUlongSmallulong(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUlongSmallulong(Long value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUlongUlong0(Long value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeUlongUlong0(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readUlongUlong0(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUlongUlong0(Long value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
