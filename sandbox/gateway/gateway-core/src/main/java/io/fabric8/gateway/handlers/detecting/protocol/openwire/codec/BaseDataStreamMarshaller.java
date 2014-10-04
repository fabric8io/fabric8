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
package io.fabric8.gateway.handlers.detecting.protocol.openwire.codec;

import io.fabric8.gateway.handlers.detecting.protocol.openwire.OpenwireException;
import io.fabric8.gateway.handlers.detecting.protocol.openwire.command.DataStructure;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.DataByteArrayInputStream;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;
import org.fusesource.hawtbuf.UTF8Buffer;

import java.io.IOException;
import java.lang.reflect.Constructor;

public abstract class BaseDataStreamMarshaller implements DataStreamMarshaller {

    public static final Constructor STACK_TRACE_ELEMENT_CONSTRUCTOR;

    static {
        Constructor constructor = null;
        try {
            constructor = StackTraceElement.class.getConstructor(new Class[] {UTF8Buffer.class, UTF8Buffer.class,
                                                                              UTF8Buffer.class, int.class});
        } catch (Throwable e) {
        }
        STACK_TRACE_ELEMENT_CONSTRUCTOR = constructor;
    }

    public abstract byte getDataStructureType();

    public abstract DataStructure createObject();

    public int tightMarshal1(OpenWireFormat wireFormat, Object o, BooleanStream bs) throws IOException {
        return 0;
    }

    public void tightMarshal2(OpenWireFormat wireFormat, Object o, DataByteArrayOutputStream dataOut, BooleanStream bs)
        throws IOException {
    }

    public void tightUnmarshal(OpenWireFormat wireFormat, Object o, DataByteArrayInputStream dataIn, BooleanStream bs)
        throws IOException {
    }

    public int tightMarshalLong1(OpenWireFormat wireFormat, long o, BooleanStream bs) throws IOException {
        if (o == 0) {
            bs.writeBoolean(false);
            bs.writeBoolean(false);
            return 0;
        } else if ((o & 0xFFFFFFFFFFFF0000L) == 0) {
            bs.writeBoolean(false);
            bs.writeBoolean(true);
            return 2;
        } else if ((o & 0xFFFFFFFF00000000L) == 0) {
            bs.writeBoolean(true);
            bs.writeBoolean(false);
            return 4;
        } else {
            bs.writeBoolean(true);
            bs.writeBoolean(true);
            return 8;
        }
    }

    public void tightMarshalLong2(OpenWireFormat wireFormat, long o, DataByteArrayOutputStream dataOut, BooleanStream bs)
        throws IOException {
        if (bs.readBoolean()) {
            if (bs.readBoolean()) {
                dataOut.writeLong(o);
            } else {
                dataOut.writeInt((int)o);
            }
        } else {
            if (bs.readBoolean()) {
                dataOut.writeShort((int)o);
            }
        }
    }

    public long tightUnmarshalLong(OpenWireFormat wireFormat, DataByteArrayInputStream dataIn, BooleanStream bs)
        throws IOException {
        if (bs.readBoolean()) {
            if (bs.readBoolean()) {
                return dataIn.readLong();
            } else {
                return toLong(dataIn.readInt());
            }
        } else {
            if (bs.readBoolean()) {
                return toLong(dataIn.readShort());
            } else {
                return 0;
            }
        }
    }

    protected long toLong(short value) {
        // lets handle negative values
        long answer = value;
        return answer & 0xffffL;
    }

    protected long toLong(int value) {
        // lets handle negative values
        long answer = value;
        return answer & 0xffffffffL;
    }

    protected DataStructure tightUnmarsalNestedObject(OpenWireFormat wireFormat, DataByteArrayInputStream dataIn,
                                                      BooleanStream bs) throws IOException {
        return wireFormat.tightUnmarshalNestedObject(dataIn, bs);
    }

    protected int tightMarshalNestedObject1(OpenWireFormat wireFormat, DataStructure o, BooleanStream bs)
        throws IOException {
        return wireFormat.tightMarshalNestedObject1(o, bs);
    }

    protected void tightMarshalNestedObject2(OpenWireFormat wireFormat, DataStructure o, DataByteArrayOutputStream dataOut,
                                             BooleanStream bs) throws IOException {
        wireFormat.tightMarshalNestedObject2(o, dataOut, bs);
    }

    protected DataStructure tightUnmarsalCachedObject(OpenWireFormat wireFormat, DataByteArrayInputStream dataIn,
                                                      BooleanStream bs) throws IOException {
        if (wireFormat.isCacheEnabled()) {
            if (bs.readBoolean()) {
                short index = dataIn.readShort();
                DataStructure object = wireFormat.tightUnmarshalNestedObject(dataIn, bs);
                wireFormat.setInUnmarshallCache(index, object);
                return object;
            } else {
                short index = dataIn.readShort();
                return wireFormat.getFromUnmarshallCache(index);
            }
        } else {
            return wireFormat.tightUnmarshalNestedObject(dataIn, bs);
        }
    }

    protected int tightMarshalCachedObject1(OpenWireFormat wireFormat, DataStructure o, BooleanStream bs)
        throws IOException {
        if (wireFormat.isCacheEnabled()) {
            Short index = wireFormat.getMarshallCacheIndex(o);
            bs.writeBoolean(index == null);
            if (index == null) {
                int rc = wireFormat.tightMarshalNestedObject1(o, bs);
                wireFormat.addToMarshallCache(o);
                return 2 + rc;
            } else {
                return 2;
            }
        } else {
            return wireFormat.tightMarshalNestedObject1(o, bs);
        }
    }

    protected void tightMarshalCachedObject2(OpenWireFormat wireFormat, DataStructure o, DataByteArrayOutputStream dataOut,
                                             BooleanStream bs) throws IOException {
        if (wireFormat.isCacheEnabled()) {
            Short index = wireFormat.getMarshallCacheIndex(o);
            if (bs.readBoolean()) {
                dataOut.writeShort(index.shortValue());
                wireFormat.tightMarshalNestedObject2(o, dataOut, bs);
            } else {
                dataOut.writeShort(index.shortValue());
            }
        } else {
            wireFormat.tightMarshalNestedObject2(o, dataOut, bs);
        }
    }

    protected Throwable tightUnmarsalThrowable(OpenWireFormat wireFormat, DataByteArrayInputStream dataIn, BooleanStream bs)
        throws IOException {
        if (bs.readBoolean()) {
            UTF8Buffer clazz = tightUnmarshalString(dataIn, bs);
            UTF8Buffer message = tightUnmarshalString(dataIn, bs);
            Throwable o = createThrowable(clazz, message);
            if (wireFormat.isStackTraceEnabled()) {
                if (STACK_TRACE_ELEMENT_CONSTRUCTOR != null) {
                    StackTraceElement ss[] = new StackTraceElement[dataIn.readShort()];
                    for (int i = 0; i < ss.length; i++) {
                        try {
                            ss[i] = (StackTraceElement)STACK_TRACE_ELEMENT_CONSTRUCTOR
                                .newInstance(new Object[] {tightUnmarshalString(dataIn, bs),
                                                           tightUnmarshalString(dataIn, bs),
                                                           tightUnmarshalString(dataIn, bs),
                                                           Integer.valueOf(dataIn.readInt())});
                        } catch (IOException e) {
                            throw e;
                        } catch (Throwable e) {
                        }
                    }
                    o.setStackTrace(ss);
                } else {
                    short size = dataIn.readShort();
                    for (int i = 0; i < size; i++) {
                        tightUnmarshalString(dataIn, bs);
                        tightUnmarshalString(dataIn, bs);
                        tightUnmarshalString(dataIn, bs);
                        dataIn.readInt();
                    }
                }
                o.initCause(tightUnmarsalThrowable(wireFormat, dataIn, bs));

            }
            return o;
        } else {
            return null;
        }
    }

    private Throwable createThrowable(UTF8Buffer className, UTF8Buffer message) {
        try {
            Class clazz = Class.forName(className.toString(), false, BaseDataStreamMarshaller.class.getClassLoader());
            Constructor constructor = clazz.getConstructor(new Class[] {UTF8Buffer.class});
            return (Throwable)constructor.newInstance(new Object[] {message.toString()});
        } catch (Throwable e) {
            return new OpenwireException(message.toString(), className.toString());
        }
    }

    protected int tightMarshalThrowable1(OpenWireFormat wireFormat, Throwable o, BooleanStream bs)
        throws IOException {
        if (o == null) {
            bs.writeBoolean(false);
            return 0;
        } else {
            int rc = 0;
            bs.writeBoolean(true);
            String className = o instanceof OpenwireException ? ((OpenwireException)o).getClassName() : o.getClass().getName();
            rc += tightMarshalString1(new UTF8Buffer(className), bs);
            rc += tightMarshalString1(new UTF8Buffer(o.getMessage()), bs);
            if (wireFormat.isStackTraceEnabled()) {
                rc += 2;
                StackTraceElement[] stackTrace = o.getStackTrace();
                for (int i = 0; i < stackTrace.length; i++) {
                    StackTraceElement element = stackTrace[i];
                    rc += tightMarshalString1(new UTF8Buffer(element.getClassName()), bs);
                    rc += tightMarshalString1(new UTF8Buffer(element.getMethodName()), bs);
                    rc += tightMarshalString1(new UTF8Buffer(element.getFileName()), bs);
                    rc += 4;
                }
                rc += tightMarshalThrowable1(wireFormat, o.getCause(), bs);
            }
            return rc;
        }
    }

    protected void tightMarshalThrowable2(OpenWireFormat wireFormat, Throwable o, DataByteArrayOutputStream dataOut,
                                          BooleanStream bs) throws IOException {
        if (bs.readBoolean()) {
            String className = o instanceof OpenwireException ? ((OpenwireException)o).getClassName() : o.getClass().getName();
            tightMarshalString2(new UTF8Buffer(className), dataOut, bs);
            tightMarshalString2(new UTF8Buffer(o.getMessage()), dataOut, bs);
            if (wireFormat.isStackTraceEnabled()) {
                StackTraceElement[] stackTrace = o.getStackTrace();
                dataOut.writeShort(stackTrace.length);
                for (int i = 0; i < stackTrace.length; i++) {
                    StackTraceElement element = stackTrace[i];
                    tightMarshalString2(new UTF8Buffer(element.getClassName()), dataOut, bs);
                    tightMarshalString2(new UTF8Buffer(element.getMethodName()), dataOut, bs);
                    tightMarshalString2(new UTF8Buffer(element.getFileName()), dataOut, bs);
                    dataOut.writeInt(element.getLineNumber());
                }
                tightMarshalThrowable2(wireFormat, o.getCause(), dataOut, bs);
            }
        }
    }

    @SuppressWarnings("deprecation")
    protected UTF8Buffer tightUnmarshalString(DataByteArrayInputStream dataIn, BooleanStream bs) throws IOException {
        if (bs.readBoolean()) {
            boolean ascii = bs.readBoolean(); // ignored for now.
            int size = dataIn.readShort();
            if( size== 0 ) {
                return new UTF8Buffer("");
            } else {
                Buffer buffer = dataIn.readBuffer(size);
                return buffer.utf8();
            }
        } else {
            return null;
        }
    }

    protected int tightMarshalString1(UTF8Buffer value, BooleanStream bs) throws IOException {
        bs.writeBoolean(value != null);
        if (value != null) {

            boolean ascii = false;
//  we could check to see if its' really ascii.. for now punt.
//            boolean ascii = true;
//            int last = value.offset+value.length;
//            for (int i = value.offset; i < last; i++) {
//                if( (value.data[i] & 0x80) !=0 ) {
//                    ascii = false;
//                }
//            }

            bs.writeBoolean(ascii);
            return value.length() + 2;

        } else {
            return 0;
        }
    }

    protected void tightMarshalString2(UTF8Buffer value, DataByteArrayOutputStream dataOut, BooleanStream bs) throws IOException {
        if (bs.readBoolean()) {
            // If we verified it only holds ascii values
            bs.readBoolean();
            dataOut.writeShort(value.length);
            dataOut.write(value);
        }
    }

    protected int tightMarshalObjectArray1(OpenWireFormat wireFormat, DataStructure[] objects,
                                           BooleanStream bs) throws IOException {
        if (objects != null) {
            int rc = 0;
            bs.writeBoolean(true);
            rc += 2;
            for (int i = 0; i < objects.length; i++) {
                rc += tightMarshalNestedObject1(wireFormat, objects[i], bs);
            }
            return rc;
        } else {
            bs.writeBoolean(false);
            return 0;
        }
    }

    protected void tightMarshalObjectArray2(OpenWireFormat wireFormat, DataStructure[] objects,
                                            DataByteArrayOutputStream dataOut, BooleanStream bs) throws IOException {
        if (bs.readBoolean()) {
            dataOut.writeShort(objects.length);
            for (int i = 0; i < objects.length; i++) {
                tightMarshalNestedObject2(wireFormat, objects[i], dataOut, bs);
            }
        }
    }

    protected int tightMarshalConstByteArray1(byte[] data, BooleanStream bs, int i) throws IOException {
        return i;
    }

    protected void tightMarshalConstByteArray2(byte[] data, DataByteArrayOutputStream dataOut, BooleanStream bs, int i)
        throws IOException {
        dataOut.write(data, 0, i);
    }

    protected byte[] tightUnmarshalConstByteArray(DataByteArrayInputStream dataIn, BooleanStream bs, int i)
        throws IOException {
        byte data[] = new byte[i];
        dataIn.readFully(data);
        return data;
    }

    protected int tightMarshalByteArray1(byte[] data, BooleanStream bs) throws IOException {
        bs.writeBoolean(data != null);
        if (data != null) {
            return data.length + 4;
        } else {
            return 0;
        }
    }

    protected void tightMarshalByteArray2(byte[] data, DataByteArrayOutputStream dataOut, BooleanStream bs)
        throws IOException {
        if (bs.readBoolean()) {
            dataOut.writeInt(data.length);
            dataOut.write(data);
        }
    }

    protected byte[] tightUnmarshalByteArray(DataByteArrayInputStream dataIn, BooleanStream bs) throws IOException {
        byte rc[] = null;
        if (bs.readBoolean()) {
            int size = dataIn.readInt();
            rc = new byte[size];
            dataIn.readFully(rc);
        }
        return rc;
    }

    protected int tightMarshalBuffer1(Buffer data, BooleanStream bs) throws IOException {
        bs.writeBoolean(data != null);
        if (data != null) {
            return data.getLength() + 4;
        } else {
            return 0;
        }
    }

    protected void tightMarshalBuffer2(Buffer data, DataByteArrayOutputStream dataOut, BooleanStream bs)
        throws IOException {
        if (bs.readBoolean()) {
            dataOut.writeInt(data.getLength());
            dataOut.write(data.getData(), data.getOffset(), data.getLength());
        }
    }

    protected Buffer tightUnmarshalBuffer(DataByteArrayInputStream dataIn, BooleanStream bs) throws IOException {
        Buffer rc = null;
        if (bs.readBoolean()) {
            int size = dataIn.readInt();
            byte[] t = new byte[size];
            dataIn.readFully(t);
            return new Buffer(t, 0, size);
        }
        return rc;
    }

    //
    // The loose marshaling logic
    //

    public void looseMarshal(OpenWireFormat wireFormat, Object o, DataByteArrayOutputStream dataOut) throws IOException {
    }

    public void looseUnmarshal(OpenWireFormat wireFormat, Object o, DataByteArrayInputStream dataIn) throws IOException {
    }

    public void looseMarshalLong(OpenWireFormat wireFormat, long o, DataByteArrayOutputStream dataOut) throws IOException {
        dataOut.writeLong(o);
    }

    public long looseUnmarshalLong(OpenWireFormat wireFormat, DataByteArrayInputStream dataIn) throws IOException {
        return dataIn.readLong();
    }

    protected DataStructure looseUnmarsalNestedObject(OpenWireFormat wireFormat, DataByteArrayInputStream dataIn)
        throws IOException {
        return wireFormat.looseUnmarshalNestedObject(dataIn);
    }

    protected void looseMarshalNestedObject(OpenWireFormat wireFormat, DataStructure o, DataByteArrayOutputStream dataOut)
        throws IOException {
        wireFormat.looseMarshalNestedObject(o, dataOut);
    }

    protected DataStructure looseUnmarsalCachedObject(OpenWireFormat wireFormat, DataByteArrayInputStream dataIn)
        throws IOException {
        if (wireFormat.isCacheEnabled()) {
            if (dataIn.readBoolean()) {
                short index = dataIn.readShort();
                DataStructure object = wireFormat.looseUnmarshalNestedObject(dataIn);
                wireFormat.setInUnmarshallCache(index, object);
                return object;
            } else {
                short index = dataIn.readShort();
                return wireFormat.getFromUnmarshallCache(index);
            }
        } else {
            return wireFormat.looseUnmarshalNestedObject(dataIn);
        }
    }

    protected void looseMarshalCachedObject(OpenWireFormat wireFormat, DataStructure o, DataByteArrayOutputStream dataOut)
        throws IOException {
        if (wireFormat.isCacheEnabled()) {
            Short index = wireFormat.getMarshallCacheIndex(o);
            dataOut.writeBoolean(index == null);
            if (index == null) {
                index = wireFormat.addToMarshallCache(o);
                dataOut.writeShort(index.shortValue());
                wireFormat.looseMarshalNestedObject(o, dataOut);
            } else {
                dataOut.writeShort(index.shortValue());
            }
        } else {
            wireFormat.looseMarshalNestedObject(o, dataOut);
        }
    }

    protected Throwable looseUnmarsalThrowable(OpenWireFormat wireFormat, DataByteArrayInputStream dataIn)
        throws IOException {
        if (dataIn.readBoolean()) {
            UTF8Buffer clazz = looseUnmarshalString(dataIn);
            UTF8Buffer message = looseUnmarshalString(dataIn);
            Throwable o = createThrowable(clazz, message);
            if (wireFormat.isStackTraceEnabled()) {
                if (STACK_TRACE_ELEMENT_CONSTRUCTOR != null) {
                    StackTraceElement ss[] = new StackTraceElement[dataIn.readShort()];
                    for (int i = 0; i < ss.length; i++) {
                        try {
                            ss[i] = (StackTraceElement)STACK_TRACE_ELEMENT_CONSTRUCTOR
                                .newInstance(new Object[] {looseUnmarshalString(dataIn),
                                                           looseUnmarshalString(dataIn),
                                                           looseUnmarshalString(dataIn),
                                                           Integer.valueOf(dataIn.readInt())});
                        } catch (IOException e) {
                            throw e;
                        } catch (Throwable e) {
                        }
                    }
                    o.setStackTrace(ss);
                } else {
                    short size = dataIn.readShort();
                    for (int i = 0; i < size; i++) {
                        looseUnmarshalString(dataIn);
                        looseUnmarshalString(dataIn);
                        looseUnmarshalString(dataIn);
                        dataIn.readInt();
                    }
                }
                o.initCause(looseUnmarsalThrowable(wireFormat, dataIn));

            }
            return o;
        } else {
            return null;
        }
    }

    protected void looseMarshalThrowable(OpenWireFormat wireFormat, Throwable o, DataByteArrayOutputStream dataOut)
        throws IOException {
        dataOut.writeBoolean(o != null);
        if (o != null) {
            String className = o instanceof OpenwireException ? ((OpenwireException)o).getClassName() : o.getClass().getName();
            looseMarshalString(new UTF8Buffer(className), dataOut);
            looseMarshalString(new UTF8Buffer(o.getMessage()), dataOut);
            if (wireFormat.isStackTraceEnabled()) {
                StackTraceElement[] stackTrace = o.getStackTrace();
                dataOut.writeShort(stackTrace.length);
                for (int i = 0; i < stackTrace.length; i++) {
                    StackTraceElement element = stackTrace[i];
                    looseMarshalString(new UTF8Buffer(element.getClassName()), dataOut);
                    looseMarshalString(new UTF8Buffer(element.getMethodName()), dataOut);
                    looseMarshalString(new UTF8Buffer(element.getFileName()), dataOut);
                    dataOut.writeInt(element.getLineNumber());
                }
                looseMarshalThrowable(wireFormat, o.getCause(), dataOut);
            }
        }
    }

    protected UTF8Buffer looseUnmarshalString(DataByteArrayInputStream dataIn) throws IOException {
        if (dataIn.readBoolean()) {
            int size = dataIn.readShort();
            return dataIn.readBuffer(size).utf8();
        } else {
            return null;
        }
    }

    protected void looseMarshalString(UTF8Buffer value, DataByteArrayOutputStream dataOut) throws IOException {
        dataOut.writeBoolean(value != null);
        if (value != null) {
            dataOut.writeShort(value.length);
            dataOut.write(value);
        }
    }

    protected void looseMarshalObjectArray(OpenWireFormat wireFormat, DataStructure[] objects,
                                           DataByteArrayOutputStream dataOut) throws IOException {
        dataOut.writeBoolean(objects != null);
        if (objects != null) {
            dataOut.writeShort(objects.length);
            for (int i = 0; i < objects.length; i++) {
                looseMarshalNestedObject(wireFormat, objects[i], dataOut);
            }
        }
    }

    protected void looseMarshalConstByteArray(OpenWireFormat wireFormat, byte[] data, DataByteArrayOutputStream dataOut,
                                              int i) throws IOException {
        dataOut.write(data, 0, i);
    }

    protected byte[] looseUnmarshalConstByteArray(DataByteArrayInputStream dataIn, int i) throws IOException {
        byte data[] = new byte[i];
        dataIn.readFully(data);
        return data;
    }

    protected void looseMarshalByteArray(OpenWireFormat wireFormat, byte[] data, DataByteArrayOutputStream dataOut)
        throws IOException {
        dataOut.writeBoolean(data != null);
        if (data != null) {
            dataOut.writeInt(data.length);
            dataOut.write(data);
        }
    }

    protected byte[] looseUnmarshalByteArray(DataByteArrayInputStream dataIn) throws IOException {
        byte rc[] = null;
        if (dataIn.readBoolean()) {
            int size = dataIn.readInt();
            rc = new byte[size];
            dataIn.readFully(rc);
        }
        return rc;
    }

    protected void looseMarshalBuffer(OpenWireFormat wireFormat, Buffer data, DataByteArrayOutputStream dataOut)
        throws IOException {
        dataOut.writeBoolean(data != null);
        if (data != null) {
            dataOut.writeInt(data.getLength());
            dataOut.write(data.getData(), data.getOffset(), data.getLength());
        }
    }

    protected Buffer looseUnmarshalBuffer(DataByteArrayInputStream dataIn) throws IOException {
        Buffer rc = null;
        if (dataIn.readBoolean()) {
            int size = dataIn.readInt();
            byte[] t = new byte[size];
            dataIn.readFully(t);
            rc = new Buffer(t, 0, size);
        }
        return rc;
    }
}
