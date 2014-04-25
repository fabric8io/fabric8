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

import org.vertx.java.core.buffer.Buffer;

import static io.fabric8.gateway.handlers.detecting.protocol.BufferSupport.startsWith;

/**
 */
public class AmqpHeader {

    static final Buffer PREFIX = new Buffer(new byte[]{
      'A', 'M', 'Q', 'P'
    });

    private Buffer buffer;

    public AmqpHeader(){
        this(new Buffer(new byte[]{
          'A', 'M', 'Q', 'P', 0, 1, 0, 0
        }));
    }

    public AmqpHeader(Buffer buffer){
        setBuffer(buffer);
    }

    public int getProtocolId() {
        return buffer.getByte(4) & 0xFF;
    }
    public void setProtocolId(int value) {
        buffer.setByte(4,  (byte) value);
    }

    public int getMajor() {
        return buffer.getByte(5) & 0xFF;
    }
    public void setMajor(int value) {
        buffer.setByte(5, (byte) value);
    }

    public int getMinor() {
        return buffer.getByte(6) & 0xFF;
    }
    public void setMinor(int value) {
        buffer.setByte(6,  (byte) value);
    }

    public int getRevision() {
        return buffer.getByte(7) & 0xFF;
    }
    public void setRevision(int value) {
        buffer.setByte(7, (byte) value);
    }

    public Buffer getBuffer() {
        return buffer;
    }
    public void setBuffer(Buffer value) {
        if( !startsWith(value, PREFIX) || value.length()!=8 ) {
            throw new IllegalArgumentException("Not an AMQP header buffer");
        }
        buffer = value.copy();
    }

    @Override
    public String toString() {
        return buffer.toString();
    }
}
