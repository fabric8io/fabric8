/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.codec;

import org.fusesource.fabric.apollo.amqp.codec.interfaces.AmqpType;
import org.fusesource.fabric.apollo.amqp.codec.interfaces.Frame;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.TypeReader;
import org.fusesource.hawtbuf.Buffer;

import java.io.*;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Represents an AMQP frame
 */
public class AmqpFrame {

    public static final int AMQP_FRAME_TYPE = 0x00;
    public static final int AMQP_SASL_FRAME_TYPE = 0x01;

    protected Buffer header = new Buffer(8);
    protected Buffer extHeader = new Buffer(0);
    protected Buffer encodedBody = null;
    Frame body = null;

    protected int bytesRead = 0;

    protected static final int SIZE_OFFSET = 0;
    protected static final int DOFF_OFFSET = 4;
    protected static final int TYPE_OFFSET = 5;
    protected static final int CHANNEL_OFFSET = 6;

    public AmqpFrame() {
        this.body = null;
    }

    public AmqpFrame(Frame body) {
        this.body = body;
    }

    public AmqpFrame(DataInput in) throws Exception {
        read(in);
    }

    public AmqpFrame(ReadableByteChannel channel) throws Exception {
        bytesRead = read(channel);
    }

    public boolean equals(AmqpFrame other) {
        if ( other == null ) {
            return false;
        }

        if ( !header.equals(other.header) ) {
            return false;
        }

        if ( !extHeader.equals(other.extHeader) ) {
            return false;
        }

        if ( this.body != null && other.body == null ) {
            return false;
        }

        if ( body == null && other.body == null ) {
            return true;
        }

        return body.toString().equals(other.body.toString());
    }

    public int calculateDataOffset() {
        return (header.length + extHeader.length) / 4;
    }

    public int read(ReadableByteChannel channel) throws IOException {
        int rc = 0;
        rc += channel.read(header.toByteBuffer());
        initExtHeader();
        rc += channel.read(extHeader.toByteBuffer());
        encodedBody = new Buffer((int)getDataSize());
        rc += channel.read(encodedBody.toByteBuffer());
        return rc;
    }

    public int write(WritableByteChannel channel) throws IOException {
        int rc = 0;
        setDoff(calculateDataOffset());
        setSize(getFrameSize());
        rc += channel.write(header.toByteBuffer());
        rc += channel.write(extHeader.toByteBuffer());
        if ( body == null && encodedBody != null ) {
            rc += channel.write(encodedBody.toByteBuffer());
        } else if ( body != null ) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            encodedBody = new Buffer(bos.toByteArray());
            rc += channel.write(encodedBody.toByteBuffer());
        }
        return rc;
    }

    public void read(DataInput in) throws Exception {
        header.readFrom(in);
        initExtHeader();
        extHeader.readFrom(in);
        if ( getDataSize() > 0 ) {
            body = (Frame)TypeReader.read(in);
        }
        if ( getDataSize() == 0 ) {
            body = null;
        }
    }

    private void initExtHeader() {
        int dataOffset = getDoff() * 4;
        if (dataOffset > header.length) {
            extHeader = new Buffer(dataOffset - header.length);
        }
    }

    public void handle(AmqpHandler handler) throws Exception {
        // TODO
        /*
        if ( body != null && body instanceof Frame ) {
            ((AmqpCommand)body).handle(handler);
        } else if ( body == null ) {
            handler.handleEmpty();
        } else if ( !(body instanceof AmqpCommand) ) {
            handler.handleUnknown(body);
        }
        */
    }

    public long getDataSize() {
        return getSize() - (header.length() + extHeader.length());
    }

    public void write(DataOutput out) throws Exception {
        setDoff(calculateDataOffset());
        setSize(getFrameSize());
        header.writeTo(out);
        extHeader.writeTo(out);
        if ( body != null ) {
            body.write(out);
        }
    }

    public long getFrameSize() {
        long ret = header.length + extHeader.length;
        if ( body != null ) {
            ret += body.size();
        }
        return ret;
    }

    public <T> T getBody(Class<T> type) throws Exception {
        body = getBody();
        if ( type.isInstance(body) ) {
            return type.cast(body);
        }
        return null;
    }

    public Frame getBody() throws Exception {
        if ( body == null && encodedBody != null ) {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(encodedBody.data));
            body = (Frame)TypeReader.read(in);
            encodedBody = null;
        }
        return body;
    }

    public void setBody(Frame body) {
        this.encodedBody = null;
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
        Frame type = null;
        try {
            type = getBody();
        } catch (Exception e) {
            // type will just show up as null
        }
        return "AmqpFrame{size=" + getSize() + " dataOffset=" + getDoff() + " channel=" + getChannel() + " type=" + getType() + " body={" + type + "}}";
    }
}
