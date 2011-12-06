/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.codec.marshaller;

import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPType;

import java.io.DataOutput;

/**
 *
 */
public class ArraySupport {

    public static Object getArrayConstructor(Object value[]) {
        AMQPType[] arr = (AMQPType[]) value;
        Object constructor;
        try {
            constructor = ((AMQPType) arr.getClass().getComponentType().newInstance()).getArrayConstructor();
        } catch (Exception e) {
            throw new RuntimeException("Error determining array size : " + e.getMessage());
        }
        return constructor;
    }

    public static long getArrayConstructorSize(Object value[]) {
        Object constructor = getArrayConstructor(value);
        if ( constructor instanceof Byte ) {
            return 1;
        } else if ( constructor instanceof DescribedConstructor ) {
            return ((DescribedConstructor) constructor).size();
        }
        throw new RuntimeException("Unknown array constructor type : " + constructor.getClass().getSimpleName());
    }

    public static long getArrayBodySize(Object value[]) {
        AMQPType[] arr = (AMQPType[]) value;
        long size = 0;
        for ( AMQPType t : arr ) {
            size += t.sizeOfBody();
        }
        return size;
    }

    public static void writeArrayConstructor(Object value[], DataOutput out) throws Exception {
        Object constructor = getArrayConstructor(value);
        if ( constructor instanceof Byte ) {
            out.writeByte((Byte) constructor);
        } else if ( constructor instanceof DescribedConstructor ) {
            ((DescribedConstructor) constructor).write(out);
        }
    }

}

