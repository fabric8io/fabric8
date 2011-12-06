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

import org.fusesource.fabric.apollo.amqp.codec.api.DataMessage;
import org.fusesource.hawtbuf.Buffer;

import java.io.DataOutput;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class DataMessageImpl extends BareMessageImpl<List<Data>> implements DataMessage {

    public DataMessageImpl() {

    }

    public DataMessageImpl(Data data) {
        this.data = new ArrayList<Data>();
        this.data.add(data);
    }

    public DataMessageImpl(Buffer data) {
        this.data = new ArrayList<Data>();
        this.data.add(new Data(data));
    }

    public DataMessageImpl(List data) {
        this.data = new ArrayList<Data>();
        for ( Object obj : data ) {
            if ( obj instanceof Buffer ) {
                this.data.add(new Data((Buffer) obj));
            } else if ( obj instanceof Data ) {
                this.data.add((Data) obj);
            }
        }
    }

    @Override
    public long dataSize() {
        long rc = 0;
        for ( Data d : data ) {
            if ( d != null ) {
                rc += d.size();
            }
        }
        return rc;
    }

    @Override
    public void dataWrite(DataOutput out) throws Exception {
        if ( data != null ) {
            for ( Data d : data ) {
                if ( d != null ) {
                    d.write(out);
                }
            }
        }
    }

}
