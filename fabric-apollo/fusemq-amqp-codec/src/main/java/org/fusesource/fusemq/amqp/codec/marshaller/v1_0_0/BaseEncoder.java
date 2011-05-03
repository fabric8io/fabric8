/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fusemq.amqp.codec.marshaller.v1_0_0;

import org.fusesource.fusemq.amqp.codec.BitUtils;
import org.fusesource.fusemq.amqp.codec.marshaller.AmqpEncodingError;
import org.fusesource.fusemq.amqp.codec.marshaller.v1_0_0.AmqpStringMarshaller.STRING_ENCODING;
import org.fusesource.fusemq.amqp.codec.marshaller.v1_0_0.AmqpSymbolMarshaller.SYMBOL_ENCODING;
import org.fusesource.hawtbuf.Buffer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Date;
import java.util.UUID;

class BaseEncoder implements PrimitiveEncoder {
    public final Buffer decodeBinary(Buffer encoded, int offset, int length) throws AmqpEncodingError {
        return new Buffer(encoded.data, encoded.offset + offset, length);
    }

    public final Buffer decodeBinaryVbin32(Buffer encoded, int offset, int length) throws AmqpEncodingError {
        return decodeBinary(encoded, encoded.offset + offset, length);
    }

    public final Buffer decodeBinaryVbin8(Buffer encoded, int offset, int length) throws AmqpEncodingError {
        return decodeBinary(encoded, encoded.offset + offset, length);
    }

    public final Byte decodeByte(Buffer encoded, int offset) throws AmqpEncodingError {
        return encoded.get(encoded.offset + offset);
    }

    /**
     * Writes a BigInteger encoded as 64-bit unsigned integer in network byte order
     */
    public void writeUlongUlong(BigInteger val, DataOutput buf) throws IOException, AmqpEncodingError {
        writeUlong(val, buf);
    }

    /**
     * Encodes a BigInteger as 64-bit unsigned integer in network byte order
     * <p/>
     * The encoded data should be written into the supplied buffer at the given offset.
     */
    public void encodeUlongUlong(BigInteger val, Buffer buf, int offset) throws AmqpEncodingError {
        encodeUlong(val, buf, offset);
    }

    /**
     * Reads a BigInteger encoded as 64-bit unsigned integer in network byte order
     */
    public BigInteger readUlongUlong(DataInput dis) throws IOException, AmqpEncodingError {
        return readUlong(dis);
    }

    /**
     * Decodes a BigInteger encoded as 64-bit unsigned integer in network byte order
     */
    public BigInteger decodeUlongUlong(Buffer encoded, int offset) throws AmqpEncodingError {
        return decodeUlong(encoded, offset);
    }

    /**
     * Writes a BigInteger encoded as unsigned long value in the range 0-255
     */
    public void writeUlongSmallulong(BigInteger val, DataOutput buf) throws IOException, AmqpEncodingError {
        writeUbyte(val.shortValue(), buf);
    }

    /**
     * Encodes a BigInteger as unsigned long value in the range 0-255
     * <p/>
     * The encoded data should be written into the supplied buffer at the given offset.
     */
    public void encodeUlongSmallulong(BigInteger val, Buffer buf, int offset) throws AmqpEncodingError {
        encodeUbyte(val.shortValue(), buf, offset);
    }

    /**
     * Reads a BigInteger encoded as unsigned long value in the range 0-255
     */
    public BigInteger readUlongSmallulong(DataInput dis) throws IOException, AmqpEncodingError {
        short value = readUbyte(dis);
        return BigInteger.valueOf(value);
    }

    /**
     * Decodes a BigInteger encoded as unsigned long value in the range 0-255
     */
    public BigInteger decodeUlongSmallulong(Buffer encoded, int offset) throws AmqpEncodingError {
        short value = decodeUbyte(encoded, offset);
        return BigInteger.valueOf(value);
    }

    public final Integer decodeChar(Buffer encoded, int offset) throws AmqpEncodingError {
        return BitUtils.getInt(encoded.data, encoded.offset + offset);
    }

    /**
     * Writes a Long encoded as 64-bit two's-complement integer in network byte order
     */
    public void writeLongLong(Long val, DataOutput buf) throws IOException, AmqpEncodingError {
        writeLong(val, buf);
    }

    /**
     * Encodes a Long as 64-bit two's-complement integer in network byte order
     * <p/>
     * The encoded data should be written into the supplied buffer at the given offset.
     */
    public void encodeLongLong(Long val, Buffer buf, int offset) throws AmqpEncodingError {
        encodeLong(val, buf, offset);
    }

    /**
     * Reads a Long encoded as 64-bit two's-complement integer in network byte order
     */
    public Long readLongLong(DataInput dis) throws IOException, AmqpEncodingError {
        return readLong(dis);
    }

    /**
     * Decodes a Long encoded as 64-bit two's-complement integer in network byte order
     */
    public Long decodeLongLong(Buffer encoded, int offset) throws AmqpEncodingError {
        return decodeLong(encoded, offset);
    }

    /**
     * Writes a Long encoded as unsigned long value in the range -128-127
     */
    public void writeLongSmalllong(Long val, DataOutput buf) throws IOException, AmqpEncodingError {
        writeUbyte(val.shortValue(), buf);
    }

    /**
     * Encodes a Long as unsigned long value in the range -128-127
     * <p/>
     * The encoded data should be written into the supplied buffer at the given offset.
     */
    public void encodeLongSmalllong(Long val, Buffer buf, int offset) throws AmqpEncodingError {
        encodeUbyte(val.shortValue(), buf, offset);
    }

    /**
     * Reads a Long encoded as unsigned long value in the range -128-127
     */
    public Long readLongSmalllong(DataInput dis) throws IOException, AmqpEncodingError {
        short value = readUbyte(dis);
        return (long) value;
    }

    /**
     * Decodes a Long encoded as unsigned long value in the range -128-127
     */
    public Long decodeLongSmalllong(Buffer encoded, int offset) throws AmqpEncodingError {
        short value = decodeUbyte(encoded, offset);
        return (long) value;
    }

    public final Double decodeDouble(Buffer encoded, int offset) throws AmqpEncodingError {
        return Double.longBitsToDouble(decodeLong(encoded, encoded.offset + offset));
    }

    public final Float decodeFloat(Buffer encoded, int offset) throws AmqpEncodingError {
        return Float.intBitsToFloat(decodeInt(encoded, encoded.offset + offset));
    }

    public final BigDecimal decodeDecimal32(Buffer encoded, int offset) throws AmqpEncodingError {
        Float fl = decodeFloat(encoded, offset);
        return new BigDecimal(fl, MathContext.DECIMAL32).stripTrailingZeros();
    }

    public final BigDecimal decodeDecimal64(Buffer encoded, int offset) throws AmqpEncodingError {
        Double dbl = decodeDouble(encoded, offset);
        return new BigDecimal(dbl, MathContext.DECIMAL64).stripTrailingZeros();
    }

    // TODO - implement
    public BigDecimal decodeDecimal128(Buffer encoded, int offset) throws AmqpEncodingError {
        throw new UnsupportedOperationException("decimal128 not yet supported");
    }

    public final Integer decodeInt(Buffer encoded, int offset) throws AmqpEncodingError {
        return BitUtils.getInt(encoded.data, encoded.offset + offset);
    }

    public final Long decodeLong(Buffer encoded, int offset) throws AmqpEncodingError {
        return BitUtils.getLong(encoded.data, encoded.offset + offset);
    }

    public final Short decodeShort(Buffer encoded, int offset) throws AmqpEncodingError {
        return BitUtils.getShort(encoded.data, encoded.offset + offset);
    }

    public final String decodeString(Buffer encoded, int offset, int length, String charset) throws AmqpEncodingError {
        try {
            return new String(encoded.data, encoded.offset + offset, length, charset);
        } catch (UnsupportedEncodingException e) {
            throw new AmqpEncodingError(e.getMessage(), e);
        }
    }

    public final String decodeStringStr32Utf16(Buffer encoded, int offset, int length) throws AmqpEncodingError {
        return decodeString(encoded, encoded.offset + offset, length, "utf-16");
    }

    public final String decodeStringStr32Utf8(Buffer encoded, int offset, int length) throws AmqpEncodingError {
        return decodeString(encoded, encoded.offset + offset, length, "utf-8");
    }

    public final String decodeStringStr8Utf16(Buffer encoded, int offset, int length) throws AmqpEncodingError {
        return decodeString(encoded, encoded.offset + offset, length, "utf-16");
    }

    public final String decodeStringStr8Utf8(Buffer encoded, int offset, int length) throws AmqpEncodingError {
        return decodeString(encoded, encoded.offset + offset, length, "utf-8");
    }

    public final String decodeSymbolSym32(Buffer encoded, int offset, int length) throws AmqpEncodingError {
        return decodeString(encoded, encoded.offset + offset, length, "us-ascii");
    }

    /**
     * Writes a Integer encoded as 32-bit two's-complement integer in network byte order
     */
    public void writeIntInt(Integer val, DataOutput buf) throws IOException, AmqpEncodingError {
        writeInt(val, buf);
    }

    /**
     * Encodes a Integer as 32-bit two's-complement integer in network byte order
     * <p/>
     * The encoded data should be written into the supplied buffer at the given offset.
     */
    public void encodeIntInt(Integer val, Buffer buf, int offset) throws AmqpEncodingError {
        encodeInt(val, buf, offset);
    }

    /**
     * Reads a Integer encoded as 32-bit two's-complement integer in network byte order
     */
    public Integer readIntInt(DataInput dis) throws IOException, AmqpEncodingError {
        return readInt(dis);
    }

    /**
     * Decodes a Integer encoded as 32-bit two's-complement integer in network byte order
     */
    public Integer decodeIntInt(Buffer encoded, int offset) throws AmqpEncodingError {
        return decodeInt(encoded, offset);
    }

    /**
     * Writes a Integer encoded as unsigned integer value in the range -128-127
     */
    public void writeIntSmallint(Integer val, DataOutput buf) throws IOException, AmqpEncodingError {
        writeUbyte(val.shortValue(), buf);
    }

    /**
     * Encodes a Integer as unsigned integer value in the range -128-127
     * <p/>
     * The encoded data should be written into the supplied buffer at the given offset.
     */
    public void encodeIntSmallint(Integer val, Buffer buf, int offset) throws AmqpEncodingError {
        encodeUbyte(val.shortValue(), buf, offset);
    }

    /**
     * Reads a Integer encoded as unsigned integer value in the range -128-127
     */
    public Integer readIntSmallint(DataInput dis) throws IOException, AmqpEncodingError {
        return readUbyte(dis).intValue();
    }

    /**
     * Decodes a Integer encoded as unsigned integer value in the range -128-127
     */
    public Integer decodeIntSmallint(Buffer encoded, int offset) throws AmqpEncodingError {
        short value = decodeUbyte(encoded, offset);
        return (int) value;
    }

    public final String decodeSymbolSym8(Buffer encoded, int offset, int length) throws AmqpEncodingError {
        return decodeString(encoded, encoded.offset + offset, length, "us-ascii");
    }

    public final Date decodeTimestamp(Buffer encoded, int offset) throws AmqpEncodingError {
        return new Date(decodeLong(encoded, encoded.offset + offset));
    }

    public final Short decodeUbyte(Buffer encoded, int offset) throws AmqpEncodingError {
        int value = BitUtils.getUByte(encoded.data, encoded.offset + offset);
        return (short)value;
    }

    /**
     * Writes a Long encoded as 32-bit unsigned integer in network byte order
     */
    public void writeUintUint(Long val, DataOutput buf) throws IOException, AmqpEncodingError {
        writeUint(val, buf);
    }

    /**
     * Encodes a Long as 32-bit unsigned integer in network byte order
     * <p/>
     * The encoded data should be written into the supplied buffer at the given offset.
     */
    public void encodeUintUint(Long val, Buffer buf, int offset) throws AmqpEncodingError {
        encodeUint(val, buf, offset);
    }

    /**
     * Reads a Long encoded as 32-bit unsigned integer in network byte order
     */
    public Long readUintUint(DataInput dis) throws IOException, AmqpEncodingError {
        return readUint(dis);
    }

    /**
     * Decodes a Long encoded as 32-bit unsigned integer in network byte order
     */
    public Long decodeUintUint(Buffer encoded, int offset) throws AmqpEncodingError {
        return decodeUint(encoded, offset);
    }

    /**
     * Writes a Long encoded as unsigned integer value in the range 0-255
     */
    public void writeUintSmalluint(Long val, DataOutput buf) throws IOException, AmqpEncodingError {
        writeUbyte(val.shortValue(), buf);
    }

    /**
     * Encodes a Long as unsigned integer value in the range 0-255
     * <p/>
     * The encoded data should be written into the supplied buffer at the given offset.
     */
    public void encodeUintSmalluint(Long val, Buffer buf, int offset) throws AmqpEncodingError {
        encodeUbyte(val.shortValue(), buf, offset);
    }

    /**
     * Reads a Long encoded as unsigned integer value in the range 0-255
     */
    public Long readUintSmalluint(DataInput dis) throws IOException, AmqpEncodingError {
        return readUbyte(dis).longValue();
    }

    /**
     * Decodes a Long encoded as unsigned integer value in the range 0-255
     */
    public Long decodeUintSmalluint(Buffer encoded, int offset) throws AmqpEncodingError {
        return decodeUbyte(encoded, offset).longValue();
    }

    public Long decodeUint(Buffer encoded, int offset) throws AmqpEncodingError {
        return BitUtils.getUInt(encoded.data, encoded.offset + offset);
    }

    public BigInteger decodeUlong(Buffer encoded, int offset) throws AmqpEncodingError {
        return BitUtils.getULong(encoded.data, encoded.offset + offset);
    }

    public Integer decodeUshort(Buffer encoded, int offset) throws AmqpEncodingError {
        return BitUtils.getUShort(encoded.data, encoded.offset + offset);
    }

    public UUID decodeUuid(Buffer encoded, int offset) throws AmqpEncodingError {
        return new UUID(decodeLong(encoded, offset), decodeLong(encoded, offset + 8));
    }

    public void encodeBinaryVbin32(Buffer val, Buffer buf, int offset) throws AmqpEncodingError {
        System.arraycopy(val.data, val.offset, buf.data, buf.offset + offset, val.length);

    }

    public void encodeBinaryVbin8(Buffer val, Buffer buf, int offset) throws AmqpEncodingError {
        System.arraycopy(val.data, val.offset, buf.data, buf.offset + offset, val.length);
    }

    public void encodeByte(Byte val, Buffer buf, int offset) throws AmqpEncodingError {
        buf.data[buf.offset + offset] = val;

    }

    public void encodeChar(Integer val, Buffer buf, int offset) throws AmqpEncodingError {
        BitUtils.setUInt(buf.data, buf.offset + offset, val);
    }

    public void encodeDouble(Double val, Buffer buf, int offset) throws AmqpEncodingError {
        BitUtils.setLong(buf.data, buf.offset + offset, Double.doubleToLongBits(val));
    }

    public void encodeFloat(Float val, Buffer buf, int offset) throws AmqpEncodingError {
        BitUtils.setInt(buf.data, buf.offset + offset, Float.floatToIntBits(val));
    }

    public void encodeDecimal32(BigDecimal val, Buffer buf, int offset) throws AmqpEncodingError {
        BigDecimal withContext = new BigDecimal(val.toPlainString(), MathContext.DECIMAL32);
        encodeFloat(withContext.floatValue(), buf, offset);
    }

    public void encodeDecimal64(BigDecimal val, Buffer buf, int offset) throws AmqpEncodingError {
        BigDecimal withContext = new BigDecimal(val.toPlainString(), MathContext.DECIMAL64);
        encodeDouble(withContext.doubleValue(), buf, offset);
    }

    public void encodeDecimal128(BigDecimal val, Buffer buf, int offset) throws AmqpEncodingError {
        // TODO
        throw new UnsupportedOperationException("decimal128 not yet supported");
    }

    public void encodeInt(Integer val, Buffer buf, int offset) throws AmqpEncodingError {
        BitUtils.setInt(buf.data, buf.offset + offset, val);
    }

    public void encodeLong(Long val, Buffer buf, int offset) throws AmqpEncodingError {
        BitUtils.setLong(buf.data, buf.offset + offset, val);
    }

    public void encodeShort(Short val, Buffer buf, int offset) throws AmqpEncodingError {
        BitUtils.setShort(buf.data, buf.offset + offset, val);
    }

    public void encodeStringStr32Utf16(String val, Buffer buf, int offset) throws AmqpEncodingError {
        byte[] s;
        try {
            s = val.getBytes("utf-16");
        } catch (UnsupportedEncodingException e) {
            throw new AmqpEncodingError(e.getMessage(), e);
        }
        System.arraycopy(s, 0, buf.data, buf.offset + offset, s.length);

    }

    public void encodeStringStr32Utf8(String val, Buffer buf, int offset) throws AmqpEncodingError {
        byte[] s;
        try {
            s = val.getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new AmqpEncodingError(e.getMessage(), e);
        }
        System.arraycopy(s, 0, buf.data, buf.offset + offset, s.length);
    }

    public void encodeStringStr8Utf16(String val, Buffer buf, int offset) throws AmqpEncodingError {
        byte[] s;
        try {
            s = val.getBytes("utf-16");
        } catch (UnsupportedEncodingException e) {
            throw new AmqpEncodingError(e.getMessage(), e);
        }
        System.arraycopy(s, 0, buf.data, buf.offset + offset, s.length);

    }

    public void encodeStringStr8Utf8(String val, Buffer buf, int offset) throws AmqpEncodingError {
        byte[] s;
        try {
            s = val.getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new AmqpEncodingError(e.getMessage(), e);
        }
        System.arraycopy(s, 0, buf.data, buf.offset + offset, s.length);

    }

    public void encodeSymbolSym32(String val, Buffer buf, int offset) throws AmqpEncodingError {
        byte[] s;
        try {
            s = val.getBytes("us-ascii");
        } catch (UnsupportedEncodingException e) {
            throw new AmqpEncodingError(e.getMessage(), e);
        }
        System.arraycopy(s, 0, buf.data, buf.offset + offset, s.length);

    }

    public void encodeSymbolSym8(String val, Buffer buf, int offset) throws AmqpEncodingError {
        byte[] s;
        try {
            s = val.getBytes("us-ascii");
        } catch (UnsupportedEncodingException e) {
            throw new AmqpEncodingError(e.getMessage(), e);
        }
        System.arraycopy(s, 0, buf.data, buf.offset + offset, s.length);
    }

    public void encodeTimestamp(Date val, Buffer buf, int offset) throws AmqpEncodingError {
        BitUtils.setLong(buf.data, buf.offset + offset, val.getTime());
    }

    public void encodeUbyte(Short val, Buffer buf, int offset) throws AmqpEncodingError {
        BitUtils.setUByte(buf.data, buf.offset + offset, val);
    }

    public void encodeUint(Long val, Buffer buf, int offset) throws AmqpEncodingError {
        BitUtils.setUInt(buf.data, buf.offset + offset, val);
    }

    public void encodeUlong(BigInteger val, Buffer buf, int offset) throws AmqpEncodingError {
        BitUtils.setULong(buf.data, buf.offset + offset, val);
    }

    public void encodeUshort(Integer val, Buffer buf, int offset) throws AmqpEncodingError {
        BitUtils.setUShort(buf.data, buf.offset + offset, val);
    }

    public void encodeUuid(UUID val, Buffer buf, int offset) throws AmqpEncodingError {
        BitUtils.setLong(buf.data, buf.offset + offset, val.getMostSignificantBits());
        BitUtils.setLong(buf.data, buf.offset + offset + 8, val.getLeastSignificantBits());
    }

    public final int getEncodedSizeOfString(String val, STRING_ENCODING encoding) throws AmqpEncodingError {
        try {
            switch (encoding) {
            case STR32_UTF16:
            case STR8_UTF16: {
                return val.getBytes("utf-16").length;
            }
            case STR32_UTF8:
            case STR8_UTF8: {
                return val.getBytes("utf-8").length;
            }
            default:
                throw new UnsupportedEncodingException(encoding.name());
            }
        } catch (UnsupportedEncodingException uee) {
            throw new AmqpEncodingError(uee.getMessage(), uee);
        }
    }

    public final int getEncodedSizeOfSymbol(String val, SYMBOL_ENCODING encoding) {
        return val.length();
    }

    public final byte[] readBinary(AmqpBinaryMarshaller.BINARY_ENCODING encoding, int length, int count, DataInput dis) throws IOException {
        byte[] rc = new byte[length];
        dis.readFully(rc);
        return rc;
    }

    public Buffer readBinaryVbin32(int size, DataInput dis) throws IOException, AmqpEncodingError {
        return readBinaryVbin32(size, dis);
    }

    public Buffer readBinaryVbin8(int size, DataInput dis) throws IOException, AmqpEncodingError {
        return readBinaryVbin32(size, dis);
    }

    public final Byte readByte(DataInput dis) throws IOException {
        return dis.readByte();
    }

    public final Integer readChar(DataInput dis) throws IOException {
        return dis.readInt();
    }

    public final Double readDouble(DataInput dis) throws IOException {
        return dis.readDouble();
    }

    public final Float readFloat(DataInput dis) throws IOException {
        return dis.readFloat();
    }

    public final BigDecimal readDecimal32(DataInput dis) throws IOException {
        Float fl = readFloat(dis);
        return new BigDecimal(fl, MathContext.DECIMAL32).stripTrailingZeros();
    }

    public final BigDecimal readDecimal64(DataInput dis) throws IOException {
        Double dbl = readDouble(dis);
        return new BigDecimal(dbl, MathContext.DECIMAL64).stripTrailingZeros();
    }

    // TODO - implement
    public BigDecimal readDecimal128(DataInput dis) throws IOException, AmqpEncodingError {
        throw new UnsupportedOperationException("decimal128 not yet supported");
    }

    public final Integer readInt(DataInput dis) throws IOException {
        return dis.readInt();
    }

    public final Long readLong(DataInput dis) throws IOException {
        return dis.readLong();
    }

    public final Short readShort(DataInput dis) throws IOException {
        return dis.readShort();
    }

    public final String readString(AmqpStringMarshaller.STRING_ENCODING encoding, int size, int count, DataInput dis) throws IOException {
        byte[] str = new byte[size];
        dis.readFully(str);
        switch (encoding) {
        case STR32_UTF16:
        case STR8_UTF16:
            return new String(str, "utf-16");
        case STR32_UTF8:
        case STR8_UTF8:
            return new String(str, "utf-8");
        default:
            throw new UnsupportedEncodingException(encoding.name());
        }
    }

    public String readStringStr32Utf16(int size, DataInput dis) throws IOException, AmqpEncodingError {
        byte[] str = new byte[size];
        dis.readFully(str);
        return new String(str, "utf-16");
    }

    public String readStringStr32Utf8(int size, DataInput dis) throws IOException, AmqpEncodingError {
        byte[] str = new byte[size];
        dis.readFully(str);
        return new String(str, "utf-8");
    }

    public String readStringStr8Utf16(int size, DataInput dis) throws IOException, AmqpEncodingError {
        byte[] str = new byte[size];
        dis.readFully(str);
        return new String(str, "utf-16");
    }

    public String readStringStr8Utf8(int size, DataInput dis) throws IOException, AmqpEncodingError {
        byte[] str = new byte[size];
        dis.readFully(str);
        return new String(str, "utf-8");
    }

    public final String readSymbol(AmqpSymbolMarshaller.SYMBOL_ENCODING encoding, int size, int count, DataInput dis) throws IOException {
        byte[] str = new byte[size];
        dis.readFully(str);
        return new String(str, "us-ascii");
    }

    public String readSymbolSym32(int size, DataInput dis) throws IOException, AmqpEncodingError {
        byte[] str = new byte[size];
        dis.readFully(str);
        return new String(str, "us-ascii");
    }

    public String readSymbolSym8(int size, DataInput dis) throws IOException, AmqpEncodingError {
        byte[] str = new byte[size];
        dis.readFully(str);
        return new String(str, "us-ascii");
    }

    public final Date readTimestamp(DataInput dis) throws IOException {
        return new Date(dis.readLong());
    }

    public final Short readUbyte(DataInput dis) throws IOException {
        return (short) dis.readUnsignedByte();
    }

    public final Long readUint(DataInput dis) throws IOException {
        long rc = 0;
        rc = rc | (0xFFFFFFFFL & (((long) dis.readByte()) << 24));
        rc = rc | (0xFFFFFFFFL & (((long) dis.readByte()) << 16));
        rc = rc | (0xFFFFFFFFL & (((long) dis.readByte()) << 8));
        rc = rc | (0xFFFFFFFFL & (long) dis.readByte());

        return rc;
    }

    public final BigInteger readUlong(DataInput dis) throws IOException {
        byte[] rc = new byte[9];
        rc[0] = 0;
        dis.readFully(rc, 1, 8);
        return new BigInteger(rc);
    }

    public final Integer readUshort(DataInput dis) throws IOException {
        int rc = 0;
        rc = rc | (0xFFFF & (((int) dis.readByte()) << 8));
        rc = rc | (0xFFFF & (int) dis.readByte());

        return rc;
    }

    public final UUID readUuid(DataInput dis) throws IOException {
        return new UUID(dis.readLong(), dis.readLong());
    }

    public final void writeBinary(byte[] val, AmqpBinaryMarshaller.BINARY_ENCODING encoding, DataOutput dos) throws IOException {
        dos.write(val);
    }

    public void writeBinaryVbin32(Buffer val, DataOutput out) throws IOException, AmqpEncodingError {
        out.write(val.data, val.offset, val.length);
    }

    public void writeBinaryVbin8(Buffer val, DataOutput out) throws IOException, AmqpEncodingError {
        out.write(val.data, val.offset, val.length);

    }

    public final void writeByte(Byte val, DataOutput dos) throws IOException {
        dos.writeByte(val);
    }

    public final void writeChar(Integer val, DataOutput dos) throws IOException {
        dos.writeInt(val);

    }

    public final void writeDouble(Double val, DataOutput dos) throws IOException {
        dos.writeLong(Double.doubleToLongBits(val));
    }


    public final void writeFloat(Float val, DataOutput dos) throws IOException {
        dos.writeInt(Float.floatToIntBits(val));
    }

    public final void writeDecimal32(BigDecimal val, DataOutput dos) throws IOException {
        BigDecimal withContext = new BigDecimal(val.toPlainString(), MathContext.DECIMAL32);
        writeFloat(withContext.floatValue(), dos);
    }

    public final void writeDecimal64(BigDecimal val, DataOutput dos) throws IOException {
        BigDecimal withContext = new BigDecimal(val.toPlainString(), MathContext.DECIMAL64);
        writeDouble(withContext.doubleValue(), dos);
    }

    public void writeDecimal128(BigDecimal val, DataOutput buf) throws IOException, AmqpEncodingError {
        // TODO - implement
        throw new UnsupportedOperationException("decimal128 not yet supported");
    }

    public final void writeInt(Integer val, DataOutput dos) throws IOException {
        dos.writeInt(val);
    }

    public final void writeLong(Long val, DataOutput dos) throws IOException {
        dos.writeLong(val);
    }

    public final void writeShort(Short val, DataOutput dos) throws IOException {
        dos.writeShort(val);
    }

    public final void writeString(String val, AmqpStringMarshaller.STRING_ENCODING encoding, DataOutput dos) throws IOException {
        switch (encoding) {
        case STR32_UTF16:
        case STR8_UTF16: {
            dos.write(val.getBytes("utf-16"));
        }
        case STR32_UTF8:
        case STR8_UTF8: {
            dos.write(val.getBytes("utf-8"));
        }
        default:
            throw new UnsupportedEncodingException(encoding.name());
        }

    }

    public void writeStringStr32Utf16(String val, DataOutput out) throws IOException, AmqpEncodingError {
        out.write(val.getBytes("utf-16"));

    }

    public void writeStringStr32Utf8(String val, DataOutput out) throws IOException, AmqpEncodingError {
        out.write(val.getBytes("utf-8"));

    }

    public void writeStringStr8Utf16(String val, DataOutput out) throws IOException, AmqpEncodingError {
        out.write(val.getBytes("utf-16"));

    }

    public void writeStringStr8Utf8(String val, DataOutput out) throws IOException, AmqpEncodingError {
        out.write(val.getBytes("utf-8"));
    }

    public final void writeSymbol(String val, SYMBOL_ENCODING encoding, DataOutput dos) throws IOException {
        dos.write(val.getBytes("us-ascii"));
    }

    public void writeSymbolSym32(String val, DataOutput out) throws IOException, AmqpEncodingError {
        out.write(val.getBytes("us-ascii"));
    }

    public void writeSymbolSym8(String val, DataOutput out) throws IOException, AmqpEncodingError {
        out.write(val.getBytes("us-ascii"));
    }

    public final void writeTimestamp(Date val, DataOutput dos) throws IOException {
        dos.writeLong(val.getTime());
    }

    public final void writeUbyte(Short val, DataOutput dos) throws IOException {
        dos.writeByte(val);
    }

    public final void writeUint(Long val, DataOutput dos) throws IOException {
        if ( val != null ) {
            dos.writeInt((int) val.longValue());
        }
    }

    public final void writeUlong(BigInteger val, DataOutput dos) throws IOException {
        byte[] b = val.toByteArray();
        if (b.length > 8) {
            for (int i = 0; i < b.length - 8; i++) {
                if (b[i] > 0) {
                    throw new UnsupportedEncodingException("Unsigned long too large");
                }
            }
        }
        byte[] toWrite = new byte[8];
        int length = toWrite.length - 1;
        for ( int i = length; i >= 0; i-- ) {
            int position = length - i;
            if ( position < b.length ) {
                toWrite[i] = b[position];
            } else {
                toWrite[i] = 0x0;
            }
        }
        dos.write(toWrite);
    }

    public final void writeUshort(Integer val, DataOutput dos) throws IOException {
        dos.writeShort((short) val.intValue());
    }

    public final void writeUuid(UUID val, DataOutput dos) throws IOException {
        dos.writeLong(val.getMostSignificantBits());
        dos.writeLong(val.getLeastSignificantBits());
    }
}
