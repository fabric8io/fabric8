/*
 * #%L
 * Fabric8 :: SPI
 * %%
 * Copyright (C) 2014 Red Hat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.fabric8.jolokia.client;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.jboss.gravia.utils.IllegalStateAssertion;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * A utility that can unmarshal a JSON type into an MXBean compliant type.
 *
 * @author thomas.diesler@jboss.com
 * @since 23-Jun-2014
 */
public final class JSONTypeGenerator {

    // Hide ctor
    private JSONTypeGenerator() {
    }

    public static Object toOpenData(OpenType<?> otype, ClassLoader classLoader, Object value) throws OpenDataException {
        Object result;
        if (otype instanceof CompositeType && value instanceof JSONObject) {
            result = fromJSONObject((CompositeType) otype, classLoader, (JSONObject) value);    
        } else if (otype instanceof TabularType && value instanceof JSONObject) {
            result = fromJSONObject((TabularType) otype, classLoader, (JSONObject) value);    
        } else if (otype instanceof ArrayType && value instanceof JSONArray) {
            result = fromJSONArray((ArrayType<?>) otype, classLoader, (JSONArray) value);    
        } else {
            result = OpenTypeGenerator.toOpenData(otype, value);
        }
        boolean isAssignable = result == null || otype.isValue(result);
        IllegalStateAssertion.assertTrue(isAssignable, "Value " + result + " is not a value of: " + otype);
        return result;
    }
    
    private static Object fromJSONArray(ArrayType<?> atype, ClassLoader classLoader, JSONArray value) throws OpenDataException {
        Object result;
        OpenType<?> elementType = atype.getElementOpenType();
        if (elementType instanceof CompositeType) {
            Object targetArray = OpenTypeGenerator.getOpenTypeArray(atype, classLoader, value.size());
            for (int i = 0; i < value.size(); i++) {
                Object val = toOpenData(elementType, classLoader, value.get(i));
                Array.set(targetArray, i, val);
            }
            result = targetArray;
        } else {
            result = OpenTypeGenerator.toOpenData(atype, value);
        }
        return result;
    }
    
    private static CompositeData fromJSONObject(CompositeType ctype, ClassLoader classLoader, JSONObject value) throws OpenDataException {
        List<String> keys = new ArrayList<>(ctype.keySet());
        List<Object> items = new ArrayList<>();
        for (String key : keys) {
            OpenType<?> itemType = ctype.getType(key);
            Object item = value.get(key);
            items.add(toOpenData(itemType, classLoader, item));
        }
        String[] names = keys.toArray(new String[keys.size()]);
        return new CompositeDataSupport(ctype, names, items.toArray(new Object[items.size()]));
    }

    
    private static TabularData fromJSONObject(TabularType ttype, ClassLoader classLoader, JSONObject value) throws OpenDataException {
        TabularDataSupport tdata = new TabularDataSupport(ttype);
        CompositeType rowType = ttype.getRowType();
        OpenType<?> keyType = rowType.getType("key");
        OpenType<?> valueType = rowType.getType("value");
        for (Object jsonKey : value.keySet()) {
            Map<String, Object> rowData = new HashMap<>();
            Object keyData = toOpenData(keyType, classLoader, jsonKey);
            Object valData = toOpenData(valueType, classLoader, value.get(jsonKey));
            rowData.put("key", keyData);
            rowData.put("value", valData);
            tdata.put(OpenTypeGenerator.toCompositeData(rowType, rowData));
        }
        return tdata;
    }
}
