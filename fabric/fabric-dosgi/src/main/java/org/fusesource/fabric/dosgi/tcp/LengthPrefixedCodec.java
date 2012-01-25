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
package org.fusesource.fabric.dosgi.tcp;

import java.io.EOFException;
import java.io.IOException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;

import org.fusesource.fabric.dosgi.io.ProtocolCodec;
import org.fusesource.hawtbuf.Buffer;

public class LengthPrefixedCodec implements ProtocolCodec {


    int write_buffer_size = 1024 * 64;
    long write_counter = 0L;
    WritableByteChannel write_channel;
    ByteBuffer write_buffer = ByteBuffer.allocate(0);

    ArrayList<Buffer> next_write_buffers = new ArrayList<Buffer>();
    int next_write_size = 0;

    public boolean full() {
        return next_write_size >= (write_buffer_size >> 1);
    }

    protected boolean empty() {
        return write_buffer.remaining() == 0 && next_write_size==0;
    }

    public void setWritableByteChannel(WritableByteChannel channel) {
        this.write_channel = channel;
        if (channel instanceof SocketChannel) {
            try {
                ((SocketChannel) channel).socket().setSendBufferSize(write_buffer_size);
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
    }

    public BufferState write(Object value) throws IOException {
        if (full()) {
            return BufferState.FULL;
        } else {
            boolean wasEmpty = empty();
            Buffer buffer = (Buffer) value;
            next_write_size += buffer.length;
            next_write_buffers.add(buffer);
            return wasEmpty ? BufferState.WAS_EMPTY : BufferState.NOT_EMPTY;
        }
    }

    public BufferState flush() throws IOException {
        if (write_buffer.remaining() == 0 && next_write_size > 0) {
            if( next_write_buffers.size()==1 ) {
                write_buffer = next_write_buffers.remove(0).toByteBuffer();
            } else {
                // consolidate the buffers into 1 big buffer to reduce
                // the number of system calls we do.

                if( write_buffer.capacity() < next_write_size ) {
                    // Re-allocate if we need a bigger write buffer...
                    write_buffer = ByteBuffer.allocate(next_write_size);
                } else if( next_write_size < write_buffer_size && write_buffer.capacity() > write_buffer_size )  {
                    // Re-allocate if We don't need that big write buffer anymore..
                    write_buffer = ByteBuffer.allocate(next_write_size);
                }

                write_buffer.clear();
                for( Buffer b: next_write_buffers) {
                    write_buffer.put(b.data, b.offset, b.length);
                }
                next_write_buffers.clear();
                next_write_size = 0;
                write_buffer.flip();
            }

        }
        if (write_buffer.remaining() != 0) {
            write_counter += write_channel.write(write_buffer);
        }
        return empty() ? BufferState.EMPTY : BufferState.NOT_EMPTY;
    }

    public long getWriteCounter() {
        return write_counter;
    }

    long read_counter = 0L;
    int read_buffer_size = 1024 * 64;
    ReadableByteChannel read_channel = null;
    ByteBuffer read_buffer = ByteBuffer.allocate(4);


    public void setReadableByteChannel(ReadableByteChannel channel) {
        read_channel = channel;
        if (channel instanceof SocketChannel) {
            try {
                ((SocketChannel) channel).socket().setReceiveBufferSize(read_buffer_size);
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
    }

    public Object read() throws IOException {
        while(true) {
            if( read_buffer.remaining()!=0 ) {
                // keep reading from the channel until we fill the read buffer..
                int count = read_channel.read(read_buffer);
                if (count == -1) {
                    throw new EOFException("Peer disconnected");
                } else if (count == 0) {
                    return null;
                }
                read_counter += count;
            } else {
                //read buffer is full.. interpret it..
                read_buffer.flip();

                if( read_buffer.capacity() == 4 ) {
                    // Finding out the
                    int size = read_buffer.getInt(0);
                    if( size < 4 ) {
                        throw new ProtocolException("Expecting a size greater than 3");
                    }
                    if( size == 4 ) {
                        // weird.. empty frame.. guess it could happen.
                        Buffer rc = new Buffer(read_buffer);
                        read_buffer = ByteBuffer.allocate(4);
                        return rc;
                    } else {
                        // Resize to the right size.. this resumes the reads..
                        ByteBuffer next = ByteBuffer.allocate(size);
                        next.putInt(size);
                        read_buffer = next;
                    }
                } else {
                    // finish loading the rest of the buffer..
                    Buffer rc = new Buffer(read_buffer);
                    read_buffer = ByteBuffer.allocate(4);
                    return rc;
                }
            }
        }
    }

    public long getReadCounter() {
        return read_counter;
    }

}
