/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.codec.types;

import org.fusesource.fabric.apollo.amqp.codec.marshaller.BitUtils;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.DataByteArrayInputStream;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Represents an AMQP frame
 */
public class AMQPFrame {

    public static final int AMQP_FRAME_TYPE = 0x00;
    public static final int AMQP_SASL_FRAME_TYPE = 0x01;

    protected Buffer header = new Buffer(8);
    protected Buffer extHeader = new Buffer(0);
    protected Buffer body = null;

    protected int bytesRead = 0;

    protected static final int SIZE_OFFSET = 0;
    protected static final int DOFF_OFFSET = 4;
    protected static final int TYPE_OFFSET = 5;
    protected static final int CHANNEL_OFFSET = 6;

    public AMQPFrame() {

    }

    public AMQPFrame(Buffer body) {
        this.body = body;
    }

    public AMQPFrame(DataInput in) throws Exception {
        read(in);
    }

    public AMQPFrame(ReadableByteChannel channel) throws Exception {
        bytesRead = read(channel);
    }

    public boolean equals(AMQPFrame other) {
        if ( other == null ) {
            return false;
        }
        if ( !header.equals(other.header) ) {
            return false;
        }
        if ( !extHeader.equals(other.extHeader) ) {
            return false;
        }
        if (this.body == null && other.body != null) {
            return false;
        }
        return body.equals(other.body);
    }

    public int calculateDataOffset() {
        return (header.length + extHeader.length) / 4;
    }

    public int read(ReadableByteChannel channel) throws IOException {
        int rc = 0;
        rc += channel.read(header.toByteBuffer());
        initExtHeader();
        rc += channel.read(extHeader.toByteBuffer());
        body = new Buffer((int)getDataSize());
        rc += channel.read(body.toByteBuffer());
        return rc;
    }

    public int write(WritableByteChannel channel) throws IOException {
        int rc = 0;
        setDoff(calculateDataOffset());
        setSize(getFrameSize());
        rc += channel.write(header.toByteBuffer());
        rc += channel.write(extHeader.toByteBuffer());
        if ( body != null ) {
            rc += channel.write(body.toByteBuffer());
        }
        return rc;
    }

    public void read(DataInput in) throws Exception {
        header.readFrom(in);
        initExtHeader();
        extHeader.readFrom(in);
        if ( getDataSize() > 0 ) {
            body = new Buffer((int)getDataSize());
            body.readFrom(in);
        }
    }

    private void initExtHeader() {
        int dataOffset = getDoff() * 4;
        if (dataOffset > header.length) {
            extHeader = new Buffer(dataOffset - header.length);
        }
    }

    public long getDataSize() {
        return getSize() - (header.length() + extHeader.length());
    }

    public void write(DataOutput out) throws Exception {
        setDoff(calculateDataOffset());
        setSize(getFrameSize());
        header.writeTo(out);
        extHeader.writeTo(out);
        body.writeTo(out);
    }

    public long getFrameSize() {
        long ret = header.length + extHeader.length;
        if ( body != null ) {
            ret += body.length();
        }
        return ret;
    }

    public DataInput dataInput() {
        return new DataByteArrayInputStream(body);
    }

    public Buffer getBody() {
        return body;
    }

    public void setBody(Buffer body) {
        this.body = body;
    }

    public final void setSize(long size) {
        BitUtils.setUInt(header.data, SIZE_OFFSET, size);
    }

    public long getSize() {
        return BitUtils.getUInt(header.data, SIZE_OFFSET);
    }

    public final void setType(int type) {
        BitUtils.setUByte(header.data, TYPE_OFFSET, (short)type);
    }

    public final int getType() {
        return BitUtils.getUByte(header.data, TYPE_OFFSET);
    }

    public final void setDoff(int doff) {
        BitUtils.setUByte(header.data, DOFF_OFFSET, (short)doff);
    }

    public final int getDoff() {
       return BitUtils.getUByte(header.data, DOFF_OFFSET);
    }

    public final void setChannel(int channel) {
        BitUtils.setUShort(header.data, CHANNEL_OFFSET, channel);
    }

    public final int getChannel() {
        return BitUtils.getUShort(header.data, CHANNEL_OFFSET);
    }

    public int getBytesRead() {
        return bytesRead;
    }

    public String toString() {
        return "AmqpFrame{size=" + getSize() + " dataOffset=" + getDoff() + " channel=" + getChannel() + " type=" + getType() + " body={" + body + "}}";
    }
}
