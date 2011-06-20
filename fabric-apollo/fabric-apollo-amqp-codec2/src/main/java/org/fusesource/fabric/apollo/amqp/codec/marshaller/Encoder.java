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

import org.fusesource.fabric.apollo.amqp.codec.BitUtils;
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AmqpType;
import org.fusesource.fabric.apollo.amqp.codec.interfaces.PrimitiveEncoder;
import org.fusesource.fabric.apollo.amqp.codec.types.*;
import org.fusesource.hawtbuf.Buffer;

import java.io.DataInput;
import java.io.DataOutput;
import java.math.BigDecimal;
import java.math.BigInteger;
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
        out.writeLong(value.getTime());
    }

    public void encodeTimestampMS64(Date value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Date decodeTimestampMS64(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Short readUByte(DataInput in) throws Exception {
        return (short)in.readUnsignedByte();
    }

    public void writeUByte(Short value, DataOutput out) throws Exception {
        out.writeByte(value);
    }

    public void encodeUByte(Short value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Short decodeUByte(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readUInt(DataInput in) throws Exception {
        long rc = 0;
        rc = rc | (0xFFFFFFFFL & (((long) in.readByte()) << 24));
        rc = rc | (0xFFFFFFFFL & (((long) in.readByte()) << 16));
        rc = rc | (0xFFFFFFFFL & (((long) in.readByte()) << 8));
        rc = rc | (0xFFFFFFFFL & (long) in.readByte());
        return rc;
    }

    public void writeUInt(Long value, DataOutput out) throws Exception {
        out.writeInt(value.intValue());
    }

    public void encodeUInt(Long value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeUInt(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readUIntSmallUInt(DataInput in) throws Exception {
        return (long)in.readUnsignedByte();
    }

    public void writeUIntSmallUInt(Long value, DataOutput out) throws Exception {
        out.writeByte((short)value.intValue());
    }

    public void encodeUIntSmallUInt(Long value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeUIntSmallUInt(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long readUIntUInt0(DataInput in) throws Exception {
        return (long)0;
    }

    public void writeUIntUInt0(Long value, DataOutput out) throws Exception {

    }

    public void encodeUIntUInt0(Long value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Long decodeUIntUInt0(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigInteger readULong(DataInput in) throws Exception {
        byte[] rc = new byte[8];
        in.readFully(rc);
        return new BigInteger(1, rc);
    }

    public void writeULong(BigInteger value, DataOutput out) throws Exception {
        byte[] toWrite = new byte[8];
        Arrays.fill(toWrite, (byte)0x0);
        BitUtils.setULong(toWrite, 0, value.abs());
        out.write(toWrite);
    }

    public void encodeULong(BigInteger value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigInteger decodeULong(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigInteger readULongSmallULong(DataInput in) throws Exception {
        byte b[] = new byte[1];
        in.readFully(b);
        return new BigInteger(b);
    }

    public void writeULongSmallULong(BigInteger value, DataOutput out) throws Exception {
        out.writeByte(value.byteValue());
    }

    public void encodeULongSmallULong(BigInteger value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigInteger decodeULongSmallULong(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigInteger readULongULong0(DataInput in) throws Exception {
        return BigInteger.ZERO;
    }

    public void writeULongULong0(BigInteger value, DataOutput out) throws Exception {
    }

    public void encodeULongULong0(BigInteger value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigInteger decodeULongULong0(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer readUShort(DataInput in) throws Exception {
        int rc = 0;
        rc = rc | (0xFFFF & (((int) in.readByte()) << 8));
        rc = rc | (0xFFFF & (int) in.readByte());
        return rc;
    }

    public void writeUShort(Integer value, DataOutput out) throws Exception {
        out.writeShort(value.shortValue());
    }

    public void encodeUShort(Integer value, Buffer buffer, int offset) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Integer decodeUShort(Buffer buffer, int offset) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public UUID readUUID(DataInput in) throws Exception {
        return new UUID(in.readLong(), in.readLong());
    }

    public void writeUUID(UUID value, DataOutput out) throws Exception {
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
