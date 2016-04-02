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
import org.assertj.core.api.ShortAssert;
import org.assertj.core.api.StringAssert;
import org.jolokia.client.J4pClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Provides access to the assertThat() functions for creating asserts on Jolokia
 */
public class Assertions extends org.assertj.core.api.Assertions {

    public static JolokiaAssert assertThat(J4pClient client) {
        return new JolokiaAssert(client);
    }

    public static <T> T asInstanceOf(Object value, Class<T> clazz) {
        assertThat(value).isInstanceOf(clazz);
        return clazz.cast(value);
    }

    public static BigDecimalAssert assertBigDecimal(Object value) {
        BigDecimal typedValue = asInstanceOf(value, BigDecimal.class);
        return (BigDecimalAssert) assertThat(typedValue);
    }

    public static BooleanAssert assertBoolean(Object value) {
        Boolean typedValue = asInstanceOf(value, Boolean.class);
        return (BooleanAssert) assertThat(typedValue);
    }

    public static ByteAssert assertByte(Object value) {
        Byte typedValue = asInstanceOf(value, Byte.class);
        return (ByteAssert) assertThat(typedValue);
    }

    public static CharacterAssert assertCharacter(Object value) {
        Character typedValue = asInstanceOf(value, Character.class);
        return (CharacterAssert) assertThat(typedValue);
    }

    public static DateAssert assertDate(Object value) {
        Date typedValue = asInstanceOf(value, Date.class);
        return (DateAssert) assertThat(typedValue);
    }

    public static DoubleAssert assertDouble(Object value) {
        Double typedValue = asInstanceOf(value, Double.class);
        return (DoubleAssert) assertThat(typedValue);
    }

    public static FloatAssert assertFloat(Object value) {
        Float typedValue = asInstanceOf(value, Float.class);
        return (FloatAssert) assertThat(typedValue);
    }

    public static IntegerAssert assertInteger(Object value) {
        Integer typedValue = asInstanceOf(value, Integer.class);
        return (IntegerAssert) assertThat(typedValue);
    }

    public static JSONArrayAssert assertJSONArray(Object value) {
        JSONArray typedValue = asInstanceOf(value, JSONArray.class);
        return new JSONArrayAssert(typedValue);
    }

    public static JSONObjectAssert assertJSONObject(Object value) {
        JSONObject typedValue = asInstanceOf(value, JSONObject.class);
        return new JSONObjectAssert(typedValue);
    }

    public static ListAssert assertList(Object value) {
        List typedValue = asInstanceOf(value, List.class);
        return (ListAssert) assertThat(typedValue);
    }

    public static LongAssert assertLong(Object value) {
        Long typedValue = asInstanceOf(value, Long.class);
        return (LongAssert) assertThat(typedValue);
    }

    public static MapAssert assertMap(Object value) {
        Map typedValue = asInstanceOf(value, Map.class);
        return (MapAssert) assertThat(typedValue);
    }

    public static ShortAssert assertShort(Object value) {
        Short typedValue = asInstanceOf(value, Short.class);
        return (ShortAssert) assertThat(typedValue);
    }

    public static StringAssert assertString(Object value) {
        String typedValue = asInstanceOf(value, String.class);
        return (StringAssert) assertThat(typedValue);
    }
}
