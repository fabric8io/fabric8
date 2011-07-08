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

import org.fusesource.fabric.apollo.amqp.codec.types.AmqpSequence;

import java.io.DataOutput;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class AmqpSequenceMessage extends BareMessage<List<AmqpSequence>> {

    public AmqpSequenceMessage() {
        data = new ArrayList<AmqpSequence>();
    }

    public long size() {
        long rc = 0;
        for (AmqpSequence s : data) {
            if (s != null) {
                rc += s.size();
            }
        }
        return rc;
    }

    @Override
    public void write(DataOutput out) throws Exception {
        if (data != null) {
            for (AmqpSequence s : data) {
                if (s != null) {
                    s.write(out);
                }
            }
        }
    }

}
