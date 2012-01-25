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

import org.fusesource.fabric.apollo.amqp.codec.AMQPDefinitions;
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPProtocolHeader;
import org.fusesource.hawtbuf.codec.Codec;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class AMQPProtocolHeaderCodec implements Codec<AMQPProtocolHeader> {

    public static final AMQPProtocolHeaderCodec INSTANCE = new AMQPProtocolHeaderCodec();

    public AMQPProtocolHeader decode(DataInput in) throws IOException {
        byte magic[] = new byte[4];
        in.readFully(magic);
        if( !Arrays.equals(magic, AMQPDefinitions.MAGIC) ) {
            throw new IOException("Invalid magic");
        }
        AMQPProtocolHeader rc = new AMQPProtocolHeader();
        rc.protocolId = (short) (in.readByte() & 0xFF);
        rc.major = (short) (in.readByte() & 0xFF);
        rc.minor = (short) (in.readByte() & 0xFF);
        rc.revision = (short) (in.readByte() & 0xFF);
        return rc;
    }

    public void encode(AMQPProtocolHeader value, DataOutput out) throws IOException {
        out.write(AMQPDefinitions.MAGIC);
        out.write(value.protocolId);
        out.writeByte(value.major);
        out.write(value.minor);
        out.write(value.revision);
    }

    public int getFixedSize() {
        return 8;
    }

    public boolean isEstimatedSizeSupported() {
        return true;
    }

    public int estimatedSize(AMQPProtocolHeader value) {
        return 8;
    }

    public boolean isDeepCopySupported() {
        return true;
    }

    public AMQPProtocolHeader deepCopy(AMQPProtocolHeader value) {
        return new AMQPProtocolHeader(value);
    }
}
