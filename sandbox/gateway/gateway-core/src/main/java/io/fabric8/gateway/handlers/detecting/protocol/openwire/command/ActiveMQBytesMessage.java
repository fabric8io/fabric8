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
package io.fabric8.gateway.handlers.detecting.protocol.openwire.command;

import io.fabric8.gateway.handlers.detecting.protocol.openwire.support.OpenwireException;
import io.fabric8.gateway.handlers.detecting.protocol.openwire.support.Settings;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.BufferEditor;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.fusesource.hawtbuf.ByteArrayOutputStream;

import java.io.*;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * @openwire:marshaller code=24
 */
public class ActiveMQBytesMessage extends ActiveMQMessage {

    public static final byte DATA_STRUCTURE_TYPE = CommandTypes.ACTIVEMQ_BYTES_MESSAGE;

    protected transient DataOutputStream dataOut;
    protected transient ByteArrayOutputStream bytesOut;
    protected transient DataInputStream dataIn;
    protected transient int length;

    public Message copy() {
        ActiveMQBytesMessage copy = new ActiveMQBytesMessage();
        copy(copy);
        return copy;
    }

    private void copy(ActiveMQBytesMessage copy) {
        storeContent();
        super.copy(copy);
        copy.dataOut = null;
        copy.bytesOut = null;
        copy.dataIn = null;
    }

    public void onSend() throws OpenwireException {
        super.onSend();
        storeContent();
    }

    private void storeContent() {
        try {
            if (dataOut != null) {
                dataOut.close();
                Buffer bs = bytesOut.toBuffer();
                if (compressed) {
                    int pos = bs.offset;
                    bs.offset = 0;
                    BufferEditor e = BufferEditor.big(bs);
                    e.writeInt(length);
                    bs.offset = pos;
                }
                setContent(bs);
                bytesOut = null;
                dataOut = null;
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage(), ioe); // TODO verify
                                                                // RuntimeException
        }
    }

    public byte getDataStructureType() {
        return DATA_STRUCTURE_TYPE;
    }

    public String getJMSXMimeType() {
        return "jms/bytes-message";
    }

    public void clearBody() throws OpenwireException {
        super.clearBody();
        this.dataOut = null;
        this.dataIn = null;
        this.bytesOut = null;
    }

    public long getBodyLength() throws OpenwireException {
        initializeReading();
        return length;
    }

    public boolean readBoolean() throws OpenwireException {
        initializeReading();
        try {
            return this.dataIn.readBoolean();
        } catch (EOFException e) {
            throw new OpenwireException(e);
        } catch (IOException e) {
            throw new OpenwireException(e);
        }
    }

    public byte readByte() throws OpenwireException {
        initializeReading();
        try {
            return this.dataIn.readByte();
        } catch (EOFException e) {
            throw new OpenwireException(e);
        } catch (IOException e) {
            throw new OpenwireException(e);
        }
    }

    public int readUnsignedByte() throws OpenwireException {
        initializeReading();
        try {
            return this.dataIn.readUnsignedByte();
        } catch (EOFException e) {
            throw new OpenwireException(e);
        } catch (IOException e) {
            throw new OpenwireException(e);
        }
    }

    public short readShort() throws OpenwireException {
        initializeReading();
        try {
            return this.dataIn.readShort();
        } catch (EOFException e) {
            throw new OpenwireException(e);
        } catch (IOException e) {
            throw new OpenwireException(e);
        }
    }

    public int readUnsignedShort() throws OpenwireException {
        initializeReading();
        try {
            return this.dataIn.readUnsignedShort();
        } catch (EOFException e) {
            throw new OpenwireException(e);
        } catch (IOException e) {
            throw new OpenwireException(e);
        }
    }

    public char readChar() throws OpenwireException {
        initializeReading();
        try {
            return this.dataIn.readChar();
        } catch (EOFException e) {
            throw new OpenwireException(e);
        } catch (IOException e) {
            throw new OpenwireException(e);
        }
    }

    public int readInt() throws OpenwireException {
        initializeReading();
        try {
            return this.dataIn.readInt();
        } catch (EOFException e) {
            throw new OpenwireException(e);
        } catch (IOException e) {
            throw new OpenwireException(e);
        }
    }

    public long readLong() throws OpenwireException {
        initializeReading();
        try {
            return this.dataIn.readLong();
        } catch (EOFException e) {
            throw new OpenwireException(e);
        } catch (IOException e) {
            throw new OpenwireException(e);
        }
    }

    public float readFloat() throws OpenwireException {
        initializeReading();
        try {
            return this.dataIn.readFloat();
        } catch (EOFException e) {
            throw new OpenwireException(e);
        } catch (IOException e) {
            throw new OpenwireException(e);
        }
    }

    public double readDouble() throws OpenwireException {
        initializeReading();
        try {
            return this.dataIn.readDouble();
        } catch (EOFException e) {
            throw new OpenwireException(e);
        } catch (IOException e) {
            throw new OpenwireException(e);
        }
    }

    public String readUTF() throws OpenwireException {
        initializeReading();
        try {
            return this.dataIn.readUTF();
        } catch (EOFException e) {
            throw new OpenwireException(e);
        } catch (IOException e) {
            throw new OpenwireException(e);
        }
    }

    public int readBytes(byte[] value) throws OpenwireException {
        return readBytes(value, value.length);
    }

    public int readBytes(byte[] value, int length) throws OpenwireException {
        initializeReading();
        try {
            int n = 0;
            while (n < length) {
                int count = this.dataIn.read(value, n, length - n);
                if (count < 0) {
                    break;
                }
                n += count;
            }
            if (n == 0 && length > 0) {
                n = -1;
            }
            return n;
        } catch (EOFException e) {
            throw new OpenwireException(e);
        } catch (IOException e) {
            throw new OpenwireException(e);
        }
    }

    public void writeBoolean(boolean value) throws OpenwireException {
        initializeWriting();
        try {
            this.dataOut.writeBoolean(value);
        } catch (IOException ioe) {
            throw new OpenwireException(ioe);
        }
    }

    public void writeByte(byte value) throws OpenwireException {
        initializeWriting();
        try {
            this.dataOut.writeByte(value);
        } catch (IOException ioe) {
            throw new OpenwireException(ioe);
        }
    }

    public void writeShort(short value) throws OpenwireException {
        initializeWriting();
        try {
            this.dataOut.writeShort(value);
        } catch (IOException ioe) {
            throw new OpenwireException(ioe);
        }
    }

    public void writeChar(char value) throws OpenwireException {
        initializeWriting();
        try {
            this.dataOut.writeChar(value);
        } catch (IOException ioe) {
            throw new OpenwireException(ioe);
        }
    }

    public void writeInt(int value) throws OpenwireException {
        initializeWriting();
        try {
            this.dataOut.writeInt(value);
        } catch (IOException ioe) {
            throw new OpenwireException(ioe);
        }
    }

    public void writeLong(long value) throws OpenwireException {
        initializeWriting();
        try {
            this.dataOut.writeLong(value);
        } catch (IOException ioe) {
            throw new OpenwireException(ioe);
        }
    }

    public void writeFloat(float value) throws OpenwireException {
        initializeWriting();
        try {
            this.dataOut.writeFloat(value);
        } catch (IOException ioe) {
            throw new OpenwireException(ioe);
        }
    }

    public void writeDouble(double value) throws OpenwireException {
        initializeWriting();
        try {
            this.dataOut.writeDouble(value);
        } catch (IOException ioe) {
            throw new OpenwireException(ioe);
        }
    }

    public void writeUTF(String value) throws OpenwireException {
        initializeWriting();
        try {
            this.dataOut.writeUTF(value);
        } catch (IOException ioe) {
            throw new OpenwireException(ioe);
        }
    }

    public void writeBytes(byte[] value) throws OpenwireException {
        initializeWriting();
        try {
            this.dataOut.write(value);
        } catch (IOException ioe) {
            throw new OpenwireException(ioe);
        }
    }

    public void writeBytes(byte[] value, int offset, int length) throws OpenwireException {
        initializeWriting();
        try {
            this.dataOut.write(value, offset, length);
        } catch (IOException ioe) {
            throw new OpenwireException(ioe);
        }
    }

    public void writeObject(Object value) throws OpenwireException {
        if (value == null) {
            throw new NullPointerException();
        }
        initializeWriting();
        if (value instanceof Boolean) {
            writeBoolean(((Boolean)value).booleanValue());
        } else if (value instanceof Character) {
            writeChar(((Character)value).charValue());
        } else if (value instanceof Byte) {
            writeByte(((Byte)value).byteValue());
        } else if (value instanceof Short) {
            writeShort(((Short)value).shortValue());
        } else if (value instanceof Integer) {
            writeInt(((Integer)value).intValue());
        } else if (value instanceof Long) {
            writeLong(((Long)value).longValue());
        } else if (value instanceof Float) {
            writeFloat(((Float)value).floatValue());
        } else if (value instanceof Double) {
            writeDouble(((Double)value).doubleValue());
        } else if (value instanceof String) {
            writeUTF(value.toString());
        } else if (value instanceof byte[]) {
            writeBytes((byte[])value);
        } else {
            throw new OpenwireException("Cannot write non-primitive type:" + value.getClass());
        }
    }

    public void reset() throws OpenwireException {
        storeContent();
        this.bytesOut = null;
        this.dataIn = null;
        this.dataOut = null;
        setReadOnlyBody(true);
    }

    private void initializeWriting() throws OpenwireException {
        checkReadOnlyBody();
        if (this.dataOut == null) {
            this.bytesOut = new ByteArrayOutputStream();
            OutputStream os = bytesOut;
            if (Settings.enable_compression()) {
                // keep track of the real length of the content if
                // we are compressed.
                try {
                    os.write(new byte[4]);
                } catch (IOException e) {
                    throw new OpenwireException(e);
                }
                length = 0;
                compressed = true;
                final Deflater deflater = new Deflater(Deflater.BEST_SPEED);
                os = new FilterOutputStream(new DeflaterOutputStream(os, deflater)) {
                    public void write(byte[] arg0) throws IOException {
                        length += arg0.length;
                        out.write(arg0);
                    }

                    public void write(byte[] arg0, int arg1, int arg2) throws IOException {
                        length += arg2;
                        out.write(arg0, arg1, arg2);
                    }

                    public void write(int arg0) throws IOException {
                        length++;
                        out.write(arg0);
                    }

                    @Override
                    public void close() throws IOException {
                        super.close();
                        deflater.end();
                    }
                };
            }
            this.dataOut = new DataOutputStream(os);
        }
    }

    protected void checkWriteOnlyBody() throws OpenwireException {
        if (!readOnlyBody) {
            throw new OpenwireException("Message body is write-only");
        }
    }

    private void initializeReading() throws OpenwireException {
        checkWriteOnlyBody();
        if (dataIn == null) {
            Buffer data = getContent();
            if (data == null) {
                data = new Buffer(new byte[] {}, 0, 0);
            }
            InputStream is = new ByteArrayInputStream(data);
            if (isCompressed()) {
                // keep track of the real length of the content if
                // we are compressed.
                try {
                    DataInputStream dis = new DataInputStream(is);
                    length = dis.readInt();
                    dis.close();
                } catch (IOException e) {
                    throw new OpenwireException(e);
                }
                is = new InflaterInputStream(is);
            } else {
                length = data.getLength();
            }
            dataIn = new DataInputStream(is);
        }
    }

    public void setObjectProperty(String name, Object value) throws OpenwireException {
        initializeWriting();
        super.setObjectProperty(name, value);
    }

    public String toString() {
        return super.toString() + " ActiveMQBytesMessage{ " + "bytesOut = " + bytesOut + ", dataOut = " + dataOut + ", dataIn = " + dataIn + " }";
    }
}
