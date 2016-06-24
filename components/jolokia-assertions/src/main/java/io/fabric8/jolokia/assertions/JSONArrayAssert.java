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
import org.json.simple.JSONArray;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * For making assertions on the given {@link org.json.simple.JSONArray} value
 */
public class JSONArrayAssert extends ListAssert {
    public JSONArrayAssert(JSONArray actual) {
        super(actual);
    }

    /**
     * Returns the actual underlying value
     */
    public JSONArray get() {
        return (JSONArray) actual;
    }

    /**
     * Returns an assertion on the size of the collection
     */
    public IntegerAssert assertSize() {
        return (IntegerAssert) assertThat(get().size()).as("size");
    }

    /**
     * Asserts that there is a value at the given index returning the assertion object so that further assertions can be chained
     */
    public ObjectAssert assertObject(int index) {
        Object value = value(index);
        return (ObjectAssert) assertThat(value);
    }

    /**
     * Asserts that there is a {@link java.math.BigDecimal} at the given index returning the
     * {@link BigDecimalAssert} object so that further assertions can be chained
     */
    public BigDecimalAssert assertBigDecimal(int index) {
        Object value = value(index);
        return Assertions.assertBigDecimal(value);
    }

    /**
     * Asserts that there is a {@link Boolean} at the given index returning the
     * {@link BooleanAssert} object so that further assertions can be chained
     */
    public BooleanAssert assertBoolean(int index) {
        Object value = value(index);
        return Assertions.assertBoolean(value);
    }

    /**
     * Asserts that there is a {@link Byte} at the given index returning the
     * {@link ByteAssert} object so that further assertions can be chained
     */
    public ByteAssert assertByte(int index) {
        Object value = value(index);
        return Assertions.assertByte(value);
    }

    /**
     * Asserts that there is a {@link java.util.Date} at the given index returning the
     * {@link DateAssert} object so that further assertions can be chained
     */
    public DateAssert assertDate(int index) {
        Object value = value(index);
        return Assertions.assertDate(value);
    }

    /**
     * Asserts that there is a {@link Double} at the given index returning the
     * {@link DoubleAssert} object so that further assertions can be chained
     */
    public DoubleAssert assertDouble(int index) {
        Object value = value(index);
        return Assertions.assertDouble(value);
    }

    /**
     * Asserts that there is a {@link Float} at the given index returning the
     * {@link FloatAssert} object so that further assertions can be chained
     */
    public FloatAssert assertFloat(int index) {
        Object value = value(index);
        return Assertions.assertFloat(value);
    }

    /**
     * Asserts that there is a {@link Integer} at the given index returning the
     * {@link IntegerAssert} object so that further assertions can be chained
     */
    public IntegerAssert assertInteger(int index) {
        Object value = value(index);
        return Assertions.assertInteger(value);
    }

    /**
     * Asserts that there is a {@link org.json.simple.JSONArray} at the given index returning the
     * {@link JSONArrayAssert} object so that further assertions can be chained
     */
    public JSONArrayAssert assertJSONArray(int index) {
        Object value = value(index);
        return Assertions.assertJSONArray(value);
    }

    /**
     * Asserts that there is a {@link org.json.simple.JSONObject} at the given index returning the
     * {@link JSONObjectAssert} object so that further assertions can be chained
     */
    public JSONObjectAssert assertJSONObject(int index) {
        Object value = value(index);
        return Assertions.assertJSONObject(value);
    }


    /**
     * Asserts that there is a {@link java.util.List} at the given index returning the
     * {@link ListAssert} object so that further assertions can be chained
     */
    public ListAssert assertList(int index) {
        Object value = value(index);
        return Assertions.assertList(value);
    }

    /**
     * Asserts that there is a {@link Long} at the given index returning the
     * {@link LongAssert} object so that further assertions can be chained
     */
    public LongAssert assertLong(int index) {
        Object value = value(index);
        return Assertions.assertLong(value);
    }

    /**
     * Asserts that there is a {@link java.util.Map} at the given index returning the
     * {@link MapAssert} object so that further assertions can be chained
     */
    public MapAssert assertMap(int index) {
        Object value = value(index);
        return Assertions.assertMap(value);
    }

    /**
     * Asserts that there is a {@link Short} at the given index returning the
     * {@link ShortAssert} object so that further assertions can be chained
     */
    public ShortAssert assertShort(int index) {
        Object value = value(index);
        return Assertions.assertShort(value);
    }

    /**
     * Asserts that there is a {@link String} at the given index returning the
     * {@link StringAssert} object so that further assertions can be chained
     */
    public StringAssert assertString(int index) {
        Object value = value(index);
        return Assertions.assertString(value);
    }


    /**
     * Returns the value at the given index
     */
    public Object value(int index) {
        JSONArray array = get();
        assertThat(array.size()).as("size of array").isGreaterThan(index);
        return array.get(index);
    }
}
