/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

