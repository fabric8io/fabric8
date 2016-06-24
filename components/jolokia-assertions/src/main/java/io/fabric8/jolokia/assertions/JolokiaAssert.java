/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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
        return Assertions.assertBigDecimal(value);
    }

    /**
     * Performs an assertion on the <code>Boolean</code> value of an Attribute on an MBean
     */
    public BooleanAssert booleanAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return Assertions.assertBoolean(value);
    }

    /**
     * Performs an assertion on the <code>Byte</code> value of an Attribute on an MBean
     */
    public ByteAssert byteAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return Assertions.assertByte(value);
    }

    /**
     * Performs an assertion on the <code>Character</code> value of an Attribute on an MBean
     */
    public CharacterAssert characterAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return Assertions.assertCharacter(value);
    }

    /**
     * Performs an assertion on the <code>Date</code> value of an Attribute on an MBean
     */
    public DateAssert dateAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return Assertions.assertDate(value);
    }

    /**
     * Performs an assertion on the <code>Double</code> value of an Attribute on an MBean
     */
    public DoubleAssert doubleAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return Assertions.assertDouble(value);
    }

    /**
     * Performs an assertion on the <code>Float</code> value of an Attribute on an MBean
     */
    public FloatAssert floatAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return Assertions.assertFloat(value);
    }

    /**
     * Performs an assertion on the <code>Integer</code> value of an Attribute on an MBean
     */
    public IntegerAssert integerAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return Assertions.assertInteger(value);
    }

    /**
     * Performs an assertion on the <code>JSONArray</code> value of an Attribute on an MBean
     */
    public JSONArrayAssert jsonArrayAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return Assertions.assertJSONArray(value);
    }

    /**
     * Performs an assertion on the <code>JSONObject</code> value of an Attribute on an MBean
     */
    public JSONObjectAssert jsonObjectAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return Assertions.assertJSONObject(value);
    }

    /**
     * Performs an assertion on the <code>List</code> value of an Attribute on an MBean
     */
    public ListAssert listAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return Assertions.assertList(value);
    }

    /**
     * Performs an assertion on the <code>Long</code> value of an Attribute on an MBean
     */
    public LongAssert longAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return Assertions.assertLong(value);
    }

    /**
     * Performs an assertion on the <code>Map</code> value of an Attribute on an MBean
     */
    public MapAssert mapAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return Assertions.assertMap(value);
    }

    /**
     * Performs an assertion on the <code>Short</code> value of an Attribute on an MBean
     */
    public ShortAssert shortAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return Assertions.assertShort(value);
    }

    /**
     * Performs an assertion on the <code>String</code> value of an Attribute on an MBean
     */
    public StringAssert stringAttribute(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
        Object value = attributeValue(mbean, attribute);
        return Assertions.assertString(value);
    }

    /**
     * Returns the attribute value of the given mbean and attribute name
     */
    public Object attributeValue(String mbean, String attribute) throws MalformedObjectNameException, J4pException {
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
        return Assertions.assertBigDecimal(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link Boolean} result
     * so that assertions can be performed on the resulting {@link BooleanAssert}
     */
    public BooleanAssert booleanOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return Assertions.assertBoolean(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link Byte} result
     * so that assertions can be performed on the resulting {@link ByteAssert}
     */
    public ByteAssert byteOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return Assertions.assertByte(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link Character} result
     * so that assertions can be performed on the resulting {@link CharacterAssert}
     */
    public CharacterAssert characterOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return Assertions.assertCharacter(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link Date} result
     * so that assertions can be performed on the resulting {@link DateAssert}
     */
    public DateAssert dateOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return Assertions.assertDate(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link Double} result
     * so that assertions can be performed on the resulting {@link DoubleAssert}
     */
    public DoubleAssert doubleOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return Assertions.assertDouble(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link Float} result
     * so that assertions can be performed on the resulting {@link FloatAssert}
     */
    public FloatAssert floatOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return Assertions.assertFloat(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link Integer} result
     * so that assertions can be performed on the resulting {@link IntegerAssert}
     */
    public IntegerAssert integerOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return Assertions.assertInteger(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link JSONArray} result
     * so that assertions can be performed on the resulting {@link JSONArrayAssert}
     */
    public JSONArrayAssert jsonArrayOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return Assertions.assertJSONArray(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link JSONObject} result
     * so that assertions can be performed on the resulting {@link JSONObjectAssert}
     */
    public JSONObjectAssert jsonObjectOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return Assertions.assertJSONObject(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link List} result
     * so that assertions can be performed on the resulting {@link ListAssert}
     */
    public ListAssert listOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return Assertions.assertList(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link Long} result
     * so that assertions can be performed on the resulting {@link LongAssert}
     */
    public LongAssert longOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return Assertions.assertLong(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link Map} result
     * so that assertions can be performed on the resulting {@link MapAssert}
     */
    public MapAssert mapOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return Assertions.assertMap(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link Short} result
     * so that assertions can be performed on the resulting {@link ShortAssert}
     */
    public ShortAssert shortOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return Assertions.assertShort(value);
    }

    /**
     * Performs the given operation name and arguments on the mbean and asserts that there is a non null {@link String} result
     * so that assertions can be performed on the resulting {@link StringAssert}
     */
    public StringAssert stringOperation(String mbean, String operation, Object... arguments) throws MalformedObjectNameException, J4pException {
        Object value = operationResult(mbean, operation, arguments);
        return Assertions.assertString(value);
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


}
