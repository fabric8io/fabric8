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
package org.fusesource.insight.activemq.audit;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;

public final class ScriptUtils {

    private static final SimpleDateFormat format;
    private static final ObjectMapper mapper;

    static {
        format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        mapper = new ObjectMapper();
        mapper.setSerializationConfig(
                mapper.getSerializationConfig()
                      .withDateFormat(format)
        );
    }

    public static String toIso(Date d) {
        return format.format(d);
    }

    public static String toJson(Object o) {
        try {
            if (o instanceof Collection) {
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                for (Object c : (Collection) o) {
                    if (sb.length() > 1) {
                        sb.append(",");
                    }
                    sb.append(toJson(c));
                }
                sb.append("]");
                return sb.toString();
            } else if (o instanceof Map) {
                StringBuilder sb = new StringBuilder();
                sb.append("{");
                for (Map.Entry<Object, Object> e : ((Map<Object, Object>) o).entrySet()) {
                    if (sb.length() > 1) {
                        sb.append(",");
                    }
                    sb.append(toJson(e.getKey().toString()));
                    sb.append(":");
                    sb.append(toJson(e.getValue()));
                }
                sb.append("}");
                return sb.toString();
            } else if (o == null) {
                return "null";
            } else if (o instanceof Date) {
                return "\"" + toIso((Date) o) + "\"";
            } else {
                return mapper.writeValueAsString(o.toString());
            }
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
