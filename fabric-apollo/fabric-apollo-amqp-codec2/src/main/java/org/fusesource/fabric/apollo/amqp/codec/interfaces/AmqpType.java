package org.fusesource.fabric.apollo.amqp.codec.interfaces;

import java.io.DataInput;
import java.io.DataOutput;

public interface AmqpType {

    public Object getArrayConstructor();

    public void write(DataOutput out) throws Exception;
    public byte writeConstructor(DataOutput out) throws Exception;
    public void writeBody(byte formatCode, DataOutput out) throws Exception;

    public void read(byte formatCode, DataInput in) throws Exception;

    /*
    public void encodeTo(Buffer buffer, int offset) throws Exception;
    public void decodeFrom(byte formatCode, Buffer buffer, int offset) throws Exception;
    */

    public long size();
    public long sizeOfConstructor();
    public long sizeOfBody();

}