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

import java.util.ArrayList;
import java.util.List;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import org.json.simple.JSONObject;

/**
 * A utility that can marshal an MXBean compliant type into
 * a JSONObject and vise versa.
 *
 * It utilises the {@link OpenTypeGenerator}.
 *
 * @author thomas.diesler@jboss.com
 * @since 23-Jun-2014
 */
public final class JSONTypeGenerator {

    // Hide ctor
    private JSONTypeGenerator() {
    }

    public static JSONObject toJSONObject(Object bean) throws OpenDataException {
        return toJSONObject(OpenTypeGenerator.toCompositeData(bean));
    }

    @SuppressWarnings("unchecked")
    public static JSONObject toJSONObject(CompositeData cdata) throws OpenDataException {
        JSONObject jsonObject = new JSONObject();
        CompositeType ctype = cdata.getCompositeType();
        for (String key : ctype.keySet()) {
            OpenType<?> otype = ctype.getType(key);
            if (otype == SimpleType.STRING) {
                String value = (String) cdata.get(key);
                jsonObject.put(key, value);
            } else if (otype instanceof CompositeType) {
                CompositeData value = (CompositeData) cdata.get(key);
                jsonObject.put(key, toJSONObject(value));
            } else {
                throw new OpenDataException("Unsupported type '" + otype + " ' for: " + key);
            }
        }
        return jsonObject;
    }

    public static <T extends Object> T fromJSONObject(Class<T> beanClass, JSONObject jsonObject) throws OpenDataException {
        CompositeType ctype = OpenTypeGenerator.getCompositeType(beanClass);
        CompositeData cdata = getCompositeData(ctype, jsonObject);
        return OpenTypeGenerator.fromCompositeData(beanClass, cdata);
    }

    private static CompositeData getCompositeData(CompositeType compositeType, JSONObject jsonObject) throws OpenDataException {
        List<String> keys = new ArrayList<>(compositeType.keySet());
        List<Object> items = new ArrayList<>();
        for (String key : keys) {
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                CompositeType itemType = (CompositeType) compositeType.getType(key);
                items.add(getCompositeData(itemType, (JSONObject) value));
            } else {
                items.add(value);
            }
        }
        String[] names = keys.toArray(new String[keys.size()]);
        return new CompositeDataSupport(compositeType, names, items.toArray(new Object[items.size()]));
    }
}
