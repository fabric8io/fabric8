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

import org.fusesource.fabric.apollo.amqp.codec.interfaces.Frame;

import java.io.DataInput;
import java.io.DataOutput;

/**
 *
 */
public class NoPerformative implements Frame {

    public static final NoPerformative INSTANCE = new NoPerformative();

    public Object getArrayConstructor() {
        return null;
    }

    public void write(DataOutput out) throws Exception {
    }

    public byte writeConstructor(DataOutput out) throws Exception {
        return 0;
    }

    public void writeBody(byte formatCode, DataOutput out) throws Exception {

    }

    public void read(byte formatCode, DataInput in) throws Exception {

    }

    public long size() {
        return 0;
    }

    public long sizeOfConstructor() {
        return 0;
    }

    public long sizeOfBody() {
        return 0;
    }

    public String toString() {
        return getClass().getSimpleName();
    }
}
