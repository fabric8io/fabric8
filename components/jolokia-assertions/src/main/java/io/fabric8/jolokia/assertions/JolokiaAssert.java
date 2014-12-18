/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.jolokia.assertions;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.BigDecimalAssert;
import org.assertj.core.api.BooleanAssert;
import org.assertj.core.api.ByteAssert;
import org.assertj.core.api.CharacterAssert;
import org.assertj.core.api.DateAssert;
import org.assertj.core.api.DoubleAssert;
import org.assertj.core.api.FloatAssert;
import org.assertj.core.api.IntegerAssert;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.LongAssert;
import org.assertj.core.api.MapAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.api.ShortAssert;
import org.assertj.core.api.StringAssert;
import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pResponse;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * An assert class for making assertions about JMX attributes and options via <a href="http://jolokia.org/">jolokia</a>
 * using the <a href="http://joel-costigliola.github.io/assertj">assertj library</a>
 */
public class JolokiaAssert extends AbstractAssert<JolokiaAssert, J4pClient> {
    private final J4pClient client;

    public JolokiaAssert(J4pClient client) {
        super(client, JolokiaAssert.class);
        this.client = client;
    }

    protected static <T> T asInstanceOf(Object value, Class<T> clazz) {
        assertThat(value).isInstanceOf(clazz);
        return clazz.cast(value);
    }

    /**
     * Performs an assertion on the value of an Attribute of a given Class on an MBean
     */
    public <T> ObjectAssert<T> attribute(String mbean, String attribute, Class<T> clazz) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return (ObjectAssert<T>) assertThat(value).isNotNull().isInstanceOf(clazz);
    }

    /**
     * Performs an assertion on the value of an Attribute on an MBean
     */
    public ObjectAssert<?> attribute(String mbean, String attribute) throws J4pException, MalformedObjectNameException {
        Object value = attributeValue(mbean, attribute);
        return (ObjectAssert<?>) assertThat(value);
    }

    /**
     * Performs an assertion on the <code>BigDecimal</code> value of an Attribute on an MBean
     */
    public BigDecimalAssert bigDecimalAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        BigDecimal typedValue = asInstanceOf(value, BigDecimal.class);
        return (BigDecimalAssert) assertThat(typedValue);
    }

    /**
     * Performs an assertion on the <code>Boolean</code> value of an Attribute on an MBean
     */
    public BooleanAssert booleanAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        Boolean typedValue = asInstanceOf(value, Boolean.class);
        return (BooleanAssert) assertThat(typedValue);
    }

    /**
     * Performs an assertion on the <code>Byte</code> value of an Attribute on an MBean
     */
    public ByteAssert byteAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        Byte typedValue = asInstanceOf(value, Byte.class);
        return (ByteAssert) assertThat(typedValue);
    }

    /**
     * Performs an assertion on the <code>Character</code> value of an Attribute on an MBean
     */
    public CharacterAssert characterAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        Character typedValue = asInstanceOf(value, Character.class);
        return (CharacterAssert) assertThat(typedValue);
    }

    /**
     * Performs an assertion on the <code>Date</code> value of an Attribute on an MBean
     */
    public DateAssert dateAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        Date typedValue = asInstanceOf(value, Date.class);
        return (DateAssert) assertThat(typedValue);
    }

    /**
     * Performs an assertion on the <code>Double</code> value of an Attribute on an MBean
     */
    public DoubleAssert doubleAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        Double typedValue = asInstanceOf(value, Double.class);
        return (DoubleAssert) assertThat(typedValue);
    }

    /**
     * Performs an assertion on the <code>Float</code> value of an Attribute on an MBean
     */
    public FloatAssert floatAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        Float typedValue = asInstanceOf(value, Float.class);
        return (FloatAssert) assertThat(typedValue);
    }

    /**
     * Performs an assertion on the <code>Integer</code> value of an Attribute on an MBean
     */
    public IntegerAssert integerAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        Integer typedValue = asInstanceOf(value, Integer.class);
        return (IntegerAssert) assertThat(typedValue);
    }

    /**
     * Performs an assertion on the <code>List</code> value of an Attribute on an MBean
     */
    public ListAssert listAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        List typedValue = asInstanceOf(value, List.class);
        return (ListAssert) assertThat(typedValue);
    }

    /**
     * Performs an assertion on the <code>Long</code> value of an Attribute on an MBean
     */
    public LongAssert longAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        Long typedValue = asInstanceOf(value, Long.class);
        return (LongAssert) assertThat(typedValue);
    }

    /**
     * Performs an assertion on the <code>Map</code> value of an Attribute on an MBean
     */
    public MapAssert mapAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        Map typedValue = asInstanceOf(value, Map.class);
        return (MapAssert) assertThat(typedValue);
    }

    /**
     * Performs an assertion on the <code>Short</code> value of an Attribute on an MBean
     */
    public ShortAssert shortAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        Short typedValue = asInstanceOf(value, Short.class);
        return (ShortAssert) assertThat(typedValue);
    }

    /**
     * Performs an assertion on the <code>String</code> value of an Attribute on an MBean
     */
    public StringAssert stringAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        String typedValue = asInstanceOf(value, String.class);
        return (StringAssert) assertThat(typedValue);
    }


    protected Object attributeValue(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        ObjectName objectName = new ObjectName(mbean);
        J4pResponse<J4pReadRequest> results = client.execute(new J4pReadRequest(objectName, attribute));
        return results.getValue();
    }

}
