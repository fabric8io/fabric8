/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.codec.interfaces;

import java.io.DataInput;
import java.io.DataOutput;

public interface AMQPType {

    public Object getArrayConstructor();

    public void write(DataOutput out) throws Exception;
    public byte writeConstructor(DataOutput out) throws Exception;
    public void writeBody(byte formatCode, DataOutput out) throws Exception;

    public void read(byte formatCode, DataInput in) throws Exception;

    /*
    public void encodeTo(Buffer buffer, int offset) throws Exception;
    public void decodeFrom(byte formatCode, Buffer buffer, int offset) throws Exception;
    */

    public long size();
    public long sizeOfConstructor();
    public long sizeOfBody();

}
