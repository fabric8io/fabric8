/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.codec.marshaller;

import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPType;
import org.fusesource.fabric.apollo.amqp.codec.interfaces.PrimitiveEncoder;
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPArray;
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPList;
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPMap;
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPULong;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;

import java.io.DataInput;
import java.io.DataOutput;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.*;

import static org.fusesource.fabric.apollo.amqp.codec.marshaller.ArraySupport.*;

/**
 *
 */
public class Encoder implements PrimitiveEncoder {

    protected static final Encoder SINGLETON = new Encoder();

    public static Encoder instance() {
        return SINGLETON;
    }

    public Object[] readArray8(DataInput in) throws Exception {
        int size = in.readUnsignedByte();
        int count = in.readUnsignedByte();
        byte formatCode = (byte) in.readUnsignedByte();
        return readArray(in, count, formatCode);
    }

    private Object[] readArray(DataInput in, long count, byte formatCode) throws Exception {
        Object[] rc;
        if ( formatCode == TypeRegistry.DESCRIBED_FORMAT_CODE ) {
            BigInteger descriptor = AMQPULong.read(in);
            Class clazz = TypeRegistry.instance().getFormatCodeMap().get(descriptor);
            rc = (Object[]) Array.newInstance(clazz, (int) count);
            for ( int i = 0; i < rc.length; i++ ) {
                rc[i] = clazz.newInstance();
                ((AMQPType) rc[i]).read((byte) 0x0, in);
            }
        } else {
            Class clazz = TypeRegistry.instance().getPrimitiveFormatCodeMap().get(formatCode);
            rc = (Object[]) Array.newInstance(clazz, (int) count);
            for ( int i = 0; i < rc.length; i++ ) {
                rc[i] = clazz.newInstance();
                ((AMQPType) rc[i]).read(formatCode, in);
            }
        }
        return rc;
    }

    public void writeArray8(Object[] value, DataOutput out) throws Exception {
        Long size = AMQPArray.ARRAY_ARRAY8_WIDTH + getArrayConstructorSize(value) + getArrayBodySize(value);
        Long count = (long) value.length;
        writeUByte(size.shortValue(), out);
        writeUByte(count.shortValue(), out);
        writeArrayBody(value, out);
    }

    private void writeArrayBody(Object[] value, DataOutput out) throws Exception {
        writeArrayConstructor(value, out);
        for ( Object obj : value ) {
            Object constructor = getArrayConstructor(value);
            if ( constructor instanceof Byte ) {
                ((AMQPType) obj).writeBody((Byte) constructor, out);
            } else {
                ((AMQPType) obj).writeBody((byte) 0x0, out);
            }
        }
    }

    public Object[] readArray32(DataInput in) throws Exception {
        long size = readUInt(in);
        long count = readUInt(in);
        byte formatCode = (byte) in.readUnsignedByte();
        return readArray(in, count, formatCode);
    }

    public void writeArray32(Object[] value, DataOutput out) throws Exception {
        long size = AMQPArray.ARRAY_ARRAY32_WIDTH + getArrayConstructorSize(value) + getArrayBodySize(value);
        long count = value.length;
        writeUInt(size, out);
        writeUInt(count, out);
        writeArrayBody(value, out);
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

    public Boolean readBoolean(DataInput in) throws Exception {
        byte val = in.readByte();
        if ( val == 0 ) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    public void writeBoolean(Boolean value, DataOutput out) throws Exception {
        if ( value ) {
            out.writeByte(1);
        } else {
            out.writeByte(0);
        }
    }

    public Boolean readBooleanTrue(DataInput in) throws Exception {
        return Boolean.TRUE;
    }

    public void writeBooleanTrue(Boolean value, DataOutput out) throws Exception {
    }

    public Boolean readBooleanFalse(DataInput in) throws Exception {
        return Boolean.FALSE;
    }

    public void writeBooleanFalse(Boolean value, DataOutput out) throws Exception {
    }

    public Byte readByte(DataInput in) throws Exception {
        return in.readByte();
    }

    public void writeByte(Byte value, DataOutput out) throws Exception {
        out.writeByte(value);
    }

    public Character readCharUTF32(DataInput in) throws Exception {
        return in.readChar();
    }

    public void writeCharUTF32(Character value, DataOutput out) throws Exception {
        out.writeChar(value);
    }

    public BigDecimal readDecimal128IEEE754(DataInput in) throws Exception {
        // TODO - support Decimal128
        throw new RuntimeException("Decimal128 not supported");
    }

    public void writeDecimal128IEEE754(BigDecimal value, DataOutput out) throws Exception {
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

    public BigDecimal readDecimal64IEEE754(DataInput in) throws Exception {
        Double dbl = in.readDouble();
        return new BigDecimal(dbl, MathContext.DECIMAL64).stripTrailingZeros();
    }

    public void writeDecimal64IEEE754(BigDecimal value, DataOutput out) throws Exception {
        BigDecimal withContext = new BigDecimal(value.toPlainString(), MathContext.DECIMAL64);
        out.writeLong(Double.doubleToLongBits(withContext.doubleValue()));
    }

    public Double readDoubleIEEE754(DataInput in) throws Exception {
        return in.readDouble();
    }

    public void writeDoubleIEEE754(Double value, DataOutput out) throws Exception {
        out.writeDouble(value);
    }

    public Float readFloatIEEE754(DataInput in) throws Exception {
        return in.readFloat();
    }

    public void writeFloatIEEE754(Float value, DataOutput out) throws Exception {
        out.writeFloat(value);
    }

    public Integer readInt(DataInput in) throws Exception {
        return in.readInt();
    }

    public void writeInt(Integer value, DataOutput out) throws Exception {
        out.writeInt(value);
    }

    public Integer readIntSmallInt(DataInput in) throws Exception {
        return (int) in.readByte();
    }

    public void writeIntSmallInt(Integer value, DataOutput out) throws Exception {
        out.writeByte(value.byteValue());
    }

    public List readList0(DataInput in) throws Exception {
        return new ArrayList();
    }

    public void writeList0(List value, DataOutput out) throws Exception {
        // Nothing to do
    }

    public List readList8(DataInput in) throws Exception {
        Long size = (long) in.readUnsignedByte();
        Long count = (long) in.readUnsignedByte();
        return readListData(in, size, count, AMQPList.LIST_LIST8_WIDTH);
    }

    public void writeList8(List value, DataOutput out) throws Exception {
        Long size = TypeRegistry.instance().sizer().sizeOfList(value) - 1 - AMQPList.LIST_LIST8_WIDTH;
        Long count = (long) value.size();
        writeUByte(size.shortValue(), out);
        writeUByte(count.shortValue(), out);
        writeListData(value, out);
    }

    public List readList32(DataInput in) throws Exception {
        Long size = readUInt(in);
        Long count = readUInt(in);
        return readListData(in, size, count, AMQPList.LIST_LIST32_WIDTH);
    }

    private List readListData(DataInput in, Long size, Long count, int width) throws Exception {
        List rc = new ArrayList();
        while (count > 0) {
            rc.add(TypeReader.read(in));
            count--;
        }
        Long actualSize = TypeRegistry.instance().sizer().sizeOfList(rc) - 1 - width;
        if ( size.longValue() != actualSize.longValue() ) {
            throw new RuntimeException(String.format("Encoded size of list (%s) doesn't match actual size of list (%s)", size, actualSize));
        }
        return rc;
    }

    public void writeList32(List value, DataOutput out) throws Exception {
        Long size = TypeRegistry.instance().sizer().sizeOfList(value) - 1 - AMQPList.LIST_LIST32_WIDTH;
        Long count = (long) value.size();
        writeUInt(size, out);
        writeUInt(count, out);
        writeListData(value, out);
    }

    private void writeListData(List value, DataOutput out) throws Exception {
        for ( Object obj : value ) {
            AMQPType element = (AMQPType) obj;
            if ( element == null ) {
                writeNull(out);
            } else {
                element.write(out);
            }
        }
    }

    public Long readLong(DataInput in) throws Exception {
        return in.readLong();
    }

    public void writeLong(Long value, DataOutput out) throws Exception {
        out.writeLong(value);
    }

    public Long readLongSmallLong(DataInput in) throws Exception {
        return (long) in.readByte();
    }

    public void writeLongSmallLong(Long value, DataOutput out) throws Exception {
        out.writeByte(value.byteValue());
    }

    public Map readMap8(DataInput in) throws Exception {
        Long size = (long) in.readUnsignedByte();
        Long count = (long) in.readUnsignedByte();
        return readMapData(in, size, count, AMQPMap.MAP_MAP8_WIDTH);
    }

    private Map readMapData(DataInput in, Long size, Long count, int width) throws Exception {
        if ( count % 2 != 0 ) {
            throw new RuntimeException(String.format("Map count (%s) is not divisible by 2", count));
        }
        Map rc = new HashMap();
        while (count > 0) {
            rc.put(TypeReader.read(in), TypeReader.read(in));
            count -= 2;
        }
        Long actualSize = TypeRegistry.instance().sizer().sizeOfMap(rc) - 1 - width;
        if ( size.longValue() != actualSize.longValue() ) {
            throw new RuntimeException(String.format("Encoded size of map (%s) does not match actual size of map (%s)", size, actualSize));
        }
        return rc;
    }

    public void writeMap8(Map value, DataOutput out) throws Exception {
        Long size = TypeRegistry.instance().sizer().sizeOfMap(value) - 1 - AMQPMap.MAP_MAP8_WIDTH;
        Long count = (long) (value.keySet().size() + value.values().size());
        writeUByte(size.shortValue(), out);
        writeUByte(count.shortValue(), out);
        writeMapData(value, out);
    }

    private void writeMapData(Map value, DataOutput out) throws Exception {
        for ( Object key : value.keySet() ) {
            ((AMQPType) key).write(out);
            Object v = value.get(key);
            if ( v == null ) {
                writeNull(out);
            } else {
                ((AMQPType) v).write(out);
            }
        }
    }

    public Map readMap32(DataInput in) throws Exception {
        Long size = readUInt(in);
        Long count = readUInt(in);
        return readMapData(in, size, count, AMQPMap.MAP_MAP32_WIDTH);
    }

    public void writeMap32(Map value, DataOutput out) throws Exception {
        Long size = TypeRegistry.instance().sizer().sizeOfMap(value) - 1 - AMQPMap.MAP_MAP32_WIDTH;
        Long count = (long) (value.keySet().size() + value.values().size());
        writeUInt(size, out);
        writeUInt(count, out);
        writeMapData(value, out);
    }

    public Object readNull(DataInput in) throws Exception {
        return null;
    }

    public void writeNull(DataOutput out) throws Exception {
        out.writeByte(TypeRegistry.NULL_FORMAT_CODE);
    }

    public Short readShort(DataInput in) throws Exception {
        return in.readShort();
    }

    public void writeShort(Short value, DataOutput out) throws Exception {
        out.writeShort(value);
    }

    public String readStringStr8UTF8(DataInput in) throws Exception {
        int size = in.readUnsignedByte();
        Buffer s = new Buffer(size);
        s.readFrom(in);
        return s.utf8().toString();
    }

    public void writeStringStr8UTF8(String value, DataOutput out) throws Exception {
        UTF8Buffer buf = new UTF8Buffer(value);
        out.writeByte(buf.buffer().length());
        buf.buffer().writeTo(out);
    }

    public String readStringStr32UTF8(DataInput in) throws Exception {
        int size = in.readInt();
        Buffer s = new Buffer(size);
        s.readFrom(in);
        return s.utf8().toString();
    }

    public void writeStringStr32UTF8(String value, DataOutput out) throws Exception {
        UTF8Buffer buf = new UTF8Buffer(value);
        out.writeInt(buf.buffer().length());
        buf.buffer().writeTo(out);
    }

    public Buffer readSymbolSym8(DataInput in) throws Exception {
        int size = in.readUnsignedByte();
        Buffer rc = new Buffer(size);
        rc.readFrom(in);
        return rc;
    }

    public void writeSymbolSym8(Buffer value, DataOutput out) throws Exception {
        out.writeByte(value.length());
        value.writeTo(out);
    }

    public Buffer readSymbolSym32(DataInput in) throws Exception {
        int size = in.readInt();
        Buffer rc = new Buffer(size);
        rc.readFrom(in);
        return rc;
    }

    public void writeSymbolSym32(Buffer value, DataOutput out) throws Exception {
        out.writeInt(value.length());
        value.writeTo(out);
    }

    public Date readTimestampMS64(DataInput in) throws Exception {
        long val = in.readLong();
        return new Date(val);
    }

    public void writeTimestampMS64(Date value, DataOutput out) throws Exception {
        out.writeLong(value.getTime());
    }

    public Short readUByte(DataInput in) throws Exception {
        return (short) in.readUnsignedByte();
    }

    public void writeUByte(Short value, DataOutput out) throws Exception {
        out.writeByte(value);
    }

    public Long readUInt(DataInput in) throws Exception {
        return (long)
                ((in.readUnsignedByte() << 24 |
                        in.readUnsignedByte() << 16 |
                        in.readUnsignedByte() << 8 |
                        in.readUnsignedByte() << 0) & 0xFFFFFFFF);
    }

    public void writeUInt(Long value, DataOutput out) throws Exception {
        out.writeByte((byte) (value >> 24 & 0xFF));
        out.writeByte((byte) (value >> 16 & 0xFF));
        out.writeByte((byte) (value >> 8 & 0xFF));
        out.writeByte((byte) (value >> 0 & 0xFF));
    }

    public Long readUIntSmallUInt(DataInput in) throws Exception {
        return (long) in.readUnsignedByte();
    }

    public void writeUIntSmallUInt(Long value, DataOutput out) throws Exception {
        out.writeByte((short) value.intValue());
    }

    public Long readUIntUInt0(DataInput in) throws Exception {
        return (long) 0;
    }

    public void writeUIntUInt0(Long value, DataOutput out) throws Exception {

    }

    public BigInteger readULong(DataInput in) throws Exception {
        byte[] rc = new byte[8];
        in.readFully(rc);
        return new BigInteger(1, rc);
    }

    public void writeULong(BigInteger value, DataOutput out) throws Exception {
        byte[] toWrite = new byte[8];
        Arrays.fill(toWrite, (byte) 0x0);
        BitUtils.setULong(toWrite, 0, value.abs());
        out.write(toWrite);
    }

    public BigInteger readULongSmallULong(DataInput in) throws Exception {
        byte b[] = new byte[1];
        in.readFully(b);
        return new BigInteger(b);
    }

    public void writeULongSmallULong(BigInteger value, DataOutput out) throws Exception {
        out.writeByte(value.byteValue());
    }

    public BigInteger readULongULong0(DataInput in) throws Exception {
        return BigInteger.ZERO;
    }

    public void writeULongULong0(BigInteger value, DataOutput out) throws Exception {
    }

    public Integer readUShort(DataInput in) throws Exception {
        return (in.readUnsignedByte() << 8 |
                in.readUnsignedByte() << 0);
    }

    public void writeUShort(Integer value, DataOutput out) throws Exception {
        out.writeByte((byte) ((value >> 8) & 0xFF));
        out.writeByte((byte) ((value >> 0) & 0xFF));
    }

    public UUID readUUID(DataInput in) throws Exception {
        return new UUID(in.readLong(), in.readLong());
    }

    public void writeUUID(UUID value, DataOutput out) throws Exception {
        out.writeLong(value.getMostSignificantBits());
        out.writeLong(value.getLeastSignificantBits());
    }

}
