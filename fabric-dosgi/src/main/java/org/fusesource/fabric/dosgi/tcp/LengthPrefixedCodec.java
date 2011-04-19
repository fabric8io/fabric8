package org.fusesource.fabric.dosgi.tcp;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

import org.fusesource.fabric.dosgi.io.ProtocolCodec;

public class LengthPrefixedCodec implements ProtocolCodec {


    int write_buffer_size = 1024 * 64;
    long write_counter = 0L;
    WritableByteChannel write_channel;
    ByteBuffer next_write_buffer = ByteBuffer.allocate(write_buffer_size);
    ByteBuffer write_buffer = ByteBuffer.allocate(0);


    protected boolean isFull() {
        return next_write_buffer.position() >= (write_buffer_size >> 2);
    }

    protected boolean isEmpty() {
        return write_buffer.remaining() == 0;
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
        if (isFull()) {
            return BufferState.FULL;
        } else {
            boolean wasEmpty = isEmpty();
            byte[] bytes = (byte[]) value;
            if (next_write_buffer.remaining() < 4 + bytes.length) {
                int new_capacity = next_write_buffer.capacity() << 1;
                ByteBuffer new_buffer = ByteBuffer.allocate(new_capacity);
                new_buffer.put(next_write_buffer.array(), 0, next_write_buffer.position());
                next_write_buffer = new_buffer;
            }
            next_write_buffer.putInt(bytes.length);
            next_write_buffer.put(bytes);
            return wasEmpty ? BufferState.WAS_EMPTY : BufferState.NOT_EMPTY;
        }
    }

    public BufferState flush() throws IOException {
        if (isEmpty() && next_write_buffer.position() > 0) {
            // size of next buffer is based on how much was used in the previous buffer.
            int prev_size = Math.min(Math.max(write_buffer.position() + 512, 512), write_buffer_size);
            write_buffer = next_write_buffer;
            write_buffer.flip();
            next_write_buffer = ByteBuffer.allocate(prev_size);
        }
        if (write_buffer.remaining() != 0) {
            write_counter += write_channel.write(write_buffer);
        }
        return isEmpty() ? BufferState.EMPTY : BufferState.NOT_EMPTY;
    }

    public long getWriteCounter() {
        return write_counter;
    }

    long read_counter = 0L;
    int read_buffer_size = 1024 * 64;
    ReadableByteChannel read_channel = null;
    ByteBuffer read_buffer = ByteBuffer.allocate(read_buffer_size);
    int read_end = 0;
    int read_start = 0;


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
        if (read_end == read_buffer.position()) {
            // do we need a new data buffer to read data into??
            if (read_buffer.remaining() == 0) {
                // How much data is still not consumed
                int size = read_end - read_start;
                int new_capacity = (read_start == 0) ? size + read_buffer_size : (size > read_buffer_size) ? size + read_buffer_size : read_buffer_size;
                ByteBuffer new_buffer = ByteBuffer.allocate(new_capacity);
                new_buffer.put(read_buffer.array(), read_start, size);
                read_buffer = new_buffer;
                read_start = 0;
                read_end = size;
            }
            int count = read_channel.read(read_buffer);
            if (count == -1) {
                throw new EOFException("Peer disconnected");
            } else if (count == 0) {
                return null;
            }
            read_counter += count;
            read_end += count;
        }

        if (read_end - read_start < 4) {
            return null;
        }
        int size = read_buffer.getInt(read_start);
        if (read_end - read_start - 4 < size) {
            return null;
        }
        byte[] data = new byte[size];
        System.arraycopy(read_buffer.array(), read_start + 4, data, 0, size);
        assert(read_start <= read_end);
        assert(read_end <= read_buffer.position());
        return data;
    }

    public long getReadCounter() {
        return read_counter;
    }

}
