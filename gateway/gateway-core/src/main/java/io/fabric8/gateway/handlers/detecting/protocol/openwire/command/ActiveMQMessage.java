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
import io.fabric8.gateway.handlers.detecting.protocol.openwire.support.TypeConversionSupport;
import static org.fusesource.hawtbuf.Buffer.*;

import java.io.IOException;
import java.util.*;

/**
 * @version $Revision:$
 * @openwire:marshaller code="23"
 */
public class ActiveMQMessage extends Message {

    public static final byte DATA_STRUCTURE_TYPE = CommandTypes.ACTIVEMQ_MESSAGE;
    private static final Map<String, PropertySetter> JMS_PROPERTY_SETERS = new HashMap<String, PropertySetter>();

    public byte getDataStructureType() {
        return DATA_STRUCTURE_TYPE;
    }

    public Message copy() {
        ActiveMQMessage copy = new ActiveMQMessage();
        copy(copy);
        return copy;
    }

    protected void copy(ActiveMQMessage copy) {
        super.copy(copy);
    }

    public int hashCode() {
        MessageId id = getMessageId();
        if (id != null) {
            return id.hashCode();
        } else {
            return super.hashCode();
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || o.getClass() != getClass()) {
            return false;
        }

        ActiveMQMessage msg = (ActiveMQMessage) o;
        MessageId oMsg = msg.getMessageId();
        MessageId thisMsg = this.getMessageId();
        return thisMsg != null && oMsg != null && oMsg.equals(thisMsg);
    }

    public void clearBody() throws OpenwireException {
        setContent(null);
        readOnlyBody = false;
    }

    public void clearProperties() {
        super.clearProperties();
        readOnlyProperties = false;
    }

    public boolean propertyExists(String name) throws OpenwireException {
        try {
            return this.getProperties().containsKey(name);
        } catch (IOException e) {
            throw new OpenwireException(e);
        }
    }

    public Enumeration getPropertyNames() throws OpenwireException {
        try {
            return new Vector<String>(this.getProperties().keySet()).elements();
        } catch (IOException e) {
            throw new OpenwireException(e);
        }
    }

    interface PropertySetter {

        void set(Message message, Object value) throws OpenwireException;
    }

    static {
        JMS_PROPERTY_SETERS.put("JMSXDeliveryCount", new PropertySetter() {
            public void set(Message message, Object value) throws OpenwireException {
                Integer rc = (Integer) TypeConversionSupport.convert(value, Integer.class);
                if (rc == null) {
                    throw new OpenwireException("Property JMSXDeliveryCount cannot be set from a " + value.getClass().getName() + ".");
                }
                message.setRedeliveryCounter(rc.intValue() - 1);
            }
        });
        JMS_PROPERTY_SETERS.put("JMSXGroupID", new PropertySetter() {
            public void set(Message message, Object value) throws OpenwireException {
                String rc = (String) TypeConversionSupport.convert(value, String.class);
                if (rc == null) {
                    throw new OpenwireException("Property JMSXGroupID cannot be set from a " + value.getClass().getName() + ".");
                }
                message.setGroupID(utf8(rc));
            }
        });
        JMS_PROPERTY_SETERS.put("JMSXGroupSeq", new PropertySetter() {
            public void set(Message message, Object value) throws OpenwireException {
                Integer rc = (Integer) TypeConversionSupport.convert(value, Integer.class);
                if (rc == null) {
                    throw new OpenwireException("Property JMSXGroupSeq cannot be set from a " + value.getClass().getName() + ".");
                }
                message.setGroupSequence(rc.intValue());
            }
        });
        JMS_PROPERTY_SETERS.put("JMSCorrelationID", new PropertySetter() {
            public void set(Message message, Object value) throws OpenwireException {
                String rc = (String) TypeConversionSupport.convert(value, String.class);
                if (rc == null) {
                    throw new OpenwireException("Property JMSCorrelationID cannot be set from a " + value.getClass().getName() + ".");
                }
                message.setCorrelationId(utf8(rc));
            }
        });
        JMS_PROPERTY_SETERS.put("JMSDeliveryMode", new PropertySetter() {
            public void set(Message message, Object value) throws OpenwireException {
                Integer rc = (Integer) TypeConversionSupport.convert(value, Integer.class);
                if (rc == null) {
                    Boolean bool = (Boolean) TypeConversionSupport.convert(value, Boolean.class);
                    if (bool == null) {
                        throw new OpenwireException("Property JMSDeliveryMode cannot be set from a " + value.getClass().getName() + ".");
                    } else {
                        rc = bool.booleanValue() ? 2 : 1;
                    }
                }
                message.setPersistent(rc.intValue()==2);
            }
        });
        JMS_PROPERTY_SETERS.put("JMSExpiration", new PropertySetter() {
            public void set(Message message, Object value) throws OpenwireException {
                Long rc = (Long) TypeConversionSupport.convert(value, Long.class);
                if (rc == null) {
                    throw new OpenwireException("Property JMSExpiration cannot be set from a " + value.getClass().getName() + ".");
                }
                message.setExpiration(rc.longValue());
            }
        });
        JMS_PROPERTY_SETERS.put("JMSPriority", new PropertySetter() {
            public void set(Message message, Object value) throws OpenwireException {
                Integer rc = (Integer) TypeConversionSupport.convert(value, Integer.class);
                if (rc == null) {
                    throw new OpenwireException("Property JMSPriority cannot be set from a " + value.getClass().getName() + ".");
                }
                message.setPriority((byte) rc.intValue());
            }
        });
        JMS_PROPERTY_SETERS.put("JMSRedelivered", new PropertySetter() {
            public void set(Message message, Object value) throws OpenwireException {
                Boolean rc = (Boolean) TypeConversionSupport.convert(value, Boolean.class);
                if (rc == null) {
                    throw new OpenwireException("Property JMSRedelivered cannot be set from a " + value.getClass().getName() + ".");
                }
                message.setRedelivered(rc.booleanValue());
            }
        });
        JMS_PROPERTY_SETERS.put("JMSReplyTo", new PropertySetter() {
            public void set(Message message, Object value) throws OpenwireException {
                ActiveMQDestination rc = (ActiveMQDestination) TypeConversionSupport.convert(value, ActiveMQDestination.class);
                if (rc == null) {
                    throw new OpenwireException("Property JMSReplyTo cannot be set from a " + value.getClass().getName() + ".");
                }
                ((ActiveMQMessage) message).setReplyTo(rc);
            }
        });
        JMS_PROPERTY_SETERS.put("JMSTimestamp", new PropertySetter() {
            public void set(Message message, Object value) throws OpenwireException {
                Long rc = (Long) TypeConversionSupport.convert(value, Long.class);
                if (rc == null) {
                    throw new OpenwireException("Property JMSTimestamp cannot be set from a " + value.getClass().getName() + ".");
                }
                message.setTimestamp(rc.longValue());
            }
        });
        JMS_PROPERTY_SETERS.put("JMSType", new PropertySetter() {
            public void set(Message message, Object value) throws OpenwireException {
                String rc = (String) TypeConversionSupport.convert(value, String.class);
                if (rc == null) {
                    throw new OpenwireException("Property JMSType cannot be set from a " + value.getClass().getName() + ".");
                }
                message.setType(utf8(rc));
            }
        });
    }

    public void setObjectProperty(String name, Object value) throws OpenwireException {
        setObjectProperty(name, value, true);
    }

    public void setObjectProperty(String name, Object value, boolean checkReadOnly) throws OpenwireException {

        if (checkReadOnly) {
            checkReadOnlyProperties();
        }
        if (name == null || name.equals("")) {
            throw new IllegalArgumentException("Property name cannot be empty or null");
        }

        checkValidObject(value);
        PropertySetter setter = JMS_PROPERTY_SETERS.get(name);

        if (setter != null && value != null) {
            setter.set(this, value);
        } else {
            try {
                this.setProperty(name, value);
            } catch (IOException e) {
                throw new OpenwireException(e);
            }
        }
    }

    protected void checkValidObject(Object value) throws OpenwireException {

        boolean valid = value instanceof Boolean || value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long;
        valid = valid || value instanceof Float || value instanceof Double || value instanceof Character || value instanceof String || value == null;

        if (!valid) {

            if (Settings.enable_nested_map_and_list()) {
                if (!(value instanceof Map || value instanceof List)) {
                    throw new OpenwireException("Only objectified primitive objects, String, Map and List types are allowed but was: " + value + " type: " + value.getClass());
                }
            } else {
                throw new OpenwireException("Only objectified primitive objects and String types are allowed but was: " + value + " type: " + value.getClass());
            }
        }
    }

    public Object getObjectProperty(String name) throws OpenwireException {
        if (name == null) {
            throw new NullPointerException("Property name cannot be null");
        }
        throw new UnsupportedOperationException();
        // return createFilterable().getProperty(name);
    }

    public boolean getBooleanProperty(String name) throws OpenwireException {
        Object value = getObjectProperty(name);
        if (value == null) {
            return false;
        }
        Boolean rc = (Boolean) TypeConversionSupport.convert(value, Boolean.class);
        if (rc == null) {
            throw new OpenwireException("Property " + name + " was a " + value.getClass().getName() + " and cannot be read as a boolean");
        }
        return rc.booleanValue();
    }

    public byte getByteProperty(String name) throws OpenwireException {
        Object value = getObjectProperty(name);
        if (value == null) {
            throw new NumberFormatException("property " + name + " was null");
        }
        Byte rc = (Byte) TypeConversionSupport.convert(value, Byte.class);
        if (rc == null) {
            throw new OpenwireException("Property " + name + " was a " + value.getClass().getName() + " and cannot be read as a byte");
        }
        return rc.byteValue();
    }

    public short getShortProperty(String name) throws OpenwireException {
        Object value = getObjectProperty(name);
        if (value == null) {
            throw new NumberFormatException("property " + name + " was null");
        }
        Short rc = (Short) TypeConversionSupport.convert(value, Short.class);
        if (rc == null) {
            throw new OpenwireException("Property " + name + " was a " + value.getClass().getName() + " and cannot be read as a short");
        }
        return rc.shortValue();
    }

    public int getIntProperty(String name) throws OpenwireException {
        Object value = getObjectProperty(name);
        if (value == null) {
            throw new NumberFormatException("property " + name + " was null");
        }
        Integer rc = (Integer) TypeConversionSupport.convert(value, Integer.class);
        if (rc == null) {
            throw new OpenwireException("Property " + name + " was a " + value.getClass().getName() + " and cannot be read as an integer");
        }
        return rc.intValue();
    }

    public long getLongProperty(String name) throws OpenwireException {
        Object value = getObjectProperty(name);
        if (value == null) {
            throw new NumberFormatException("property " + name + " was null");
        }
        Long rc = (Long) TypeConversionSupport.convert(value, Long.class);
        if (rc == null) {
            throw new OpenwireException("Property " + name + " was a " + value.getClass().getName() + " and cannot be read as a long");
        }
        return rc.longValue();
    }

    public float getFloatProperty(String name) throws OpenwireException {
        Object value = getObjectProperty(name);
        if (value == null) {
            throw new NullPointerException("property " + name + " was null");
        }
        Float rc = (Float) TypeConversionSupport.convert(value, Float.class);
        if (rc == null) {
            throw new OpenwireException("Property " + name + " was a " + value.getClass().getName() + " and cannot be read as a float");
        }
        return rc.floatValue();
    }

    public double getDoubleProperty(String name) throws OpenwireException {
        Object value = getObjectProperty(name);
        if (value == null) {
            throw new NullPointerException("property " + name + " was null");
        }
        Double rc = (Double) TypeConversionSupport.convert(value, Double.class);
        if (rc == null) {
            throw new OpenwireException("Property " + name + " was a " + value.getClass().getName() + " and cannot be read as a double");
        }
        return rc.doubleValue();
    }

    public String getStringProperty(String name) throws OpenwireException {
        Object value = null;
        if (name.equals("JMSXUserID")) {
            value = getUserID();
            if (value == null) {
                value = getObjectProperty(name);
            }
        } else {
            value = getObjectProperty(name);
        }
        if (value == null) {
            return null;
        }
        String rc = (String) TypeConversionSupport.convert(value, String.class);
        if (rc == null) {
            throw new OpenwireException("Property " + name + " was a " + value.getClass().getName() + " and cannot be read as a String");
        }
        return rc;
    }

    public void setBooleanProperty(String name, boolean value) throws OpenwireException {
        setBooleanProperty(name, value, true);
    }

    public void setBooleanProperty(String name, boolean value, boolean checkReadOnly) throws OpenwireException {
        setObjectProperty(name, Boolean.valueOf(value), checkReadOnly);
    }

    public void setByteProperty(String name, byte value) throws OpenwireException {
        setObjectProperty(name, Byte.valueOf(value));
    }

    public void setShortProperty(String name, short value) throws OpenwireException {
        setObjectProperty(name, Short.valueOf(value));
    }

    public void setIntProperty(String name, int value) throws OpenwireException {
        setObjectProperty(name, Integer.valueOf(value));
    }

    public void setLongProperty(String name, long value) throws OpenwireException {
        setObjectProperty(name, Long.valueOf(value));
    }

    public void setFloatProperty(String name, float value) throws OpenwireException {
        setObjectProperty(name, new Float(value));
    }

    public void setDoubleProperty(String name, double value) throws OpenwireException {
        setObjectProperty(name, new Double(value));
    }

    public void setStringProperty(String name, String value) throws OpenwireException {
        setObjectProperty(name, value);
    }

    private void checkReadOnlyProperties() throws OpenwireException {
        if (readOnlyProperties) {
            throw new OpenwireException("Message properties are read-only");
        }
    }

    protected void checkReadOnlyBody() throws OpenwireException {
        if (readOnlyBody) {
            throw new OpenwireException("Message body is read-only");
        }
    }

    public boolean isExpired() {
        long expireTime = this.getExpiration();
        if (expireTime > 0 && System.currentTimeMillis() > expireTime) {
            return true;
        }
        return false;
    }

    /**
     * Send operation event listener. Used to get the message ready to be sent.
     */
    public void onSend() throws OpenwireException {
        setReadOnlyBody(true);
        setReadOnlyProperties(true);
    }

}
