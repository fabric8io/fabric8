/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.codec.types;

import org.fusesource.fabric.apollo.amqp.codec.AMQPDefinitions;
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.AMQPProtocolHeaderCodec;
import org.fusesource.hawtbuf.Buffer;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class AMQPProtocolHeader implements AMQPFrame {

    public static Buffer PROTOCOL_HEADER = init();

    private static Buffer init() {
        Buffer rc = new Buffer(AMQPProtocolHeaderCodec.INSTANCE.getFixedSize());
        try {
            AMQPProtocolHeaderCodec.INSTANCE.encode(new AMQPProtocolHeader(), new DataOutputStream(rc.out()));
        } catch (IOException e) {
            throw new RuntimeException("Error initializing static protocol header buffer : " + e.getMessage());
        }

        return rc;
    }


    public short protocolId;
    public short major;
    public short minor;
    public short revision;

    public AMQPProtocolHeader() {
        protocolId = AMQPDefinitions.PROTOCOL_ID;
        major = AMQPDefinitions.MAJOR;
        minor = AMQPDefinitions.MINOR;
        revision = AMQPDefinitions.REVISION;
    }

    public AMQPProtocolHeader(AMQPProtocolHeader value) {
        this.protocolId = value.protocolId;
        this.major = value.major;
        this.minor = value.minor;
        this.revision = value.revision;
    }

    public String toString() {
        return String.format("AmqpProtocolHeader : id=%s major=%s minor=%s revision=%s", protocolId, major, minor, revision);
    }
}
