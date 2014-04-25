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

import io.fabric8.gateway.handlers.detecting.protocol.openwire.support.MarshallingSupport;
import io.fabric8.gateway.handlers.detecting.protocol.openwire.support.OpenwireException;
import io.fabric8.gateway.handlers.detecting.protocol.openwire.support.Settings;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.fusesource.hawtbuf.ByteArrayOutputStream;

import java.io.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * @openwire:marshaller code="27"
 */
public class ActiveMQStreamMessage extends ActiveMQMessage {

    public static final byte DATA_STRUCTURE_TYPE = CommandTypes.ACTIVEMQ_STREAM_MESSAGE;

    protected transient DataOutputStream dataOut;
    protected transient ByteArrayOutputStream bytesOut;
    protected transient DataInputStream dataIn;
    protected transient int remainingBytes = -1;

    public Message copy() {
        ActiveMQStreamMessage copy = new ActiveMQStreamMessage();
        copy(copy);
        return copy;
    }

    private void copy(ActiveMQStreamMessage copy) {
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
        if (dataOut != null) {
            try {
                dataOut.close();
                setContent(bytesOut.toBuffer());
                bytesOut = null;
                dataOut = null;
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
    }

    public byte getDataStructureType() {
        return DATA_STRUCTURE_TYPE;
    }

    public String getJMSXMimeType() {
        return "jms/stream-message";
    }

    public void clearBody() throws OpenwireException {
        super.clearBody();
        this.dataOut = null;
        this.dataIn = null;
        this.bytesOut = null;
        this.remainingBytes = -1;
    }

    public boolean readBoolean() throws OpenwireException {
        initializeReading();
        try {

            this.dataIn.mark(10);
            int type = this.dataIn.read();
            if (type == -1) {
                throw new OpenwireException("reached end of data");
            }
            if (type == MarshallingSupport.BOOLEAN_TYPE) {
                return this.dataIn.readBoolean();
            }
            if (type == MarshallingSupport.STRING_TYPE) {
                return Boolean.valueOf(this.dataIn.readUTF()).booleanValue();
            }
            if (type == MarshallingSupport.NULL) {
                this.dataIn.reset();
                throw new NullPointerException("Cannot convert NULL value to boolean.");
            } else {
                this.dataIn.reset();
                throw new OpenwireException(" not a boolean type");
            }
        } catch (EOFException e) {
            throw new OpenwireException(e);
        } catch (IOException e) {
            throw new OpenwireException(e);
        }
    }

    public byte readByte() throws OpenwireException {
        initializeReading();
        try {

            this.dataIn.mark(10);
            int type = this.dataIn.read();
            if (type == -1) {
                throw new OpenwireException("reached end of data");
            }
            if (type == MarshallingSupport.BYTE_TYPE) {
                return this.dataIn.readByte();
            }
            if (type == MarshallingSupport.STRING_TYPE) {
                return Byte.valueOf(this.dataIn.readUTF()).byteValue();
            }
            if (type == MarshallingSupport.NULL) {
                this.dataIn.reset();
                throw new NullPointerException("Cannot convert NULL value to byte.");
            } else {
                this.dataIn.reset();
                throw new OpenwireException(" not a byte type");
            }
        } catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            } catch (IOException ioe) {
                throw new OpenwireException(ioe);
            }
            throw mfe;

        } catch (EOFException e) {
            throw new OpenwireException(e);
        } catch (IOException e) {
            throw new OpenwireException(e);
        }
    }

    public short readShort() throws OpenwireException {
        initializeReading();
        try {

            this.dataIn.mark(17);
            int type = this.dataIn.read();
            if (type == -1) {
                throw new OpenwireException("reached end of data");
            }
            if (type == MarshallingSupport.SHORT_TYPE) {
                return this.dataIn.readShort();
            }
            if (type == MarshallingSupport.BYTE_TYPE) {
                return this.dataIn.readByte();
            }
            if (type == MarshallingSupport.STRING_TYPE) {
                return Short.valueOf(this.dataIn.readUTF()).shortValue();
            }
            if (type == MarshallingSupport.NULL) {
                this.dataIn.reset();
                throw new NullPointerException("Cannot convert NULL value to short.");
            } else {
                this.dataIn.reset();
                throw new OpenwireException(" not a short type");
            }
        } catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            } catch (IOException ioe) {
                throw new OpenwireException(ioe);
            }
            throw mfe;

        } catch (EOFException e) {
            throw new OpenwireException(e);
        } catch (IOException e) {
            throw new OpenwireException(e);
        }

    }

    public char readChar() throws OpenwireException {
        initializeReading();
        try {

            this.dataIn.mark(17);
            int type = this.dataIn.read();
            if (type == -1) {
                throw new OpenwireException("reached end of data");
            }
            if (type == MarshallingSupport.CHAR_TYPE) {
                return this.dataIn.readChar();
            }
            if (type == MarshallingSupport.NULL) {
                this.dataIn.reset();
                throw new NullPointerException("Cannot convert NULL value to char.");
            } else {
                this.dataIn.reset();
                throw new OpenwireException(" not a char type");
            }
        } catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            } catch (IOException ioe) {
                throw new OpenwireException(ioe);
            }
            throw mfe;

        } catch (EOFException e) {
            throw new OpenwireException(e);
        } catch (IOException e) {
            throw new OpenwireException(e);
        }
    }

    public int readInt() throws OpenwireException {
        initializeReading();
        try {

            this.dataIn.mark(33);
            int type = this.dataIn.read();
            if (type == -1) {
                throw new OpenwireException("reached end of data");
            }
            if (type == MarshallingSupport.INTEGER_TYPE) {
                return this.dataIn.readInt();
            }
            if (type == MarshallingSupport.SHORT_TYPE) {
                return this.dataIn.readShort();
            }
            if (type == MarshallingSupport.BYTE_TYPE) {
                return this.dataIn.readByte();
            }
            if (type == MarshallingSupport.STRING_TYPE) {
                return Integer.valueOf(this.dataIn.readUTF()).intValue();
            }
            if (type == MarshallingSupport.NULL) {
                this.dataIn.reset();
                throw new NullPointerException("Cannot convert NULL value to int.");
            } else {
                this.dataIn.reset();
                throw new OpenwireException(" not an int type");
            }
        } catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            } catch (IOException ioe) {
                throw new OpenwireException(ioe);
            }
            throw mfe;

        } catch (EOFException e) {
            throw new OpenwireException(e);
        } catch (IOException e) {
            throw new OpenwireException(e);
        }
    }

    public long readLong() throws OpenwireException {
        initializeReading();
        try {

            this.dataIn.mark(65);
            int type = this.dataIn.read();
            if (type == -1) {
                throw new OpenwireException("reached end of data");
            }
            if (type == MarshallingSupport.LONG_TYPE) {
                return this.dataIn.readLong();
            }
            if (type == MarshallingSupport.INTEGER_TYPE) {
                return this.dataIn.readInt();
            }
            if (type == MarshallingSupport.SHORT_TYPE) {
                return this.dataIn.readShort();
            }
            if (type == MarshallingSupport.BYTE_TYPE) {
                return this.dataIn.readByte();
            }
            if (type == MarshallingSupport.STRING_TYPE) {
                return Long.valueOf(this.dataIn.readUTF()).longValue();
            }
            if (type == MarshallingSupport.NULL) {
                this.dataIn.reset();
                throw new NullPointerException("Cannot convert NULL value to long.");
            } else {
                this.dataIn.reset();
                throw new OpenwireException(" not a long type");
            }
        } catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            } catch (IOException ioe) {
                throw new OpenwireException(ioe);
            }
            throw mfe;

        } catch (EOFException e) {
            throw new OpenwireException(e);
        } catch (IOException e) {
            throw new OpenwireException(e);
        }
    }

    public float readFloat() throws OpenwireException {
        initializeReading();
        try {
            this.dataIn.mark(33);
            int type = this.dataIn.read();
            if (type == -1) {
                throw new OpenwireException("reached end of data");
            }
            if (type == MarshallingSupport.FLOAT_TYPE) {
                return this.dataIn.readFloat();
            }
            if (type == MarshallingSupport.STRING_TYPE) {
                return Float.valueOf(this.dataIn.readUTF()).floatValue();
            }
            if (type == MarshallingSupport.NULL) {
                this.dataIn.reset();
                throw new NullPointerException("Cannot convert NULL value to float.");
            } else {
                this.dataIn.reset();
                throw new OpenwireException(" not a float type");
            }
        } catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            } catch (IOException ioe) {
                throw new OpenwireException(ioe);
            }
            throw mfe;

        } catch (EOFException e) {
            throw new OpenwireException(e);
        } catch (IOException e) {
            throw new OpenwireException(e);
        }
    }

    public double readDouble() throws OpenwireException {
        initializeReading();
        try {

            this.dataIn.mark(65);
            int type = this.dataIn.read();
            if (type == -1) {
                throw new OpenwireException("reached end of data");
            }
            if (type == MarshallingSupport.DOUBLE_TYPE) {
                return this.dataIn.readDouble();
            }
            if (type == MarshallingSupport.FLOAT_TYPE) {
                return this.dataIn.readFloat();
            }
            if (type == MarshallingSupport.STRING_TYPE) {
                return Double.valueOf(this.dataIn.readUTF()).doubleValue();
            }
            if (type == MarshallingSupport.NULL) {
                this.dataIn.reset();
                throw new NullPointerException("Cannot convert NULL value to double.");
            } else {
                this.dataIn.reset();
                throw new OpenwireException(" not a double type");
            }
        } catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            } catch (IOException ioe) {
                throw new OpenwireException(ioe);
            }
            throw mfe;

        } catch (EOFException e) {
            throw new OpenwireException(e);
        } catch (IOException e) {
            throw new OpenwireException(e);
        }
    }

    public String readString() throws OpenwireException {
        initializeReading();
        try {

            this.dataIn.mark(65);
            int type = this.dataIn.read();
            if (type == -1) {
                throw new OpenwireException("reached end of data");
            }
            if (type == MarshallingSupport.NULL) {
                return null;
            }
            if (type == MarshallingSupport.BIG_STRING_TYPE) {
                return MarshallingSupport.readUTF8(dataIn);
            }
            if (type == MarshallingSupport.STRING_TYPE) {
                return this.dataIn.readUTF();
            }
            if (type == MarshallingSupport.LONG_TYPE) {
                return new Long(this.dataIn.readLong()).toString();
            }
            if (type == MarshallingSupport.INTEGER_TYPE) {
                return new Integer(this.dataIn.readInt()).toString();
            }
            if (type == MarshallingSupport.SHORT_TYPE) {
                return new Short(this.dataIn.readShort()).toString();
            }
            if (type == MarshallingSupport.BYTE_TYPE) {
                return new Byte(this.dataIn.readByte()).toString();
            }
            if (type == MarshallingSupport.FLOAT_TYPE) {
                return new Float(this.dataIn.readFloat()).toString();
            }
            if (type == MarshallingSupport.DOUBLE_TYPE) {
                return new Double(this.dataIn.readDouble()).toString();
            }
            if (type == MarshallingSupport.BOOLEAN_TYPE) {
                return (this.dataIn.readBoolean() ? Boolean.TRUE : Boolean.FALSE).toString();
            }
            if (type == MarshallingSupport.CHAR_TYPE) {
                return new Character(this.dataIn.readChar()).toString();
            } else {
                this.dataIn.reset();
                throw new OpenwireException(" not a String type");
            }
        } catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            } catch (IOException ioe) {
                throw new OpenwireException(ioe);
            }
            throw mfe;

        } catch (EOFException e) {
            throw new OpenwireException(e);
        } catch (IOException e) {
            throw new OpenwireException(e);
        }
    }

    public int readBytes(byte[] value) throws OpenwireException {

        initializeReading();
        try {
            if (value == null) {
                throw new NullPointerException();
            }

            if (remainingBytes == -1) {
                this.dataIn.mark(value.length + 1);
                int type = this.dataIn.read();
                if (type == -1) {
                    throw new OpenwireException("reached end of data");
                }
                if (type != MarshallingSupport.BYTE_ARRAY_TYPE) {
                    throw new OpenwireException("Not a byte array");
                }
                remainingBytes = this.dataIn.readInt();
            } else if (remainingBytes == 0) {
                remainingBytes = -1;
                return -1;
            }

            if (value.length <= remainingBytes) {
                // small buffer
                remainingBytes -= value.length;
                this.dataIn.readFully(value);
                return value.length;
            } else {
                // big buffer
                int rc = this.dataIn.read(value, 0, remainingBytes);
                remainingBytes = 0;
                return rc;
            }

        } catch (EOFException e) {
            throw new OpenwireException(e.getMessage(),e);
        } catch (IOException e) {
            throw new OpenwireException(e.getMessage(),e);
        }
    }

    public Object readObject() throws OpenwireException {
        initializeReading();
        try {
            this.dataIn.mark(65);
            int type = this.dataIn.read();
            if (type == -1) {
                throw new OpenwireException("reached end of data");
            }
            if (type == MarshallingSupport.NULL) {
                return null;
            }
            if (type == MarshallingSupport.BIG_STRING_TYPE) {
                return MarshallingSupport.readUTF8(dataIn);
            }
            if (type == MarshallingSupport.STRING_TYPE) {
                return this.dataIn.readUTF();
            }
            if (type == MarshallingSupport.LONG_TYPE) {
                return Long.valueOf(this.dataIn.readLong());
            }
            if (type == MarshallingSupport.INTEGER_TYPE) {
                return Integer.valueOf(this.dataIn.readInt());
            }
            if (type == MarshallingSupport.SHORT_TYPE) {
                return Short.valueOf(this.dataIn.readShort());
            }
            if (type == MarshallingSupport.BYTE_TYPE) {
                return Byte.valueOf(this.dataIn.readByte());
            }
            if (type == MarshallingSupport.FLOAT_TYPE) {
                return new Float(this.dataIn.readFloat());
            }
            if (type == MarshallingSupport.DOUBLE_TYPE) {
                return new Double(this.dataIn.readDouble());
            }
            if (type == MarshallingSupport.BOOLEAN_TYPE) {
                return this.dataIn.readBoolean() ? Boolean.TRUE : Boolean.FALSE;
            }
            if (type == MarshallingSupport.CHAR_TYPE) {
                return Character.valueOf(this.dataIn.readChar());
            }
            if (type == MarshallingSupport.BYTE_ARRAY_TYPE) {
                int len = this.dataIn.readInt();
                byte[] value = new byte[len];
                this.dataIn.readFully(value);
                return value;
            } else {
                this.dataIn.reset();
                throw new OpenwireException("unknown type");
            }
        } catch (NumberFormatException mfe) {
            try {
                this.dataIn.reset();
            } catch (IOException ioe) {
                throw new OpenwireException(ioe);
            }
            throw mfe;

        } catch (EOFException e) {
            throw new OpenwireException(e.getMessage(),e);
        } catch (IOException e) {
            throw new OpenwireException(e.getMessage(),e);
        }
    }

    public void writeBoolean(boolean value) throws OpenwireException {
        initializeWriting();
        try {
            MarshallingSupport.marshalBoolean(dataOut, value);
        } catch (IOException ioe) {
            throw new OpenwireException(ioe);
        }
    }

    public void writeByte(byte value) throws OpenwireException {
        initializeWriting();
        try {
            MarshallingSupport.marshalByte(dataOut, value);
        } catch (IOException ioe) {
            throw new OpenwireException(ioe);
        }
    }

    public void writeShort(short value) throws OpenwireException {
        initializeWriting();
        try {
            MarshallingSupport.marshalShort(dataOut, value);
        } catch (IOException ioe) {
            throw new OpenwireException(ioe);
        }
    }

    public void writeChar(char value) throws OpenwireException {
        initializeWriting();
        try {
            MarshallingSupport.marshalChar(dataOut, value);
        } catch (IOException ioe) {
            throw new OpenwireException(ioe);
        }
    }

    public void writeInt(int value) throws OpenwireException {
        initializeWriting();
        try {
            MarshallingSupport.marshalInt(dataOut, value);
        } catch (IOException ioe) {
            throw new OpenwireException(ioe);
        }
    }

    public void writeLong(long value) throws OpenwireException {
        initializeWriting();
        try {
            MarshallingSupport.marshalLong(dataOut, value);
        } catch (IOException ioe) {
            throw new OpenwireException(ioe);
        }
    }

    public void writeFloat(float value) throws OpenwireException {
        initializeWriting();
        try {
            MarshallingSupport.marshalFloat(dataOut, value);
        } catch (IOException ioe) {
            throw new OpenwireException(ioe);
        }
    }

    public void writeDouble(double value) throws OpenwireException {
        initializeWriting();
        try {
            MarshallingSupport.marshalDouble(dataOut, value);
        } catch (IOException ioe) {
            throw new OpenwireException(ioe);
        }
    }

    public void writeString(String value) throws OpenwireException {
        initializeWriting();
        try {
            if (value == null) {
                MarshallingSupport.marshalNull(dataOut);
            } else {
                MarshallingSupport.marshalString(dataOut, value);
            }
        } catch (IOException ioe) {
            throw new OpenwireException(ioe);
        }
    }

    public void writeBytes(byte[] value) throws OpenwireException {
        writeBytes(value, 0, value.length);
    }

    public void writeBytes(byte[] value, int offset, int length) throws OpenwireException {
        initializeWriting();
        try {
            MarshallingSupport.marshalByteArray(dataOut, value, offset, length);
        } catch (IOException ioe) {
            throw new OpenwireException(ioe);
        }
    }

    public void writeObject(Object value) throws OpenwireException {
        initializeWriting();
        if (value == null) {
            try {
                MarshallingSupport.marshalNull(dataOut);
            } catch (IOException ioe) {
                throw new OpenwireException(ioe);
            }
        } else if (value instanceof String) {
            writeString(value.toString());
        } else if (value instanceof Character) {
            writeChar(((Character)value).charValue());
        } else if (value instanceof Boolean) {
            writeBoolean(((Boolean)value).booleanValue());
        } else if (value instanceof Byte) {
            writeByte(((Byte)value).byteValue());
        } else if (value instanceof Short) {
            writeShort(((Short)value).shortValue());
        } else if (value instanceof Integer) {
            writeInt(((Integer)value).intValue());
        } else if (value instanceof Float) {
            writeFloat(((Float)value).floatValue());
        } else if (value instanceof Double) {
            writeDouble(((Double)value).doubleValue());
        } else if (value instanceof byte[]) {
            writeBytes((byte[])value);
        }else if (value instanceof Long) {
            writeLong(((Long)value).longValue());
        }else {
            throw new OpenwireException("Unsupported Object type: " + value.getClass());
        }
    }

    public void reset() throws OpenwireException {
        storeContent();
        this.bytesOut = null;
        this.dataIn = null;
        this.dataOut = null;
        this.remainingBytes = -1;
        setReadOnlyBody(true);
    }

    private void initializeWriting() throws OpenwireException {
        checkReadOnlyBody();
        if (this.dataOut == null) {
            this.bytesOut = new ByteArrayOutputStream();
            OutputStream os = bytesOut;
            if (Settings.enable_compression()) {
                compressed = true;
                os = new DeflaterOutputStream(os);
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
        if (this.dataIn == null) {
            Buffer data = getContent();
            if (data == null) {
                data = new Buffer(new byte[] {}, 0, 0);
            }
            InputStream is = new ByteArrayInputStream(data);
            if (isCompressed()) {
                is = new InflaterInputStream(is);
                is = new BufferedInputStream(is);
            }
            this.dataIn = new DataInputStream(is);
        }
    }

    public String toString() {
        return super.toString() + " ActiveMQStreamMessage{ " + "bytesOut = " + bytesOut + ", dataOut = " + dataOut + ", dataIn = " + dataIn + " }";
    }
}
