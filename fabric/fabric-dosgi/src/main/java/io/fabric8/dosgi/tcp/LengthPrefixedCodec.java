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
package io.fabric8.dosgi.tcp;

import java.io.EOFException;
import java.io.IOException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedList;
import java.util.Queue;

import io.fabric8.dosgi.io.ProtocolCodec;
import org.fusesource.hawtbuf.Buffer;

public class LengthPrefixedCodec implements ProtocolCodec {


    final int write_buffer_size = 1024 * 64;
    long write_counter = 0L;
    WritableByteChannel write_channel;

    final Queue<ByteBuffer> next_write_buffers = new LinkedList<ByteBuffer>();
    int next_write_size = 0;

    public boolean full() {
        return false;
    }

    protected boolean empty() {
        if (next_write_size > 0) {
            return false;
        }
        if (!next_write_buffers.isEmpty()) {
            for (ByteBuffer b : next_write_buffers) {
                if (b.remaining() > 0) {
                    return false;
                }
            }
        }
        return true;
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
            next_write_buffers.add(buffer.toByteBuffer());
            return wasEmpty ? BufferState.WAS_EMPTY : BufferState.NOT_EMPTY;
        }
    }

    public BufferState flush() throws IOException {
        final long writeCounterBeforeFlush = write_counter;
        while(!next_write_buffers.isEmpty()) {
            final ByteBuffer nextBuffer = next_write_buffers.peek();
            if (nextBuffer.remaining() < 1) {
                next_write_buffers.remove();
                continue;
            }
            int bytesWritten = write_channel.write(nextBuffer);
            write_counter += bytesWritten;
            next_write_size -= bytesWritten;
            if (nextBuffer.remaining() > 0) {
                break;
            }
        }
        if (empty()) {
            if (writeCounterBeforeFlush == write_counter) {
                return BufferState.WAS_EMPTY;
            } else {
                return BufferState.EMPTY;
            }
        }
        return BufferState.NOT_EMPTY;
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
