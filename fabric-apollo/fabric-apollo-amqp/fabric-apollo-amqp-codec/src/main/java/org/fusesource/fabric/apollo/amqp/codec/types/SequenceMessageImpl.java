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

import org.fusesource.fabric.apollo.amqp.codec.api.SequenceMessage;

import java.io.DataOutput;
import java.util.List;

/**
 *
 */
public class SequenceMessageImpl extends BareMessageImpl<List<AMQPSequence>> implements SequenceMessage {

    public SequenceMessageImpl() {

    }

    public SequenceMessageImpl(List sequence) {
        data = sequence;
    }

    public long dataSize() {
        long rc = 0;
        for ( AMQPSequence s : data ) {
            if ( s != null ) {
                rc += s.size();
            }
        }
        return rc;
    }

    @Override
    public void dataWrite(DataOutput out) throws Exception {
        for ( AMQPSequence s : data ) {
            if ( s != null ) {
                s.write(out);
            }
        }
    }

}
