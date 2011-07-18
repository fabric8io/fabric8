/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 * 	http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.codec.types;

import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame;
import org.fusesource.fabric.apollo.amqp.codec.interfaces.Frame;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.BitUtils;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.TypeReader;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.DataByteArrayInputStream;

import java.io.DataInput;
import java.io.DataOutput;

/**
 * Represents an AMQP frame
 */
public class AMQPTransportFrame implements AMQPFrame {

    public static final int AMQP_FRAME_TYPE = 0x00;
    public static final int AMQP_SASL_FRAME_TYPE = 0x01;

    protected Buffer header = new Buffer(8);
    protected Buffer extHeader = new Buffer(0);
    protected Frame performative = null;
    protected Buffer payload = null;

    protected static final int SIZE_OFFSET = 0;
    protected static final int DOFF_OFFSET = 4;
    protected static final int TYPE_OFFSET = 5;
    protected static final int CHANNEL_OFFSET = 6;

    public AMQPTransportFrame() {

    }

    public AMQPTransportFrame(Frame performative) {
        this.performative = performative;
    }

    public AMQPTransportFrame(int channel, Frame performative) {
        setChannel(channel);
        this.performative = performative;
        setDoff(calculateDataOffset());
        setSize(getFrameSize());
    }

    public AMQPTransportFrame(int channel, Frame performative, Buffer payload) {
        setChannel(channel);
        this.performative = performative;
        this.payload = payload;
        setDoff(calculateDataOffset());
        setSize(getFrameSize());
    }

    public AMQPTransportFrame(Buffer header, Buffer body) {
        this.header = header;
        initExtHeader(body);
        fromBuffer(body);
    }

    private void fromBuffer(Buffer body) {
        try {
            DataByteArrayInputStream in = new DataByteArrayInputStream(body);
            performative = (Frame) TypeReader.read(in);
            if (in.available() > 0) {
                payload = new Buffer(in.available());
                payload.readFrom((DataInput)in);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error creating frame from buffer : " + e.getMessage());
        }
    }

    public AMQPTransportFrame(DataInput in) throws Exception {
        read(in);
    }

    public boolean equals(AMQPTransportFrame other) {
        if ( other == null ) {
            return false;
        }
        if ( !header.equals(other.header) ) {
            return false;
        }
        if ( !extHeader.equals(other.extHeader) ) {
            return false;
        }
        if ( this.payload == null && other.payload != null ) {
            return false;
        }
        return payload.equals(other.payload);
    }

    public int calculateDataOffset() {
        return (header.length + extHeader.length) / 4;
    }

    public void read(DataInput in) throws Exception {
        header.readFrom(in);
        initExtHeader();
        extHeader.readFrom(in);
        if ( getDataSize() > 0 ) {
            Buffer body = new Buffer((int) getDataSize());
            body.readFrom(in);
            fromBuffer(body);
        }
    }

    private void initExtHeader() {
        initExtHeader(null);
    }

    private void initExtHeader(Buffer body) {
        int dataOffset = getDoff() * 4;
        if ( dataOffset > header.length ) {
            if (body == null) {
                extHeader = new Buffer(dataOffset - header.length);
            } else {
                extHeader = new Buffer(body.data, 0, dataOffset - header.length);
                body.offset(dataOffset - header.length);
            }
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
        if (performative != null) {
            performative.write(out);
        }
        if (payload != null) {
            payload.writeTo(out);
        }
    }

    public long getFrameSize() {
        long ret = header.length + extHeader.length;
        if (performative != null) {
            ret += performative.size();
        }
        if ( payload != null ) {
            ret += payload.length();
        }
        return ret;
    }

    public <T extends Frame> T getPerformative() {
        return (T)performative;
    }

    public void setPerformative(Frame performative) {
        this.performative = performative;
    }

    public Buffer getPayload() {
        return payload;
    }

    public void setPayload(Buffer buffer) {
        this.payload = payload;
    }

    public final void setSize(long size) {
        BitUtils.setUInt(header.data, SIZE_OFFSET, size);
    }

    public long getSize() {
        return BitUtils.getUInt(header.data, SIZE_OFFSET);
    }

    public final void setType(int type) {
        BitUtils.setUByte(header.data, TYPE_OFFSET, (short) type);
    }

    public final int getType() {
        return BitUtils.getUByte(header.data, TYPE_OFFSET);
    }

    public final void setDoff(int doff) {
        BitUtils.setUByte(header.data, DOFF_OFFSET, (short) doff);
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

    public String toString() {
        return "AmqpFrame{size=" + getSize() + " dataOffset=" + getDoff() + " channel=" + getChannel() + " type=" + getType() + " performative=" + performative + " payload=" + payload + "}";

    }
}
