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

    public void encodeUByte(Byte value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Byte decodeUByte(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Byte readUByte(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUByte(Byte value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUInt(Integer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer decodeUInt(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer readUInt(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUInt(Integer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeULong(Long value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeULong(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readULong(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeULong(Long value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUShort(Short value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Short decodeUShort(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Short readUShort(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUShort(Short value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUUID(UUID value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public UUID decodeUUID(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public UUID readUUID(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUUID(UUID value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeArray8(Object[] value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object[] decodeArray8(byte formatCode, Buffer buffer, int offset) {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object[] readArray8(byte formatCode, DataInput in) {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeArray8(Object[] value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeArray32(Object[] value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object[] decodeArray32(byte formatCode, Buffer buffer, int offset) {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object[] readArray32(byte formatCode, DataInput in) {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeArray32(Object[] value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeBinaryVBIN8(Buffer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer decodeBinaryVBIN8(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer readBinaryVBIN8(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeBinaryVBIN8(Buffer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeBinaryVBIN32(Buffer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer decodeBinaryVBIN32(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer readBinaryVBIN32(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeBinaryVBIN32(Buffer value, DataOutput out) {
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

    public void encodeCharUTF32(Character value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Character decodeCharUTF32(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Character readCharUTF32(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeCharUTF32(Character value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeDecimal128IEEE754(BigDecimal value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal decodeDecimal128IEEE754(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal readDecimal128IEEE754(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeDecimal128IEEE754(BigDecimal value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeDecimal32IEEE754(BigDecimal value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal decodeDecimal32IEEE754(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal readDecimal32IEEE754(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeDecimal32IEEE754(BigDecimal value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeDecimal64IEEE754(BigDecimal value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal decodeDecimal64IEEE754(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal readDecimal64IEEE754(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeDecimal64IEEE754(BigDecimal value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeDoubleIEEE754(Double value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Double decodeDoubleIEEE754(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Double readDoubleIEEE754(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeDoubleIEEE754(Double value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeFloatIEEE754(Float value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Float decodeFloatIEEE754(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Float readFloatIEEE754(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeFloatIEEE754(Float value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeIntSmallInt(Integer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer decodeIntSmallInt(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer readIntSmallInt(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeIntSmallInt(Integer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeList8(List value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public List decodeList8(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List readList8(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeList8(List value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeList32(List value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public List decodeList32(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List readList32(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeList32(List value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeLongSmallLong(Long value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeLongSmallLong(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readLongSmallLong(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeLongSmallLong(Long value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeMap8(Map value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map decodeMap8(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map readMap8(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeMap8(Map value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeMap32(Map value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map decodeMap32(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map readMap32(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeMap32(Map value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeStringStr8UTF8(String value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String decodeStringStr8UTF8(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String readStringStr8UTF8(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeStringStr8UTF8(String value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeStringStr32UTF8(String value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String decodeStringStr32UTF8(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String readStringStr32UTF8(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeStringStr32UTF8(String value, DataOutput out) {
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

    public void encodeTimestampMS64(Date value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Date decodeTimestampMS64(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Date readTimestampMS64(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeTimestampMS64(Date value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUIntSmallUInt(Integer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer decodeUIntSmallUInt(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer readUIntSmallUInt(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUIntSmallUInt(Integer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUIntUInt0(Integer value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer decodeUIntUInt0(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer readUIntUInt0(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUIntUInt0(Integer value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeULongSmallULong(Long value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeULongSmallULong(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readULongSmallULong(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeULongSmallULong(Long value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeULongULong0(Long value, Buffer buffer, int offset) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeULongULong0(byte formatCode, Buffer buffer, int offset) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readULongULong0(byte formatCode, DataInput in) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeULongULong0(Long value, DataOutput out) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
