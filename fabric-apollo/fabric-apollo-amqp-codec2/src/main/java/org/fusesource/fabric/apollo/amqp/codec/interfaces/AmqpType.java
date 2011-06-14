package org.fusesource.fabric.apollo.amqp.codec.interfaces;

import org.fusesource.hawtbuf.Buffer;

import java.io.DataInput;
import java.io.DataOutput;

public interface AmqpType {

    public void write(DataOutput out) throws Exception;
    public void read(DataInput in, int size, int count) throws Exception;

    public void encodeTo(Buffer buffer, int offset) throws Exception;
    public void decodeFrom(Buffer buffer, int offset, int size, int count) throws Exception;

}