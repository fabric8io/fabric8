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
package io.fabric8.gateway.handlers.detecting.protocol.amqp;

import io.fabric8.gateway.handlers.detecting.protocol.ProtocolDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;

import java.io.IOException;

/**
 * Implements protocol decoding for the AMQP protocol.
 */
class AmqpProtocolDecoder extends ProtocolDecoder<AmqpEvent> {

    private static final transient Logger LOG = LoggerFactory.getLogger(AmqpProtocolDecoder.class);

    private final AmqpProtocol protocol;

    public AmqpProtocolDecoder(AmqpProtocol protocol) {
        this.protocol = protocol;
    }

    @Override
    protected Action<AmqpEvent> initialDecodeAction() {
        return new Action<AmqpEvent>() {
            public AmqpEvent apply() throws IOException {
                Buffer magic = readBytes(8);
                if (magic != null) {
                    nextDecodeAction = readFrameSize;
                    return new AmqpEvent(AmqpEvent.Type.HEADER, magic, new AmqpHeader(magic));
                } else {
                    return null;
                }
            }
        };
    }

    private final Action<AmqpEvent> readFrameSize = new Action<AmqpEvent>() {
        public AmqpEvent apply() throws IOException {
            Buffer sizeBytes = peekBytes(4);
            if (sizeBytes != null) {
                int size = sizeBytes.getInt(0);
                if (size < 8) {
                    throw new IOException(String.format("specified frame size %d is smaller than minimum frame size", size));
                }
                if( size > protocol.maxFrameSize ) {
                    throw new IOException(String.format("specified frame size %d is larger than maximum frame size", size));
                }
                nextDecodeAction = readFrame(size);
                return nextDecodeAction.apply();
            } else {
                return null;
            }
        }
    };


    private final Action<AmqpEvent> readFrame(final int size) {
        return new Action<AmqpEvent>() {
            public AmqpEvent apply() throws IOException {
                Buffer frameData = readBytes(size);
                if (frameData != null) {
                    nextDecodeAction = readFrameSize;
                    return new AmqpEvent(AmqpEvent.Type.FRAME, frameData, null);
                } else {
                    return null;
                }
            }
        };
    }

    public void skipProtocolHeader() {
        nextDecodeAction = readFrameSize;
    }

    public void readProtocolHeader() {
        nextDecodeAction = initialDecodeAction();
    }

}
