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
import org.json.simple.JSONObject;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * For making assertions on the given {@link JSONObject} value
 */
public class JSONObjectAssert extends ObjectAssert<JSONObject> {
    public JSONObjectAssert(JSONObject actual) {
        super(actual);
    }

    /**
     * Returns the actual underlying value
     */
    public JSONObject get() {
        return actual;
    }

    /**
     * Returns an assertion on the size of the collection
     */
    public IntegerAssert assertSize() {
        return (IntegerAssert) assertThat(get().size()).as("size");
    }

    /**
     * Asserts that there is a value at the given key returning the assertion object so that further assertions can be chained
     */
    public ObjectAssert assertObject(String key) {
        Object value = value(key);
        return (ObjectAssert) assertThat(value);
    }

    /**
     * Asserts that there is a {@link java.math.BigDecimal} value for the given key returning the
     * {@link BigDecimalAssert} object so that further assertions can be chained
     */
    public BigDecimalAssert assertBigDecimal(String key) {
        Object value = value(key);
        return Assertions.assertBigDecimal(value);
    }

    /**
     * Asserts that there is a {@link Boolean} value for the given key returning the
     * {@link BooleanAssert} object so that further assertions can be chained
     */
    public BooleanAssert assertBoolean(String key) {
        Object value = value(key);
        return Assertions.assertBoolean(value);
    }

    /**
     * Asserts that there is a {@link Byte} value for the given key returning the
     * {@link ByteAssert} object so that further assertions can be chained
     */
    public ByteAssert assertByte(String key) {
        Object value = value(key);
        return Assertions.assertByte(value);
    }

    /**
     * Asserts that there is a {@link Character} value for the given key returning the
     * {@link CharacterAssert} object so that further assertions can be chained
     */
    public CharacterAssert assertCharacter(String key) {
        Object value = value(key);
        return Assertions.assertCharacter(value);
    }

    /**
     * Asserts that there is a {@link java.util.Date} value for the given key returning the
     * {@link DateAssert} object so that further assertions can be chained
     */
    public DateAssert assertDate(String key) {
        Object value = value(key);
        return Assertions.assertDate(value);
    }

    /**
     * Asserts that there is a {@link Double} value for the given key returning the
     * {@link DoubleAssert} object so that further assertions can be chained
     */
    public DoubleAssert assertDouble(String key) {
        Object value = value(key);
        return Assertions.assertDouble(value);
    }

    /**
     * Asserts that there is a {@link Float} value for the given key returning the
     * {@link FloatAssert} object so that further assertions can be chained
     */
    public FloatAssert assertFloat(String key) {
        Object value = value(key);
        return Assertions.assertFloat(value);
    }

    /**
     * Asserts that there is a {@link Integer} value for the given key returning the
     * {@link IntegerAssert} object so that further assertions can be chained
     */
    public IntegerAssert assertInteger(String key) {
        Object value = value(key);
        return Assertions.assertInteger(value);
    }

    /**
     * Asserts that there is a {@link org.json.simple.JSONObject} value for the given key returning the
     * {@link io.fabric8.jolokia.assertions.JSONObjectAssert} object so that further assertions can be chained
     */
    public JSONArrayAssert assertJSONArray(String key) {
        Object value = value(key);
        return Assertions.assertJSONArray(value);
    }

    /**
     * Asserts that there is a {@link org.json.simple.JSONObject} value for the given key returning the
     * {@link io.fabric8.jolokia.assertions.JSONObjectAssert} object so that further assertions can be chained
     */
    public JSONObjectAssert assertJSONObject(String key) {
        Object value = value(key);
        return Assertions.assertJSONObject(value);
    }

    /**
     * Asserts that there is a {@link java.util.List} value for the given key returning the
     * {@link ListAssert} object so that further assertions can be chained
     */
    public ListAssert assertList(String key) {
        Object value = value(key);
        return Assertions.assertList(value);
    }

    /**
     * Asserts that there is a {@link Long} value for the given key returning the
     * {@link LongAssert} object so that further assertions can be chained
     */
    public LongAssert assertLong(String key) {
        Object value = value(key);
        return Assertions.assertLong(value);
    }

    /**
     * Asserts that there is a {@link java.util.Map} value for the given key returning the
     * {@link MapAssert} object so that further assertions can be chained
     */
    public MapAssert assertMap(String key) {
        Object value = value(key);
        return Assertions.assertMap(value);
    }

    /**
     * Asserts that there is a {@link Short} value for the given key returning the
     * {@link ShortAssert} object so that further assertions can be chained
     */
    public ShortAssert assertShort(String key) {
        Object value = value(key);
        return Assertions.assertShort(value);
    }

    /**
     * Asserts that there is a {@link String} value for the given key returning the
     * {@link StringAssert} object so that further assertions can be chained
     */
    public StringAssert assertString(String key) {
        Object value = value(key);
        return Assertions.assertString(value);
    }

    /**
     * Returns the value for the given key
     */
    public Object value(String key) {
        JSONObject value = get();
        assertThat(value.size()).isGreaterThan(0);
        return value.get(key);
    }
}
