/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.insight.metrics.support;

import org.codehaus.jackson.map.ObjectMapper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public final class ScriptUtils {

    private static final SimpleDateFormat format;
    private static final ObjectMapper mapper;

    static {
        format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        mapper = new ObjectMapper();
        mapper.setSerializationConfig(mapper.getSerializationConfig().withDateFormat(format));
    }

    public static String toIso(Date d) {
        return format.format(d);
    }

    public static String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not serialize " + o, e);
        }
    }

    public static Map parseJson(String str) {
        try {
            return mapper.readValue(str, Map.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not deserialize " + str, e);
        }
    }

}
