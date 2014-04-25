/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.gateway.handlers.detecting.protocol.mqtt;

import io.fabric8.gateway.handlers.detecting.protocol.ProtocolDecoder;
import org.fusesource.mqtt.codec.MQTTFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;

import java.io.IOException;

/**
 * Implements protocol decoding for the STOMP protocol.
 */
class MqttProtocolDecoder extends ProtocolDecoder<MQTTFrame> {

    private static final transient Logger LOG = LoggerFactory.getLogger(MqttProtocolDecoder.class);

    private final MqttProtocol protocol;

    public MqttProtocolDecoder(MqttProtocol protocol) {
        this.protocol = protocol;
    }

    @Override
    protected Action<MQTTFrame> initialDecodeAction() {
        return readHeader;
    }

    private final Action<MQTTFrame> readHeader = new Action<MQTTFrame>() {
        public MQTTFrame apply() throws IOException {
            int length = readLength();
            if( length >= 0 ) {
                if( length > protocol.maxMessageLength) {
                    throw new IOException("The maximum message length was exceeded");
                }
                byte header = buff.getByte(readStart);
                int headerSize = readEnd-readStart;
                bytesDecoded += headerSize;
                readStart = readEnd;
                if( length > 0 ) {
                    nextDecodeAction = readBody(header, length);
                    return nextDecodeAction.apply();
                } else {
                    return new MQTTFrame().header(header);
                }
            }
            return null;
        }
    };


    private int readLength() throws IOException {
        readEnd = readStart+2; // Header is at least 2 bytes..
        int limit = buff.length();
        int length = 0;
        int multiplier = 1;
        byte digit;

        while (readEnd-1 < limit) {
            // last byte is part of the encoded length..
            digit = buff.getByte(readEnd - 1);
            length += (digit & 0x7F) * multiplier;
            if( (digit & 0x80) == 0 ) {
                return length;
            }

            // length extends out one more byte..
            multiplier <<= 7;
            readEnd++;
        }
        return -1;
    }

    Action<MQTTFrame> readBody(final byte header, final int remaining) {
        return new Action<MQTTFrame>() {
            public MQTTFrame apply() throws IOException {
                Buffer body = readBytes(remaining);
                if( body==null ) {
                    return null;
                } else {
                    nextDecodeAction = readHeader;
                    // TODO: optimize out this conversion to byte[]
                    return new MQTTFrame(new org.fusesource.hawtbuf.Buffer(body.getBytes())).header(header);
                }
            }
        };
    }

}
