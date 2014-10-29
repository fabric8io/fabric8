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

import io.fabric8.gateway.handlers.detecting.protocol.openwire.codec.OpenWireFormat;
import io.fabric8.gateway.handlers.detecting.protocol.openwire.support.MarshallingSupport;
import io.fabric8.gateway.handlers.detecting.protocol.openwire.support.OpenwireException;
import io.fabric8.gateway.handlers.detecting.protocol.openwire.support.Settings;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.fusesource.hawtbuf.ByteArrayOutputStream;

import java.io.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * @openwire:marshaller
 */
public class ActiveMQMapMessage extends ActiveMQMessage {

    public static final byte DATA_STRUCTURE_TYPE = CommandTypes.ACTIVEMQ_MAP_MESSAGE;

    protected transient Map<String, Object> map = new HashMap<String, Object>();

    private Object readResolve() throws ObjectStreamException {
        if(this.map == null) {
            this.map = new HashMap<String, Object>();
        }
        return this;
    }

    public Message copy() {
        ActiveMQMapMessage copy = new ActiveMQMapMessage();
        copy(copy);
        return copy;
    }

    private void copy(ActiveMQMapMessage copy) {
        storeContent();
        super.copy(copy);
    }

    // We only need to marshal the content if we are hitting the wire.
    public void beforeMarshall(OpenWireFormat wireFormat) throws IOException {
        super.beforeMarshall(wireFormat);
        storeContent();
    }

    public void clearMarshalledState() {
        super.clearMarshalledState();
        map.clear();
    }

    private void storeContent() {
        try {
            if (getContent() == null && !map.isEmpty()) {
                ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
                OutputStream os = bytesOut;
                if (Settings.enable_compression()) {
                    compressed = true;
                    os = new DeflaterOutputStream(os);
                }
                DataOutputStream dataOut = new DataOutputStream(os);
                MarshallingSupport.marshalPrimitiveMap(map, dataOut);
                dataOut.close();
                setContent(bytesOut.toBuffer());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Builds the message body from data
     * 
     * @throws OpenwireException
     */
    private void loadContent() throws OpenwireException {
        try {
            if (getContent() != null && map.isEmpty()) {
                Buffer content = getContent();
                InputStream is = new ByteArrayInputStream(content);
                if (isCompressed()) {
                    is = new InflaterInputStream(is);
                }
                DataInputStream dataIn = new DataInputStream(is);
                map = MarshallingSupport.unmarshalPrimitiveMap(dataIn);
                dataIn.close();
            }
        } catch (IOException e) {
            throw new OpenwireException(e);
        }
    }

    public byte getDataStructureType() {
        return DATA_STRUCTURE_TYPE;
    }

    public String getJMSXMimeType() {
        return "jms/map-message";
    }

    public void clearBody() throws OpenwireException {
        super.clearBody();
        map.clear();
    }

    public boolean getBoolean(String name) throws OpenwireException {
        initializeReading();
        Object value = map.get(name);
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return ((Boolean)value).booleanValue();
        }
        if (value instanceof String) {
            return Boolean.valueOf(value.toString()).booleanValue();
        } else {
            throw new OpenwireException(" cannot read a boolean from " + value.getClass().getName());
        }
    }

    public byte getByte(String name) throws OpenwireException {
        initializeReading();
        Object value = map.get(name);
        if (value == null) {
            return 0;
        }
        if (value instanceof Byte) {
            return ((Byte)value).byteValue();
        }
        if (value instanceof String) {
            return Byte.valueOf(value.toString()).byteValue();
        } else {
            throw new OpenwireException(" cannot read a byte from " + value.getClass().getName());
        }
    }

    public short getShort(String name) throws OpenwireException {
        initializeReading();
        Object value = map.get(name);
        if (value == null) {
            return 0;
        }
        if (value instanceof Short) {
            return ((Short)value).shortValue();
        }
        if (value instanceof Byte) {
            return ((Byte)value).shortValue();
        }
        if (value instanceof String) {
            return Short.valueOf(value.toString()).shortValue();
        } else {
            throw new OpenwireException(" cannot read a short from " + value.getClass().getName());
        }
    }

    public char getChar(String name) throws OpenwireException {
        initializeReading();
        Object value = map.get(name);
        if (value == null) {
            throw new NullPointerException();
        }
        if (value instanceof Character) {
            return ((Character)value).charValue();
        } else {
            throw new OpenwireException(" cannot read a short from " + value.getClass().getName());
        }
    }

    public int getInt(String name) throws OpenwireException {
        initializeReading();
        Object value = map.get(name);
        if (value == null) {
            return 0;
        }
        if (value instanceof Integer) {
            return ((Integer)value).intValue();
        }
        if (value instanceof Short) {
            return ((Short)value).intValue();
        }
        if (value instanceof Byte) {
            return ((Byte)value).intValue();
        }
        if (value instanceof String) {
            return Integer.valueOf(value.toString()).intValue();
        } else {
            throw new OpenwireException(" cannot read an int from " + value.getClass().getName());
        }
    }

    public long getLong(String name) throws OpenwireException {
        initializeReading();
        Object value = map.get(name);
        if (value == null) {
            return 0;
        }
        if (value instanceof Long) {
            return ((Long)value).longValue();
        }
        if (value instanceof Integer) {
            return ((Integer)value).longValue();
        }
        if (value instanceof Short) {
            return ((Short)value).longValue();
        }
        if (value instanceof Byte) {
            return ((Byte)value).longValue();
        }
        if (value instanceof String) {
            return Long.valueOf(value.toString()).longValue();
        } else {
            throw new OpenwireException(" cannot read a long from " + value.getClass().getName());
        }
    }

    public float getFloat(String name) throws OpenwireException {
        initializeReading();
        Object value = map.get(name);
        if (value == null) {
            return 0;
        }
        if (value instanceof Float) {
            return ((Float)value).floatValue();
        }
        if (value instanceof String) {
            return Float.valueOf(value.toString()).floatValue();
        } else {
            throw new OpenwireException(" cannot read a float from " + value.getClass().getName());
        }
    }

    public double getDouble(String name) throws OpenwireException {
        initializeReading();
        Object value = map.get(name);
        if (value == null) {
            return 0;
        }
        if (value instanceof Double) {
            return ((Double)value).doubleValue();
        }
        if (value instanceof Float) {
            return ((Float)value).floatValue();
        }
        if (value instanceof String) {
            return Float.valueOf(value.toString()).floatValue();
        } else {
            throw new OpenwireException(" cannot read a double from " + value.getClass().getName());
        }
    }

    public String getString(String name) throws OpenwireException {
        initializeReading();
        Object value = map.get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof byte[]) {
            throw new OpenwireException("Use getBytes to read a byte array");
        } else {
            return value.toString();
        }
    }

    public byte[] getBytes(String name) throws OpenwireException {
        initializeReading();
        Object value = map.get(name);
        if (value instanceof byte[]) {
            return (byte[])value;
        } else {
            throw new OpenwireException(" cannot read a byte[] from " + value.getClass().getName());
        }
    }

    public Object getObject(String name) throws OpenwireException {
        initializeReading();
        return map.get(name);
    }

    public Enumeration<String> getMapNames() throws OpenwireException {
        initializeReading();
        return Collections.enumeration(map.keySet());
    }

    protected void put(String name, Object value) throws OpenwireException {
        if (name == null) {
            throw new IllegalArgumentException("The name of the property cannot be null.");
        }
        if (name.length() == 0) {
            throw new IllegalArgumentException("The name of the property cannot be an emprty string.");
        }
        map.put(name, value);
    }

    public void setBoolean(String name, boolean value) throws OpenwireException {
        initializeWriting();
        put(name, value ? Boolean.TRUE : Boolean.FALSE);
    }

    public void setByte(String name, byte value) throws OpenwireException {
        initializeWriting();
        put(name, Byte.valueOf(value));
    }

    public void setShort(String name, short value) throws OpenwireException {
        initializeWriting();
        put(name, Short.valueOf(value));
    }

    public void setChar(String name, char value) throws OpenwireException {
        initializeWriting();
        put(name, Character.valueOf(value));
    }

    public void setInt(String name, int value) throws OpenwireException {
        initializeWriting();
        put(name, Integer.valueOf(value));
    }

    public void setLong(String name, long value) throws OpenwireException {
        initializeWriting();
        put(name, Long.valueOf(value));
    }

    public void setFloat(String name, float value) throws OpenwireException {
        initializeWriting();
        put(name, new Float(value));
    }

    public void setDouble(String name, double value) throws OpenwireException {
        initializeWriting();
        put(name, new Double(value));
    }

    public void setString(String name, String value) throws OpenwireException {
        initializeWriting();
        put(name, value);
    }

    public void setBytes(String name, byte[] value) throws OpenwireException {
        initializeWriting();
        if (value != null) {
            put(name, value);
        } else {
            map.remove(name);
        }
    }

    public void setBytes(String name, byte[] value, int offset, int length) throws OpenwireException {
        initializeWriting();
        byte[] data = new byte[length];
        System.arraycopy(value, offset, data, 0, length);
        put(name, data);
    }

    public void setObject(String name, Object value) throws OpenwireException {
        initializeWriting();
        if (value != null) {
            // byte[] not allowed on properties
            if (!(value instanceof byte[])) {
                checkValidObject(value);
            }
            put(name, value);
        } else {
            put(name, null);
        }
    }

    public boolean itemExists(String name) throws OpenwireException {
        initializeReading();
        return map.containsKey(name);
    }

    private void initializeReading() throws OpenwireException {
        loadContent();
    }

    private void initializeWriting() throws OpenwireException {
        checkReadOnlyBody();
        setContent(null);
    }

    public String toString() {
        return super.toString() + " ActiveMQMapMessage{ " + "theTable = " + map + " }";
    }

    public Map<String, Object> getContentMap() throws OpenwireException {
        initializeReading();
        return map;
    }
}
