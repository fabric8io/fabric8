/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.codec;

import org.fusesource.hawtbuf.AsciiBuffer;

/**
 * Definitions for the AMQP protocol
 */
public interface AMQPDefinitions {

    /**
     * Protocol magic identifier
     */
    public static final byte[] MAGIC = new AsciiBuffer("AMQP").getData();

    /**
     * Default IANA assigned AMQP port as an int
     */
    public static final int PORT = Integer.parseInt(Definitions.PORT);

    /**
     * Default IANA assigned secure AMQP (amqps) port as an int
     */
    public static final int SECURE_PORT = Integer.parseInt(Definitions.SECURE_PORT);

    /**
     * Protocol ID, only used in the preamble, is defined to just be 0
     */
    public static final byte PROTOCOL_ID = 0x0;

    /**
     * SASL Protocol ID
     */
    public static final byte SASL_PROTOCOL_ID = 0x3;

    /**
     * Major as a byte
     */
    public static final byte MAJOR = Byte.parseByte(Definitions.MAJOR);

    /**
     * Minor as a byte
     */
    public static final byte MINOR = Byte.parseByte(Definitions.MINOR);

    /**
     * Revision as a byte
     */
    public static final byte REVISION = Byte.parseByte(Definitions.REVISION);

    public static final int MIN_MAX_FRAME_SIZE = Integer.parseInt(Definitions.MIN_MAX_FRAME_SIZE);

}
