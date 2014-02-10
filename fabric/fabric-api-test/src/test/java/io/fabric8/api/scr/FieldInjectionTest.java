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
package io.fabric8.api.scr;

import io.fabric8.api.scr.support.ConverterHelper;
import io.fabric8.api.scr.support.ReflectionHelper;
import io.fabric8.api.test.DummyComponent;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 */
public class FieldInjectionTest {
    private static final transient Logger LOG = LoggerFactory.getLogger(FieldInjectionTest.class);

    protected String name = "James";
    private String[] stringArray = {"sa1", "sa2", "sa3"};
    private List<String> listArray = Arrays.asList("la1", "la2");
    private String[] stringArrayD = {"list2"};
    private List<String> listArrayD = Arrays.asList("array2");
    private boolean propBool = true;
    private boolean propBoolDefault = true;
    private Boolean propBoolean = true;
    private Boolean propBoolean2 = false;
    private Boolean propBooleanDefault = true;
    private byte propByte = 23;
    private byte propByteDefault = DummyComponent.DEFAULT_BYTE;
    private char propChar = 'J';
    private char propCharDefault = DummyComponent.DEFAULT_CHAR;
    private short propShort = 1234;
    private short propShortDefault = DummyComponent.DEFAULT_SHORT;
    private int propInt = 1234567;
    private int propIntDefault = DummyComponent.DEFAULT_INT;
    private Integer propInteger = 45678910;
    private Integer propIntegerDefault = DummyComponent.DEFAULT_INT;
    private long propLong = 12345678901234L;
    private long propLongDefault = DummyComponent.DEFAULT_LONG;
    private float propFloat = 3.149f;
    private float propFloatDefault = DummyComponent.DEFAULT_FLOAT;
    private double propDouble = 9.999999999d;
    private double propDoubleDefault = DummyComponent.DEFAULT_DOUBLE;
    private Double propDoubleObject = 3.33333333333d;
    private Double propDoubleObjectDefault = DummyComponent.DEFAULT_DOUBLE;


    // the fields injected without defaults
    protected String[] fieldNames = {
            "name",
            "stringArray",
            "listArray",
            "propBool",
            "propBoolean",
            "propBoolean2",
            "propByte",
            "propChar",
            "propShort",
            "propInt",
            "propInteger",
            "propLong",
            "propFloat",
            "propDouble",
            "propDoubleObject"
    };

    // the fields which are injected with defaults from the annotation
    protected String[] defaultPropertyNames = {
            "stringArrayD",
            "listArrayD",
            "propBoolDefault",
            "propBooleanDefault",
            "propByteDefault",
            "propCharDefault",
            "propShortDefault",
            "propIntDefault",
            "propIntegerDefault",
            "propLongDefault",
            "propFloatDefault",
            "propDoubleDefault",
            "propDoubleObjectDefault"
    };

    @Test
    public void testProperty() throws Exception {
        DummyComponent component = new DummyComponent();
        Map<String, String> config = new HashMap<String, String>();

        for (String fieldName : fieldNames) {
            Object value = getFieldValue(this, fieldName);
            addConfigurationValue(config, fieldName, value);
        }

        for (String fieldName : defaultPropertyNames) {
            Object value = getFieldValue(this, fieldName);
            addConfigurationValue(config, fieldName, value);
        }

        LOG.info("Properties: " + config);

        component.activate(config);

        LOG.info("Have injected: " + component);

        assertFieldValues(fieldNames, this, component);
        assertFieldValues(defaultPropertyNames, this, component);
    }

    protected static Object getFieldValue(Object instance, String fieldName) {
        Field declaredField = null;
        try {
            Class<?> clazz = instance.getClass();
            declaredField = clazz.getDeclaredField(fieldName);
        } catch (Exception e) {
            fail("Could not find field: " + fieldName + " in " + instance);
        }
        return ReflectionHelper.getField(declaredField, instance);
    }

    protected void assertFieldValues(String[] fieldNames, Object expectedFields, Object actualFields) {
        for (String fieldName : fieldNames) {
            Object expected = getFieldValue(expectedFields, fieldName);
            Object actual = getFieldValue(actualFields, fieldName);
            assertEquals("field " + fieldName, convertArrayOrCollectionToString(expected), convertArrayOrCollectionToString(actual));
        }
    }

    protected Object convertArrayOrCollectionToString(Object value) {
        if (value != null) {
            if (value.getClass().isArray() || value instanceof Collection) {
                return getStringValue(value);
            }
        }
        return value;
    }

    protected static void addConfigurationValue(Map<String, String> properties, String name, Object value) {
        if (value != null) {
            String text = getStringValue(value);
            properties.put(name, text);
        } else {
            properties.remove(name);
        }
    }

    protected static String getStringValue(Object value) {
        Class<?> aClass = value.getClass();
        String text;
        if (aClass.isArray()) {
            StringBuilder builder = new StringBuilder();
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                addStringValue(builder, Array.get(value, i));
            }
            text = builder.toString();
        } else if (value instanceof Collection) {
            StringBuilder builder = new StringBuilder();
            Collection collection = (Collection) value;
            for (Object item : collection) {
                addStringValue(builder, item);
            }
            text = builder.toString();
        } else {
            text = value.toString();
        }
        return text;
    }

    private static void addStringValue(StringBuilder builder, Object item) {
        String text = getStringValue(item);
        if (builder.length() > 0) {
            builder.append(ConverterHelper.VALUE_SEPARATOR);
        }
        builder.append(text);
    }

}
