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

import org.fusesource.fabric.apollo.amqp.codec.AmqpDefinitions;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class AmqpProtocolHeader {

    public short protocolId;
    public short major;
    public short minor;
    public short revision;

    public AmqpProtocolHeader() {
        protocolId = AmqpDefinitions.PROTOCOL_ID;
        major = AmqpDefinitions.MAJOR;
        minor = AmqpDefinitions.MINOR;
        revision = AmqpDefinitions.REVISION;
    }

    public AmqpProtocolHeader(AmqpProtocolHeader value) {
        this.protocolId = value.protocolId;
        this.major = value.major;
        this.minor = value.minor;
        this.revision = value.revision;
    }

    public String toString() {
        return String.format("AmqpProtocolHeader : id=%s major=%s minor=%s revision=%s", protocolId, major, minor, revision);
    }
}
