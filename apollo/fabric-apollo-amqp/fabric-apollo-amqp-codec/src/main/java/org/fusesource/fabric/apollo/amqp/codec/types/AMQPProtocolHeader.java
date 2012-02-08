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

package org.fusesource.fabric.apollo.amqp.codec.types;

import org.fusesource.fabric.apollo.amqp.codec.AMQPDefinitions;
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame;
import org.fusesource.fabric.apollo.amqp.codec.marshaller.AMQPProtocolHeaderCodec;
import org.fusesource.hawtbuf.Buffer;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class AMQPProtocolHeader implements AMQPFrame {

    public static Buffer PROTOCOL_HEADER = init();

    private static Buffer init() {
        Buffer rc = new Buffer(AMQPProtocolHeaderCodec.INSTANCE.getFixedSize());
        try {
            AMQPProtocolHeaderCodec.INSTANCE.encode(new AMQPProtocolHeader(), new DataOutputStream(rc.out()));
        } catch (IOException e) {
            throw new RuntimeException("Error initializing static protocol header buffer : " + e.getMessage());
        }

        return rc;
    }


    public short protocolId;
    public short major;
    public short minor;
    public short revision;

    public AMQPProtocolHeader() {
        protocolId = AMQPDefinitions.PROTOCOL_ID;
        major = AMQPDefinitions.MAJOR;
        minor = AMQPDefinitions.MINOR;
        revision = AMQPDefinitions.REVISION;
    }

    public AMQPProtocolHeader(AMQPProtocolHeader value) {
        this.protocolId = value.protocolId;
        this.major = value.major;
        this.minor = value.minor;
        this.revision = value.revision;
    }

    public String toString() {
        return String.format("AmqpProtocolHeader : id=%s major=%s minor=%s revision=%s", protocolId, major, minor, revision);
    }
}
