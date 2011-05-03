/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fusemq.amqp.codec.marshaller;

import org.fusesource.fusemq.amqp.codec.BitUtils;

public class AmqpVersion {

    private static final short PROTOCOL_ID = 0;
    public static final byte[] MAGIC = new byte[] { 'A', 'M', 'Q', 'P', PROTOCOL_ID };

    private final short major;
    private final short minor;
    private final short revision;

    private final int hashCode;

    public AmqpVersion(short major, short minor, short revision) {
        this.major = major;
        this.minor = minor;
        this.revision = revision;
        this.hashCode = BitUtils.getInt(new byte[] { PROTOCOL_ID, (byte) (major & 0xFF), (byte) (minor & 0xFF), (byte) (revision & 0xFF) }, 0);
    }

    public short getProtocolId() {
        return PROTOCOL_ID;
    }

    public short getMajor() {
        return major;
    }

    public short getMinor() {
        return minor;
    }

    public short getRevision() {
        return revision;
    }

    public int hashCode() {
        return hashCode;
    }

    public boolean equals(Object o) {
        if (o.hashCode() != hashCode) {
            return false;
        } else {
            return o instanceof AmqpVersion;
        }
    }

    public boolean equals(AmqpVersion version) {
        return version.hashCode == hashCode;
    }
}
