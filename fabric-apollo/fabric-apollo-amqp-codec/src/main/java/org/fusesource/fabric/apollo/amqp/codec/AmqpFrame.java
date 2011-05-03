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

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.AmqpMarshaller;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.Encoded;
import org.fusesource.fabric.apollo.amqp.codec.types.AmqpNull;
import org.fusesource.fabric.apollo.amqp.codec.types.AmqpType;

import java.io.*;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 *
 *   <section name="framing" label="frame layout and encoding">
 *  <doc>
 *    <p>
 *      Frames are divided into three distinct areas: a fixed width frame header, a variable width
 *      extended header, and a variable width frame body.
 *    </p>
 *    <pre>
 *    required        optional        optional
 *+--------------+-----------------+------------+
 *| frame header | extended header | frame body |
 *+--------------+-----------------+------------+
 *  8 bytes        *variable*      *variable*
 *    </pre>
 *    <dl>
 *      <dt>frame header</dt>
 *      <dd><p>The frame header is a fixed size (8 byte) structure that precedes each frame. The
 *          frame header includes mandatory information required to parse the rest of the frame
 *          including size and type information.</p></dd>
 *      <dt>extended header</dt>
 *      <dd><p>The extended header is a variable width area preceeding the frame body. This is an
 *          extension point defined for future expansion. The treatment of this area depends on the
 *          frame type.</p></dd>
 *      <dt>frame body</dt>
 *      <dd><p>The frame body is a variable width sequence of bytes the format of which depends on
 *          the frame type.</p></dd>
 *    </dl>
 *  </doc>
 *  <doc title="Frame Layout">
 *    <p>
 *      The diagram below shows the details of the general frame layout for all frame types.
 *    </p>
 *    <pre>
 *         +0       +1       +2       +3
 *     +-----------------------------------+ -.
 *   0 |                SIZE               |  |
 *     +-----------------------------------+  |---> Frame Header
 *   4 |  DOFF  |  TYPE  | <TYPE-SPECIFIC> |  |      (8 bytes)
 *     +-----------------------------------+ -'
 *     +-----------------------------------+ -.
 *   8 |                ...                |  |
 *     .                                   .  |---> Extended Header
 *     .          <TYPE-SPECIFIC>          .  |  (DOFF * 4 - 8) bytes
 *     |                ...                |  |
 *     +-----------------------------------+ -'
 *     +-----------------------------------+ -.
 *DOFF |                                   |  |
 *     .                                   .  |
 *     .                                   .  |
 *     .                                   .  |
 *     .          <TYPE-SPECIFIC>          .  |---> Frame Body
 *     .                                   .  |  (SIZE - DOFF * 4) bytes
 *     .                                   .  |
 *     .                                   .  |
 *     .                           ________|  |
 *     |                ...       |           |
 *     +--------------------------+          -'
 *    </pre>
 *    <dl>
 *      <dt>SIZE</dt>
 *      <dd><p>Bytes 0-3 of the frame header contain the frame size. This is an unsigned 32-bit
 *          integer that MUST contain the total frame size of the frame header, extended header, and
 *          frame body. The frame is malformed if the size is less than the the size of the required
 *          frame header (8 bytes).</p></dd>
 *      <dt>DOFF</dt>
 *      <dd><p>Byte 4 of the frame header is the data offset. This gives the position of the body
 *          within the frame. The value of the data offset is unsigned 8-bit integer specifying a
 *          count of 4 byte words. Due to the mandatory 8 byte frame header, the frame is malformed
 *          if the value is less than 2.</p></dd>
 *      <dt>TYPE</dt>
 *      <dd><p>Byte 5 of the frame header is a type code. The type code indicates the format and
 *          purpose of the frame. The subsequent bytes in the frame header may be interpreted
 *          differently depending on the type of the frame. A type code of 0x00 indicates that the
 *          frame is an AMQP frame. </p></dd>
 *    </dl>
 *  </doc>
 *  <doc title="AMQP Frames">
 *    <p>
 *      The AMQP frame type defines header bytes 6 and 7 to contain a channel number.
 *      <todo class="presentation">need to reference what a channel is</todo> The AMQP frame type
 *      defines bodies encoded as described types in the AMQP type system.
 *    </p>
 *    <pre>
 *            type: 0x00 - AMQP frame
 *         +0       +1       +2       +3
 *     +-----------------------------------+ -.
 *   0 |                SIZE               |  |
 *     +-----------------------------------+  |---> Frame Header
 *   4 |  DOFF  |  TYPE  |     CHANNEL     |  |      (8 bytes)
 *     +-----------------------------------+ -'
 *     +-----------------------------------+ -.
 *   8 |                ...                |  |
 *     .                                   .  |---> Extended Header
 *     .             <IGNORED>             .  |  (DOFF * 4 - 8) bytes
 *     |                ...                |  |
 *     +-----------------------------------+ -'
 *     +-----------------------------------+ -.
4*DOFF |                                   |  |
 *8    .                                   .  |
 *     .                                   .  |
 *     .      Open / Begin / Attach        .  |
 *     .   Flow / Transfer / Disposition   .  |---> Frame Body
 *     .      Detach / End / Close         .  |  (SIZE - DOFF * 4) bytes
 *     .                                   .  |
 *     .                                   .  |
 *     .                           ________|  |
 *     |                ...       |           |
 *     +--------------------------+          -'
 *    </pre>
 *    <p>
 *      A frame with no body may be used to generate artificial traffic as needed to satisfy any
 *      negotiated heartbeat interval. Other than resetting the heartbeat timer, an empty AMQP frame
 *      has no effect on the recipient.
 *    </p>
 *  </doc>
 *</section>
 *
 */

public class AmqpFrame {

    static AmqpMarshaller marshaller = org.fusesource.fabric.apollo.amqp.codec.marshaller.v1_0_0.AmqpMarshaller.getMarshaller();

    protected Buffer header = new Buffer(8);
    protected Buffer extHeader = new Buffer(0);
    protected Buffer encodedBody = null;
    AmqpType<?, ?> body = null;

    protected int bytesRead = 0;

    protected static final int SIZE_OFFSET = 0;
    protected static final int DOFF_OFFSET = 4;
    protected static final int TYPE_OFFSET = 5;
    protected static final int CHANNEL_OFFSET = 6;

    private AmqpFrame() {

    }

    public AmqpFrame(AmqpCommand body) {
        setType(0);
        this.body = (AmqpType)body;
        if ( this.body instanceof AmqpNull) {
            this.body = null;
        }
        setDoff(calculateDataOffset());
        setSize(getFrameSize());
    }

    public AmqpFrame(DataInput in) throws IOException {
        setType(0);
        read(in);
    }

    public AmqpFrame(ReadableByteChannel channel) throws IOException {
        setType(0);
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

    public void read(DataInput in) throws IOException {
        header.readFrom(in);
        initExtHeader();
        extHeader.readFrom(in);
        if ( getDataSize() > 0 ) {
            body = marshaller.unmarshalType(in);
        }
        if ( getDataSize() == 0 || body instanceof AmqpNull ) {
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
        if ( body != null && body instanceof AmqpCommand ) {
            ((AmqpCommand)body).handle(handler);
        } else if ( body == null ) {
            handler.handleEmpty();
        } else if ( !(body instanceof AmqpCommand) ) {
            handler.handleUnknown(body);
        }
    }

    public long getDataSize() {
        return getSize() - (header.length() + extHeader.length());
    }

    public void write(DataOutput out) throws IOException {
        setDoff(calculateDataOffset());
        setSize(getFrameSize());
        header.writeTo(out);
        extHeader.writeTo(out);
        if ( body != null ) {
            body.marshal(out, marshaller);
        }
    }

    public long getFrameSize() {
        long ret = header.length + extHeader.length;
        if ( body != null ) {
            Encoded<?> encoded = body.getBuffer(marshaller).getEncoded();
            ret += encoded.getEncodedSize();
        }
        return ret;
    }

    public <T> T getBody(Class<T> type) throws IOException {
        body = getBody();
        if ( type.isInstance(body) ) {
            return type.cast(body);
        }
        return null;
    }

    public AmqpType<?, ?> getBody() throws IOException {
        if ( body == null && encodedBody != null ) {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(encodedBody.data));
            body = marshaller.unmarshalType(in);
            encodedBody = null;
        }
        return body;
    }

    public void setBody(AmqpType<?, ?> body) {
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
        return (int)BitUtils.getUByte(header.data, TYPE_OFFSET);
    }

    public final void setDoff(int doff) {
        BitUtils.setUByte(header.data, DOFF_OFFSET, (short)doff);
    }

    public final int getDoff() {
       return (int)BitUtils.getUByte(header.data, DOFF_OFFSET);
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
        AmqpType<?, ?> type = null;
        try {
            type = getBody();
        } catch (IOException e) {
            // type will just show up as null
        }
        return "AmqpFrame{size=" + getSize() + " dataOffset=" + getDoff() + " channel=" + getChannel() + " type=" + getType() + " body={" + type + "}}";
    }
}
