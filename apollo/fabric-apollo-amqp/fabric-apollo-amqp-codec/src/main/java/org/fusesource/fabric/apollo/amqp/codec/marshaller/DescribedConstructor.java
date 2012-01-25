/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.amqp.codec.marshaller;

import org.fusesource.fabric.apollo.amqp.codec.types.AMQPSymbol;
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPULong;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;

import java.io.DataOutput;
import java.math.BigInteger;

/**
 *
 */
public class DescribedConstructor {

    protected Buffer buffer;

    public DescribedConstructor(BigInteger descriptor) {
        int size = (int) (1 + TypeRegistry.instance().sizer().sizeOfULong(descriptor));
        DataByteArrayOutputStream out = new DataByteArrayOutputStream(size);
        try {
            out.writeByte(0x0);
            AMQPULong.write(descriptor, out);
            buffer = out.toBuffer();
        } catch (Exception e) {
            throw new RuntimeException("Exception constructing DescribedConstructor instance for descriptor " + descriptor + " : " + e.getMessage());
        }
    }

    public DescribedConstructor(Buffer descriptor) {
        int size = (int) (1 + TypeRegistry.instance().sizer().sizeOfSymbol(descriptor));
        DataByteArrayOutputStream out = new DataByteArrayOutputStream(size);
        try {
            out.writeByte(0x0);
            AMQPSymbol.write(descriptor, out);
            buffer = out.toBuffer();
        } catch (Exception e) {
            throw new RuntimeException("Exception constructing DescribedConstructor instance for descriptor " + descriptor + " : " + e.getMessage());
        }
    }

    public Buffer getBuffer() {
        return buffer;
    }

    public void write(DataOutput out) throws Exception {
        buffer.writeTo(out);
    }

    public long size() {
        return buffer.length();
    }
}
