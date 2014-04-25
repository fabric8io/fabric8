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
package io.fabric8.gateway.handlers.detecting.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;

import java.io.IOException;
import java.net.ProtocolException;

import static io.fabric8.gateway.handlers.detecting.protocol.BufferSupport.indexOf;

/**
 * An abstract base class used to implement a Vertx handler which
 * decode a buffer stream to a protocol specific frame objects.
 */
public abstract class ProtocolDecoder<T> implements Handler<Buffer> {

    private static final transient Logger LOG = LoggerFactory.getLogger(ProtocolDecoder.class);

    private String error;
    private Handler<T> codecHander;
    private Handler<String> errorHandler;

    protected Buffer buff = null;
    protected long bytesDecoded;
    protected int readStart;
    protected Action<T> nextDecodeAction;
    protected int readEnd;

    abstract protected Action<T> initialDecodeAction();

    public static interface Action<T> {
        T apply() throws IOException;
    }

    public ProtocolDecoder<T> codecHandler(Handler<T> codecHander) {
        this.codecHander = codecHander;
        return this;
    }

    public ProtocolDecoder<T> errorHandler(Handler<String> errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    @Override
    public void handle(Buffer event) {
        if( error==null ) {
            if( buff == null ) {
                buff = event;
            } else {
                buff.appendBuffer(event);
            }
            try {
                T rc = read();
                while( rc !=null ) {
                    codecHander.handle(rc);
                    rc = read();
                }
            } catch (Exception e) {
                LOG.debug("Protocol decoding failure: "+e, e);
                error = e.getMessage();
                errorHandler.handle(error);
            }
        }
    }

    public T read() throws IOException {
        T command = null;
        if (buff !=null && readEnd < buff.length() ) {
            if( nextDecodeAction == null ) {
                nextDecodeAction = initialDecodeAction();
            }
            command = nextDecodeAction.apply();

            // did we fully read the buffer?
            if( readStart == buff.length() ) {
                buff = null;
                readEnd -= readStart;
                readStart = 0;
            }
            // Are we wasting too much space?
            else if( readStart > 1024*4 ) {
                // lets compact the buffer...
                buff = buff.getBuffer(readStart, buff.length());
                readEnd -= readStart;
                readStart = 0;
            }

            assert readStart <= readEnd;
        }
        return command;
    }

    protected Buffer readUntil(Byte octet) throws ProtocolException {
        return readUntil(octet, -1);
    }

    protected Buffer readUntil(Byte octet, int max) throws ProtocolException {
        return readUntil(octet, max, "Maximum protocol buffer length exceeded");
    }

    protected Buffer readUntil(byte octet, int max, String msg) throws ProtocolException {
        int pos = indexOf(buff, readEnd, buff.length(), octet);
        if (pos >= 0) {
            int offset = readStart;
            readEnd = pos + 1;
            bytesDecoded += readEnd-readStart;
            readStart = readEnd;
            int length = readEnd - offset;
            if (max >= 0 && length > max) {
                throw new ProtocolException(msg);
            }
            return buff.getBuffer(offset, readEnd);
        } else {
            readEnd += buff.length();
            if (max >= 0 && (readEnd - readStart) > max) {
                throw new ProtocolException(msg);
            }
            return null;
        }
    }

    protected Buffer readBytes(int length) {
        readEnd = readStart + length;
        if (buff.length() < readEnd) {
            return null;
        } else {
            bytesDecoded += readEnd-readStart;
            int offset = readStart;
            readStart = readEnd;
            return buff.getBuffer(offset, readEnd);
        }
    }

    protected Buffer peekBytes(int length) {
        readEnd = readStart + length;
        if (buff.length() < readEnd) {
            return null;
        } else {
            return buff.getBuffer(readStart, readEnd);
        }
    }

    public long getBytesDecoded() {
        return bytesDecoded;
    }
}
