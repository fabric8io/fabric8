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
public class Encoder implements PrimitiveEncoder {

    protected static final Encoder SINGLETON = new Encoder();

    public static Encoder instance() {
        return SINGLETON;
    }

    public Object[] readArray8(DataInput in) throws Exception {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeArray8(Object[] value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeArray8(Object[] value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object[] decodeArray8(Buffer buffer, int offset) throws Exception {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object[] readArray32(DataInput in) throws Exception {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeArray32(Object[] value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeArray32(Object[] value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object[] decodeArray32(Buffer buffer, int offset) throws Exception {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer readBinaryVBIN8(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeBinaryVBIN8(Buffer value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeBinaryVBIN8(Buffer value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer decodeBinaryVBIN8(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer readBinaryVBIN32(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeBinaryVBIN32(Buffer value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeBinaryVBIN32(Buffer value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer decodeBinaryVBIN32(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean readBoolean(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeBoolean(Boolean value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeBoolean(Boolean value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean decodeBoolean(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean readBooleanTrue(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeBooleanTrue(Boolean value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeBooleanTrue(Boolean value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean decodeBooleanTrue(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean readBooleanFalse(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeBooleanFalse(Boolean value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeBooleanFalse(Boolean value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean decodeBooleanFalse(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Byte readByte(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeByte(Byte value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeByte(Byte value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Byte decodeByte(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Character readCharUTF32(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeCharUTF32(Character value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeCharUTF32(Character value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Character decodeCharUTF32(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal readDecimal128IEEE754(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeDecimal128IEEE754(BigDecimal value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeDecimal128IEEE754(BigDecimal value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal decodeDecimal128IEEE754(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal readDecimal32IEEE754(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeDecimal32IEEE754(BigDecimal value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeDecimal32IEEE754(BigDecimal value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal decodeDecimal32IEEE754(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal readDecimal64IEEE754(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeDecimal64IEEE754(BigDecimal value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeDecimal64IEEE754(BigDecimal value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal decodeDecimal64IEEE754(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Double readDoubleIEEE754(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeDoubleIEEE754(Double value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeDoubleIEEE754(Double value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Double decodeDoubleIEEE754(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Float readFloatIEEE754(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeFloatIEEE754(Float value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeFloatIEEE754(Float value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Float decodeFloatIEEE754(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer readInt(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeInt(Integer value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeInt(Integer value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer decodeInt(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer readIntSmallInt(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeIntSmallInt(Integer value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeIntSmallInt(Integer value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer decodeIntSmallInt(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List readList8(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeList8(List value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeList8(List value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public List decodeList8(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List readList32(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeList32(List value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeList32(List value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public List decodeList32(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readLong(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeLong(Long value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeLong(Long value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeLong(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readLongSmallLong(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeLongSmallLong(Long value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeLongSmallLong(Long value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeLongSmallLong(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map readMap8(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeMap8(Map value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeMap8(Map value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map decodeMap8(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map readMap32(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeMap32(Map value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeMap32(Map value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map decodeMap32(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Short readShort(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeShort(Short value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeShort(Short value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Short decodeShort(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String readStringStr8UTF8(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeStringStr8UTF8(String value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeStringStr8UTF8(String value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String decodeStringStr8UTF8(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String readStringStr32UTF8(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeStringStr32UTF8(String value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeStringStr32UTF8(String value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String decodeStringStr32UTF8(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer readSymbolSym8(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeSymbolSym8(Buffer value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeSymbolSym8(Buffer value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer decodeSymbolSym8(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer readSymbolSym32(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeSymbolSym32(Buffer value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeSymbolSym32(Buffer value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer decodeSymbolSym32(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Date readTimestampMS64(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeTimestampMS64(Date value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeTimestampMS64(Date value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Date decodeTimestampMS64(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Byte readUByte(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUByte(Byte value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUByte(Byte value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Byte decodeUByte(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer readUInt(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUInt(Integer value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUInt(Integer value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer decodeUInt(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer readUIntSmallUInt(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUIntSmallUInt(Integer value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUIntSmallUInt(Integer value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer decodeUIntSmallUInt(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer readUIntUInt0(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUIntUInt0(Integer value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUIntUInt0(Integer value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer decodeUIntUInt0(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readULong(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeULong(Long value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeULong(Long value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeULong(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readULongSmallULong(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeULongSmallULong(Long value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeULongSmallULong(Long value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeULongSmallULong(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readULongULong0(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeULongULong0(Long value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeULongULong0(Long value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeULongULong0(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Short readUShort(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUShort(Short value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUShort(Short value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Short decodeUShort(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public UUID readUUID(DataInput in) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeUUID(UUID value, DataOutput out) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeUUID(UUID value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public UUID decodeUUID(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
