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

    public Object decodeAny(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object readAny(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeAny(Object value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeArray(Object[] value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object[] decodeArray(Buffer buffer, int offset) {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object[] readArray(DataInput in) {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeArray(Object[] value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeBinary(Buffer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer decodeBinary(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer readBinary(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeBinary(Buffer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeBoolean(Boolean value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean decodeBoolean(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean readBoolean(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeBoolean(Boolean value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeByte(Byte value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Byte decodeByte(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Byte readByte(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeByte(Byte value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeChar(Character value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Character decodeChar(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Character readChar(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeChar(Character value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeDecimal128(BigDecimal value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal decodeDecimal128(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal readDecimal128(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeDecimal128(BigDecimal value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeDecimal32(BigDecimal value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal decodeDecimal32(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal readDecimal32(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeDecimal32(BigDecimal value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeDecimal64(BigDecimal value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal decodeDecimal64(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal readDecimal64(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeDecimal64(BigDecimal value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeDouble(Double value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Double decodeDouble(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Double readDouble(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeDouble(Double value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeFloat(Float value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Float decodeFloat(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Float readFloat(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeFloat(Float value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeInt(Integer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer decodeInt(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer readInt(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeInt(Integer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeList(List value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public List decodeList(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List readList(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeList(List value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeLong(Long value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeLong(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readLong(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeLong(Long value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeMap(Map value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map decodeMap(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map readMap(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeMap(Map value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeShort(Short value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Short decodeShort(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Short readShort(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeShort(Short value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeString(String value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String decodeString(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String readString(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeString(String value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeSymbol(Buffer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer decodeSymbol(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer readSymbol(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeSymbol(Buffer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeTimestamp(Date value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Date decodeTimestamp(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Date readTimestamp(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeTimestamp(Date value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUbyte(Byte value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Byte decodeUbyte(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Byte readUbyte(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUbyte(Byte value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUint(Integer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer decodeUint(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer readUint(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUint(Integer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUlong(Long value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeUlong(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readUlong(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUlong(Long value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUshort(Short value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Short decodeUshort(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Short readUshort(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUshort(Short value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUuid(UUID value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public UUID decodeUuid(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public UUID readUuid(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUuid(UUID value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeArrayArray8(Object[] value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object[] decodeArrayArray8(Buffer buffer, int offset) {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object[] readArrayArray8(DataInput in) {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeArrayArray8(Object[] value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeArrayArray32(Object[] value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object[] decodeArrayArray32(Buffer buffer, int offset) {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object[] readArrayArray32(DataInput in) {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeArrayArray32(Object[] value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeBinaryVbin8(Buffer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer decodeBinaryVbin8(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer readBinaryVbin8(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeBinaryVbin8(Buffer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeBinaryVbin32(Buffer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer decodeBinaryVbin32(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer readBinaryVbin32(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeBinaryVbin32(Buffer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeBooleanTrue(Boolean value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean decodeBooleanTrue(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean readBooleanTrue(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeBooleanTrue(Boolean value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeBooleanFalse(Boolean value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean decodeBooleanFalse(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean readBooleanFalse(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeBooleanFalse(Boolean value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeCharUtf32(Character value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Character decodeCharUtf32(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Character readCharUtf32(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeCharUtf32(Character value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeDecimal128Ieee754(BigDecimal value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal decodeDecimal128Ieee754(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal readDecimal128Ieee754(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeDecimal128Ieee754(BigDecimal value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeDecimal32Ieee754(BigDecimal value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal decodeDecimal32Ieee754(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal readDecimal32Ieee754(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeDecimal32Ieee754(BigDecimal value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeDecimal64Ieee754(BigDecimal value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal decodeDecimal64Ieee754(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal readDecimal64Ieee754(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeDecimal64Ieee754(BigDecimal value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeDoubleIeee754(Double value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Double decodeDoubleIeee754(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Double readDoubleIeee754(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeDoubleIeee754(Double value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeFloatIeee754(Float value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Float decodeFloatIeee754(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Float readFloatIeee754(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeFloatIeee754(Float value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeIntSmallint(Integer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer decodeIntSmallint(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer readIntSmallint(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeIntSmallint(Integer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeListList8(List value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public List decodeListList8(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List readListList8(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeListList8(List value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeListList32(List value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public List decodeListList32(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List readListList32(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeListList32(List value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeLongSmalllong(Long value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeLongSmalllong(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readLongSmalllong(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeLongSmalllong(Long value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeMapMap8(Map value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map decodeMapMap8(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map readMapMap8(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeMapMap8(Map value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeMapMap32(Map value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map decodeMapMap32(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map readMapMap32(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeMapMap32(Map value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeStringStr8Utf8(String value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String decodeStringStr8Utf8(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String readStringStr8Utf8(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeStringStr8Utf8(String value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeStringStr32Utf8(String value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String decodeStringStr32Utf8(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String readStringStr32Utf8(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeStringStr32Utf8(String value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeSymbolSym8(Buffer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer decodeSymbolSym8(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer readSymbolSym8(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeSymbolSym8(Buffer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeSymbolSym32(Buffer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer decodeSymbolSym32(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer readSymbolSym32(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeSymbolSym32(Buffer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeTimestampMs64(Date value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Date decodeTimestampMs64(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Date readTimestampMs64(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeTimestampMs64(Date value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUintSmalluint(Integer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer decodeUintSmalluint(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer readUintSmalluint(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUintSmalluint(Integer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUintUint0(Integer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer decodeUintUint0(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer readUintUint0(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUintUint0(Integer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUlongSmallulong(Long value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeUlongSmallulong(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readUlongSmallulong(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUlongSmallulong(Long value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUlongUlong0(Long value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeUlongUlong0(Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readUlongUlong0(DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUlongUlong0(Long value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
