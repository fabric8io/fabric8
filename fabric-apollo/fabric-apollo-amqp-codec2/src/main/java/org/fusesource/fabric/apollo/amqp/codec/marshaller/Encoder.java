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
import org.fusesource.fabric.apollo.amqp.codec.interfaces.PrimitiveEncoder;
import org.fusesource.fabric.apollo.amqp.codec.types.*;
import org.fusesource.hawtbuf.Buffer;

import java.io.DataInput;
import java.io.DataOutput;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.charset.Charset;
import java.util.*;

/**
 *
 */
public class Encoder implements PrimitiveEncoder {

    protected static final Encoder SINGLETON = new Encoder();

    public static Encoder instance() {
        return SINGLETON;
    }

    public void writeAny(Object value, DataInput in) throws Exception {

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
        int size = in.readUnsignedByte();
        Buffer rc = new Buffer(size);
        rc.readFrom(in);
        return rc;
    }

    public void writeBinaryVBIN8(Buffer value, DataOutput out) throws Exception {
        out.writeByte(AMQPBinary.BINARY_VBIN8_CODE);
        out.writeByte(value.length());
        value.writeTo(out);
    }

    public void encodeBinaryVBIN8(Buffer value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer decodeBinaryVBIN8(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer readBinaryVBIN32(DataInput in) throws Exception {
        int size = in.readInt();
        Buffer rc = new Buffer(size);
        rc.readFrom(in);
        return rc;
    }

    public void writeBinaryVBIN32(Buffer value, DataOutput out) throws Exception {
        out.writeByte(AMQPBinary.BINARY_VBIN32_CODE);
        out.writeInt(value.length());
        value.writeTo(out);
    }

    public void encodeBinaryVBIN32(Buffer value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Buffer decodeBinaryVBIN32(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean readBoolean(DataInput in) throws Exception {
        byte val = in.readByte();
        if (val == 0) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    public void writeBoolean(Boolean value, DataOutput out) throws Exception {
        out.writeByte(AMQPBoolean.BOOLEAN_CODE);
        if (value) {
            out.writeByte(1);
        } else {
            out.writeByte(0);
        }
    }

    public void encodeBoolean(Boolean value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean decodeBoolean(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean readBooleanTrue(DataInput in) throws Exception {
        return Boolean.TRUE;
    }

    public void writeBooleanTrue(Boolean value, DataOutput out) throws Exception {
        out.writeByte(AMQPBoolean.BOOLEAN_TRUE_CODE);
    }

    public void encodeBooleanTrue(Boolean value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean decodeBooleanTrue(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean readBooleanFalse(DataInput in) throws Exception {
        return Boolean.FALSE;
    }

    public void writeBooleanFalse(Boolean value, DataOutput out) throws Exception {
        out.writeByte(AMQPBoolean.BOOLEAN_FALSE_CODE);
    }

    public void encodeBooleanFalse(Boolean value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean decodeBooleanFalse(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Byte readByte(DataInput in) throws Exception {
        return in.readByte();
    }

    public void writeByte(Byte value, DataOutput out) throws Exception {
        out.writeByte(AMQPByte.BYTE_CODE);
        out.writeByte(value);
    }

    public void encodeByte(Byte value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Byte decodeByte(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Character readCharUTF32(DataInput in) throws Exception {
        return in.readChar();
    }

    public void writeCharUTF32(Character value, DataOutput out) throws Exception {
        out.writeByte(AMQPChar.CHAR_UTF32_CODE);
        out.writeChar(value);
    }

    public void encodeCharUTF32(Character value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Character decodeCharUTF32(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal readDecimal128IEEE754(DataInput in) throws Exception {
        // TODO - support Decimal128
        throw new RuntimeException("Decimal128 not supported");
    }

    public void writeDecimal128IEEE754(BigDecimal value, DataOutput out) throws Exception {
        // TODO - support Decimal128
        throw new RuntimeException("Decimal128 not supported");
    }

    public void encodeDecimal128IEEE754(BigDecimal value, Buffer buffer, int offset) throws Exception {
        // TODO - support Decimal128
        throw new RuntimeException("Decimal128 not supported");
    }

    public BigDecimal decodeDecimal128IEEE754(Buffer buffer, int offset) throws Exception {
        // TODO - support Decimal128
        throw new RuntimeException("Decimal128 not supported");
    }

    public BigDecimal readDecimal32IEEE754(DataInput in) throws Exception {
        Float fl = in.readFloat();
        return new BigDecimal(fl, MathContext.DECIMAL64).stripTrailingZeros();
    }

    public void writeDecimal32IEEE754(BigDecimal value, DataOutput out) throws Exception {
        out.writeByte(AMQPDecimal32.DECIMAL32_IEEE_754_CODE);
        BigDecimal withContext = new BigDecimal(value.toPlainString(), MathContext.DECIMAL32);
        out.writeInt(Float.floatToIntBits(withContext.floatValue()));
    }

    public void encodeDecimal32IEEE754(BigDecimal value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal decodeDecimal32IEEE754(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal readDecimal64IEEE754(DataInput in) throws Exception {
        Double dbl = in.readDouble();
        return new BigDecimal(dbl, MathContext.DECIMAL64).stripTrailingZeros();
    }

    public void writeDecimal64IEEE754(BigDecimal value, DataOutput out) throws Exception {
        out.writeByte(AMQPDecimal64.DECIMAL64_IEEE_754_CODE);
        BigDecimal withContext = new BigDecimal(value.toPlainString(), MathContext.DECIMAL64);
        out.writeLong(Double.doubleToLongBits(withContext.doubleValue()));
    }

    public void encodeDecimal64IEEE754(BigDecimal value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal decodeDecimal64IEEE754(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Double readDoubleIEEE754(DataInput in) throws Exception {
        return in.readDouble();
    }

    public void writeDoubleIEEE754(Double value, DataOutput out) throws Exception {
        out.writeByte(AMQPDouble.DOUBLE_IEEE_754_CODE);
        out.writeDouble(value);
    }

    public void encodeDoubleIEEE754(Double value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Double decodeDoubleIEEE754(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Float readFloatIEEE754(DataInput in) throws Exception {
        return in.readFloat();
    }

    public void writeFloatIEEE754(Float value, DataOutput out) throws Exception {
        out.writeByte(AMQPFloat.FLOAT_IEEE_754_CODE);
        out.writeFloat(value);
    }

    public void encodeFloatIEEE754(Float value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Float decodeFloatIEEE754(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer readInt(DataInput in) throws Exception {
        return in.readInt();
    }

    public void writeInt(Integer value, DataOutput out) throws Exception {
        out.writeByte(AMQPInt.INT_CODE);
        out.writeInt(value);
    }

    public void encodeInt(Integer value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer decodeInt(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer readIntSmallInt(DataInput in) throws Exception {
        return (int)in.readByte();
    }

    public void writeIntSmallInt(Integer value, DataOutput out) throws Exception {
        out.writeByte(AMQPInt.INT_SMALLINT_CODE);
        out.writeByte(value.byteValue());
    }

    public void encodeIntSmallInt(Integer value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer decodeIntSmallInt(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List readList8(DataInput in) throws Exception {
        Long size = (long)in.readUnsignedByte();
        Long count = (long)in.readUnsignedByte();
        return readListData(in, size, count, AMQPList.LIST_LIST8_WIDTH);
    }

    public void writeList8(List value, DataOutput out) throws Exception {
        out.writeByte(AMQPList.LIST_LIST8_CODE);
        Long size = TypeRegistry.instance().sizer().sizeOfList(value) - 1 - AMQPList.LIST_LIST8_WIDTH;
        Long count = (long)value.size();
        out.writeByte(size.byteValue());
        out.writeByte(count.byteValue());
        writeListData(value, out);
    }

    public void encodeList8(List value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public List decodeList8(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List readList32(DataInput in) throws Exception {
        Long size = (long)in.readInt();
        Long count = (long)in.readInt();
        return readListData(in, size, count, AMQPList.LIST_LIST32_WIDTH);
    }

    private List readListData(DataInput in, Long size, Long count, int width) throws Exception {
        List rc = new ArrayList();
        while (count > 0) {
            rc.add(TypeReader.read(in));
            count--;
        }
        Long actualSize = TypeRegistry.instance().sizer().sizeOfList(rc) - 1 - width;
        if (size.longValue() != actualSize.longValue()) {
            throw new RuntimeException(String.format("Encoded size of list (%s) doesn't match actual size of list (%s)", size, actualSize));
        }
        return rc;
    }

    public void writeList32(List value, DataOutput out) throws Exception {
        out.writeByte(AMQPList.LIST_LIST32_CODE);
        Long size = TypeRegistry.instance().sizer().sizeOfList(value) - 1 - AMQPList.LIST_LIST32_WIDTH;
        Long count = (long)value.size();
        out.writeInt(size.intValue());
        out.writeInt(count.intValue());
        writeListData(value, out);
    }

    private void writeListData(List value, DataOutput out) throws Exception {
        for (Object obj : value) {
            AmqpType element = (AmqpType)obj;
            if (element == null) {
                writeNull(out);
            } else {
                element.write(out);
            }
        }
    }

    public void encodeList32(List value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public List decodeList32(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readLong(DataInput in) throws Exception {
        return in.readLong();
    }

    public void writeLong(Long value, DataOutput out) throws Exception {
        out.writeByte(AMQPLong.LONG_CODE);
        out.writeLong(value);
    }

    public void encodeLong(Long value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeLong(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readLongSmallLong(DataInput in) throws Exception {
        return (long)in.readByte();
    }

    public void writeLongSmallLong(Long value, DataOutput out) throws Exception {
        out.writeByte(AMQPLong.LONG_SMALLLONG_CODE);
        out.writeByte(value.byteValue());
    }

    public void encodeLongSmallLong(Long value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeLongSmallLong(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map readMap8(DataInput in) throws Exception {
        Long size = (long)in.readUnsignedByte();
        Long count = (long)in.readUnsignedByte();
        return readMapData(in, size, count, AMQPMap.MAP_MAP8_WIDTH);
    }

    private Map readMapData(DataInput in, Long size, Long count, int width) throws Exception {
        if (count % 2 != 0) {
            throw new RuntimeException(String.format("Map count (%s) is not divisible by 2", count));
        }
        Map rc = new HashMap();
        while (count > 0) {
            rc.put(TypeReader.read(in), TypeReader.read(in));
            count -= 2;
        }
        Long actualSize = TypeRegistry.instance().sizer().sizeOfMap(rc) - 1 - width;
        if (size.longValue() != actualSize.longValue()) {
            throw new RuntimeException(String.format("Encoded size of map (%s) does not match actual size of map (%s)", size, actualSize));
        }
        return rc;
    }

    public void writeMap8(Map value, DataOutput out) throws Exception {
        out.writeByte(AMQPMap.MAP_MAP8_CODE);
        Long size = TypeRegistry.instance().sizer().sizeOfMap(value) - 1 - AMQPMap.MAP_MAP8_WIDTH;
        Long count = (long)(value.keySet().size() + value.values().size());
        out.writeByte(size.byteValue());
        out.writeByte(count.byteValue());
        writeMapData(value, out);
    }

    private void writeMapData(Map value, DataOutput out) throws Exception {
        for (Object key : value.keySet()) {
            ((AmqpType)key).write(out);
            Object v = value.get(key);
            if (v == null) {
                writeNull(out);
            } else {
                ((AmqpType)v).write(out);
            }
        }
    }

    public void encodeMap8(Map value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map decodeMap8(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map readMap32(DataInput in) throws Exception {
        Long size = (long)in.readInt();
        Long count = (long)in.readInt();
        return readMapData(in, size, count, AMQPMap.MAP_MAP32_WIDTH);
    }

    public void writeMap32(Map value, DataOutput out) throws Exception {
        out.writeByte(AMQPMap.MAP_MAP32_CODE);
        Long size = TypeRegistry.instance().sizer().sizeOfMap(value) - 1 - AMQPMap.MAP_MAP32_WIDTH;
        Long count = (long)(value.keySet().size() + value.values().size());
        out.writeInt(size.intValue());
        out.writeInt(count.intValue());
        writeMapData(value, out);
    }

    public void encodeMap32(Map value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map decodeMap32(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object readNull(DataInput in) throws Exception {
        return null;
    }

    public void writeNull(DataOutput out) throws Exception {
        out.writeByte(TypeRegistry.NULL_FORMAT_CODE);
    }

    public void encodeNull(Buffer buffer, int offset) throws Exception {

    }

    public Object decodeNull(Buffer buffer, int offset) throws Exception {
        return null;
    }

    public Short readShort(DataInput in) throws Exception {
        return in.readShort();
    }

    public void writeShort(Short value, DataOutput out) throws Exception {
        out.writeByte(AMQPShort.SHORT_CODE);
        out.writeShort(value);
    }

    public void encodeShort(Short value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Short decodeShort(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String readStringStr8UTF8(DataInput in) throws Exception {
        int size = in.readUnsignedByte();
        Buffer s = new Buffer(size);
        s.readFrom(in);
        return new String(s.getData(), Charset.forName("UTF-8"));
    }

    public void writeStringStr8UTF8(String value, DataOutput out) throws Exception {
        Buffer s = new Buffer(value.getBytes("UTF-8"));
        out.writeByte(AMQPString.STRING_STR8_UTF8_CODE);
        out.writeByte(s.length());
        s.writeTo(out);
    }

    public void encodeStringStr8UTF8(String value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String decodeStringStr8UTF8(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String readStringStr32UTF8(DataInput in) throws Exception {
        int size = in.readInt();
        Buffer s = new Buffer(size);
        s.readFrom(in);
        return new String(s.getData(), Charset.forName("UTF-8"));
    }

    public void writeStringStr32UTF8(String value, DataOutput out) throws Exception {
        Buffer s = new Buffer(value.getBytes("UTF-8"));
        out.writeByte(AMQPString.STRING_STR32_UTF8_CODE);
        out.writeInt(s.length());
        s.writeTo(out);
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
        long val = in.readLong();
        return new Date(val);
    }

    public void writeTimestampMS64(Date value, DataOutput out) throws Exception {
        out.writeByte(AMQPTimestamp.TIMESTAMP_MS64_CODE);
        out.writeLong(value.getTime());
    }

    public void encodeTimestampMS64(Date value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Date decodeTimestampMS64(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Byte readUByte(DataInput in) throws Exception {
        return in.readByte();
    }

    public void writeUByte(Byte value, DataOutput out) throws Exception {
        out.writeByte(AMQPUByte.UBYTE_CODE);
        out.writeByte(value);
    }

    public void encodeUByte(Byte value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Byte decodeUByte(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer readUInt(DataInput in) throws Exception {
        return in.readInt();
    }

    public void writeUInt(Integer value, DataOutput out) throws Exception {
        out.writeByte(AMQPUInt.UINT_CODE);
        out.writeInt(value);
    }

    public void encodeUInt(Integer value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer decodeUInt(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer readUIntSmallUInt(DataInput in) throws Exception {
        return (int)in.readUnsignedByte();
    }

    public void writeUIntSmallUInt(Integer value, DataOutput out) throws Exception {
        out.writeByte(AMQPUInt.UINT_SMALLUINT_CODE);
        out.writeByte(value);
    }

    public void encodeUIntSmallUInt(Integer value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer decodeUIntSmallUInt(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer readUIntUInt0(DataInput in) throws Exception {
        return 0;
    }

    public void writeUIntUInt0(Integer value, DataOutput out) throws Exception {
        out.writeByte(AMQPUInt.UINT_UINT0_CODE);
    }

    public void encodeUIntUInt0(Integer value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer decodeUIntUInt0(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readULong(DataInput in) throws Exception {
        return in.readLong();
    }

    public void writeULong(Long value, DataOutput out) throws Exception {
        out.writeByte(AMQPULong.ULONG_CODE);
        out.writeLong(value);
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void encodeULong(Long value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeULong(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readULongSmallULong(DataInput in) throws Exception {
        return (long)in.readUnsignedByte();
    }

    public void writeULongSmallULong(Long value, DataOutput out) throws Exception {
        out.writeByte(AMQPULong.ULONG_SMALLULONG_CODE);
        out.writeByte(value.byteValue());
    }

    public void encodeULongSmallULong(Long value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeULongSmallULong(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readULongULong0(DataInput in) throws Exception {
        return 0L;
    }

    public void writeULongULong0(Long value, DataOutput out) throws Exception {
        out.writeByte(AMQPULong.ULONG_ULONG0_CODE);
    }

    public void encodeULongULong0(Long value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeULongULong0(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Short readUShort(DataInput in) throws Exception {
        return in.readShort();
    }

    public void writeUShort(Short value, DataOutput out) throws Exception {
        out.writeByte(AMQPUShort.USHORT_CODE);
        out.writeShort(value);
    }

    public void encodeUShort(Short value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Short decodeUShort(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public UUID readUUID(DataInput in) throws Exception {
        return new UUID(in.readLong(), in.readLong());
    }

    public void writeUUID(UUID value, DataOutput out) throws Exception {
        out.writeByte(AMQPUUID.UUID_CODE);
        out.writeLong(value.getMostSignificantBits());
        out.writeLong(value.getLeastSignificantBits());
    }

    public void encodeUUID(UUID value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public UUID decodeUUID(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
