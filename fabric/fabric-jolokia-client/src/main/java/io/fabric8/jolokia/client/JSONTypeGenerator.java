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
package io.fabric8.jolokia.client;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.TabularType;

import io.fabric8.api.gravia.IllegalStateAssertion;
import org.jolokia.converter.Converters;
import org.jolokia.converter.json.JsonConvertOptions;
import org.jolokia.converter.json.ObjectToJsonConverter;
import org.jolokia.converter.object.StringToOpenTypeConverter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * A utility that can marshal a JSON type into an MXBean compliant type and vice versa.
 *
 * @author thomas.diesler@jboss.com
 * @since 23-Jun-2014
 */
public final class JSONTypeGenerator {

    private static Converters converters = new Converters();
    
    // Hide ctor
    private JSONTypeGenerator() {
    }

    public static Object toJSON(Object value) throws OpenDataException {
        Object result = value;
        try {
            ObjectToJsonConverter converter = converters.getToJsonConverter();
            result = converter.convertToJson(value, null, JsonConvertOptions.DEFAULT);
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            OpenDataException odex = new OpenDataException("Cannot convert value: " + value.getClass().getName());
            odex.initCause(ex);
            throw odex;
        }
        return result;
    }
    
    public static Object toOpenData(OpenType<?> otype, ClassLoader classLoader, Object value) throws OpenDataException {
        Object result;
        if (otype instanceof CompositeType && value instanceof JSONObject) {
            StringToOpenTypeConverter converter = converters.getToOpenTypeConverter();
            result = converter.convertToObject(otype, value);
        } else if (otype instanceof TabularType && value instanceof JSONObject) {
            StringToOpenTypeConverter converter = converters.getToOpenTypeConverter();
            result = converter.convertToObject(otype, value);
        } else if (otype instanceof ArrayType && value instanceof JSONArray) {
            StringToOpenTypeConverter converter = converters.getToOpenTypeConverter();
            result = converter.convertToObject(otype, value);
        } else {
            result = OpenTypeGenerator.toOpenData(otype, value);
        }
        boolean isAssignable = result == null || otype.isValue(result);
        IllegalStateAssertion.assertTrue(isAssignable, "Value " + result + " is not a value of: " + otype);
        return result;
    }
}
