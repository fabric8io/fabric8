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
import org.jolokia.client.request.J4pExecRequest;
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

    // Attribute assertions
    //-------------------------------------------------------------------------


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
        return assertBigDecimal(value);
    }

    /**
     * Performs an assertion on the <code>Boolean</code> value of an Attribute on an MBean
     */
    public BooleanAssert booleanAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return assertBoolean(value);
    }

    /**
     * Performs an assertion on the <code>Byte</code> value of an Attribute on an MBean
     */
    public ByteAssert byteAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return assertByte(value);
    }

    /**
     * Performs an assertion on the <code>Character</code> value of an Attribute on an MBean
     */
    public CharacterAssert characterAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return assertCharacter(value);
    }

    /**
     * Performs an assertion on the <code>Date</code> value of an Attribute on an MBean
     */
    public DateAssert dateAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return assertDate(value);
    }

    /**
     * Performs an assertion on the <code>Double</code> value of an Attribute on an MBean
     */
    public DoubleAssert doubleAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return assertDouble(value);
    }

    /**
     * Performs an assertion on the <code>Float</code> value of an Attribute on an MBean
     */
    public FloatAssert floatAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return assertFloat(value);
    }

    /**
     * Performs an assertion on the <code>Integer</code> value of an Attribute on an MBean
     */
    public IntegerAssert integerAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return assertInteger(value);
    }

    /**
     * Performs an assertion on the <code>List</code> value of an Attribute on an MBean
     */
    public ListAssert listAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return assertList(value);
    }

    /**
     * Performs an assertion on the <code>Long</code> value of an Attribute on an MBean
     */
    public LongAssert longAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return assertLong(value);
    }

    /**
     * Performs an assertion on the <code>Map</code> value of an Attribute on an MBean
     */
    public MapAssert mapAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return assertMap(value);
    }

    /**
     * Performs an assertion on the <code>Short</code> value of an Attribute on an MBean
     */
    public ShortAssert shortAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return assertShort(value);
    }

    /**
     * Performs an assertion on the <code>String</code> value of an Attribute on an MBean
     */
    public StringAssert stringAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return assertString(value);
    }

    protected Object attributeValue(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        ObjectName objectName = new ObjectName(mbean);
        J4pResponse<J4pReadRequest> results = client.execute(new J4pReadRequest(objectName, attribute));
        return results.getValue();
    }

    // Operation assertions
    //-------------------------------------------------------------------------

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link BigDecimal} result
     * so that assertions can be performed on the resulting {@link BigDecimalAssert}
     */
    public BigDecimalAssert bigDecimalOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return assertBigDecimal(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link Boolean} result
     * so that assertions can be performed on the resulting {@link BooleanAssert}
     */
    public BooleanAssert booleanOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return assertBoolean(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link Byte} result
     * so that assertions can be performed on the resulting {@link ByteAssert}
     */
    public ByteAssert byteOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return assertByte(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link Character} result
     * so that assertions can be performed on the resulting {@link CharacterAssert}
     */
    public CharacterAssert characterOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return assertCharacter(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link Date} result
     * so that assertions can be performed on the resulting {@link DateAssert}
     */
    public DateAssert dateOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return assertDate(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link Double} result
     * so that assertions can be performed on the resulting {@link DoubleAssert}
     */
    public DoubleAssert doubleOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return assertDouble(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link Float} result
     * so that assertions can be performed on the resulting {@link FloatAssert}
     */
    public FloatAssert floatOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return assertFloat(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link Integer} result
     * so that assertions can be performed on the resulting {@link IntegerAssert}
     */
    public IntegerAssert integerOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return assertInteger(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link List} result
     * so that assertions can be performed on the resulting {@link ListAssert}
     */
    public ListAssert listOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return assertList(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link Long} result
     * so that assertions can be performed on the resulting {@link LongAssert}
     */
    public LongAssert longOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return assertLong(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link Map} result
     * so that assertions can be performed on the resulting {@link MapAssert}
     */
    public MapAssert mapOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return assertMap(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link Short} result
     * so that assertions can be performed on the resulting {@link ShortAssert}
     */
    public ShortAssert shortOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return assertShort(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link String} result
     * so that assertions can be performed on the resulting {@link StringAssert}
     */
    public StringAssert stringOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return assertString(value);
    }


    /**
     * Performs an assertion on the value of an Attribute on an MBean
     */
    public ObjectAssert<?> operation(String mbean, String operation, Object... arguments) throws J4pException, MalformedObjectNameException {
        Object value = operationResult(mbean, operation, arguments);
        return (ObjectAssert<?>) assertThat(value);
    }


    protected Object operationResult(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        ObjectName objectName = new ObjectName(mbean);
        J4pResponse<J4pExecRequest> results = client.execute(new J4pExecRequest(objectName, operation, arguments));
        return results.getValue();
    }


    protected static <T> T asInstanceOf(Object value, Class<T> clazz) {
        assertThat(value).isInstanceOf(clazz);
        return clazz.cast(value);
    }

    protected static BigDecimalAssert assertBigDecimal(Object value) {
        BigDecimal typedValue = asInstanceOf(value, BigDecimal.class);
        return (BigDecimalAssert) assertThat(typedValue);
    }

    protected static BooleanAssert assertBoolean(Object value) {
        Boolean typedValue = asInstanceOf(value, Boolean.class);
        return (BooleanAssert) assertThat(typedValue);
    }

    protected static ByteAssert assertByte(Object value) {
        Byte typedValue = asInstanceOf(value, Byte.class);
        return (ByteAssert) assertThat(typedValue);
    }

    protected static CharacterAssert assertCharacter(Object value) {
        Character typedValue = asInstanceOf(value, Character.class);
        return (CharacterAssert) assertThat(typedValue);
    }

    protected static DateAssert assertDate(Object value) {
        Date typedValue = asInstanceOf(value, Date.class);
        return (DateAssert) assertThat(typedValue);
    }

    protected static DoubleAssert assertDouble(Object value) {
        Double typedValue = asInstanceOf(value, Double.class);
        return (DoubleAssert) assertThat(typedValue);
    }

    protected static FloatAssert assertFloat(Object value) {
        Float typedValue = asInstanceOf(value, Float.class);
        return (FloatAssert) assertThat(typedValue);
    }

    protected static IntegerAssert assertInteger(Object value) {
        Integer typedValue = asInstanceOf(value, Integer.class);
        return (IntegerAssert) assertThat(typedValue);
    }

    protected static ListAssert assertList(Object value) {
        List typedValue = asInstanceOf(value, List.class);
        return (ListAssert) assertThat(typedValue);
    }

    protected static LongAssert assertLong(Object value) {
        Long typedValue = asInstanceOf(value, Long.class);
        return (LongAssert) assertThat(typedValue);
    }

    protected static MapAssert assertMap(Object value) {
        Map typedValue = asInstanceOf(value, Map.class);
        return (MapAssert) assertThat(typedValue);
    }

    protected static ShortAssert assertShort(Object value) {
        Short typedValue = asInstanceOf(value, Short.class);
        return (ShortAssert) assertThat(typedValue);
    }

    protected static StringAssert assertString(Object value) {
        String typedValue = asInstanceOf(value, String.class);
        return (StringAssert) assertThat(typedValue);
    }



}
