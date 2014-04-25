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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import io.fabric8.gateway.handlers.detecting.protocol.openwire.codec.OpenWireFormat;
import io.fabric8.gateway.handlers.detecting.protocol.openwire.support.Settings;
import io.fabric8.gateway.handlers.detecting.protocol.openwire.support.OpenwireException;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.fusesource.hawtbuf.ByteArrayOutputStream;

/**
 * @openwire:marshaller
 */
public class ActiveMQObjectMessage extends ActiveMQMessage {
    
    // TODO: verify classloader
    public static final byte DATA_STRUCTURE_TYPE = CommandTypes.ACTIVEMQ_OBJECT_MESSAGE;
    static final ClassLoader ACTIVEMQ_CLASSLOADER = ActiveMQObjectMessage.class.getClassLoader(); 

    protected transient Serializable object;

    public Message copy() {
        ActiveMQObjectMessage copy = new ActiveMQObjectMessage();
        copy(copy);
        return copy;
    }

    private void copy(ActiveMQObjectMessage copy) {
        storeContent();
        super.copy(copy);
        copy.object = null;
    }

    @Override
    public void beforeMarshall(OpenWireFormat wireFormat) throws IOException {
        super.beforeMarshall(wireFormat);
        // may have initiated on vm transport with deferred marshalling
        storeContent();
    }

    public void storeContent() {
        Buffer bodyAsBytes = getContent();
        if (bodyAsBytes == null && object != null) {
            try {
                ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
                OutputStream os = bytesOut;
                if (Settings.enable_compression()) {
                    compressed = true;
                    os = new DeflaterOutputStream(os);
                }
                DataOutputStream dataOut = new DataOutputStream(os);
                ObjectOutputStream objOut = new ObjectOutputStream(dataOut);
                objOut.writeObject(object);
                objOut.flush();
                objOut.reset();
                objOut.close();
                setContent(bytesOut.toBuffer());
            } catch (IOException ioe) {
                throw new RuntimeException(ioe.getMessage(), ioe);
            }
        }
    }

    public byte getDataStructureType() {
        return DATA_STRUCTURE_TYPE;
    }

    public String getJMSXMimeType() {
        return "jms/object-message";
    }

    /**
     */
    public void clearBody() throws OpenwireException {
        super.clearBody();
        this.object = null;
    }

    public void setObject(Serializable newObject) throws OpenwireException {
        checkReadOnlyBody();
        this.object = newObject;
        setContent(null);
        storeContent();
    }

    /**
     * Gets the serializable object containing this message's data. The default
     * value is null.
     * 
     * @return the serializable object containing this message's data
     * @throws OpenwireException
     */
    public Serializable getObject() throws OpenwireException {
        if (object == null && getContent() != null) {
//            try {
                Buffer content = getContent();
                InputStream is = new ByteArrayInputStream(content);
                if (isCompressed()) {
                    is = new InflaterInputStream(is);
                }
                DataInputStream dataIn = new DataInputStream(is);

                throw new UnsupportedOperationException();
//                ClassLoadingAwareObjectInputStream objIn = new ClassLoadingAwareObjectInputStream(dataIn);
//                try {
//                    object = (Serializable)objIn.readObject();
//                } catch (ClassNotFoundException ce) {
//                    throw new OpenwireException("Failed to build body from content. Serializable class not available to broker. Reason: " + ce, ce);
//                } finally {
//                    dataIn.close();
//                }
//            } catch (IOException e) {
//                throw new OpenwireException("Failed to build body from bytes. Reason: " + e, e);
//            }
        }
        return this.object;
    }

    public void clearMarshalledState() {
        super.clearMarshalledState();
        object = null;
    }

    public void onMessageRolledBack() {
        super.onMessageRolledBack();

        // lets force the object to be deserialized again - as we could have
        // changed the object
        object = null;
    }

    public String toString() {
        try {
            getObject();
        } catch (OpenwireException e) {
        }
        return super.toString();
    }
}
