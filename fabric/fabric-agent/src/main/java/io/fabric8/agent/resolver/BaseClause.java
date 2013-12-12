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
package io.fabric8.agent.resolver;

import org.osgi.framework.Version;
import org.osgi.resource.Resource;

import java.util.Map;

/**
 */
public abstract class BaseClause {

    public abstract Resource getResource();

    public abstract String getNamespace();

    public abstract Map<String, String> getDirectives();

    public abstract Map<String, Object> getAttributes();

    @Override
    public String toString() {
        return toString(getResource(), getNamespace(), getAttributes(), getDirectives());
    }

    public static String toString(Resource res, String namespace, Map<String, Object> attrs, Map<String, String> dirs) {
        StringBuilder sb = new StringBuilder();
        if (res != null) {
            sb.append("[").append(res).append("] ");
        }
        sb.append(namespace);
        for (String key : attrs.keySet()) {
            sb.append("; ");
            append(sb, key, attrs.get(key), true);
        }
        for (String key : dirs.keySet()) {
            sb.append("; ");
            append(sb, key, dirs.get(key), false);
        }
        return sb.toString();
    }

    private static void append(StringBuilder sb, String key, Object val, boolean attribute) {
        sb.append(key);
        if (val instanceof Version) {
            sb.append(":Version=");
            sb.append(val);
        } else if (val instanceof Long) {
            sb.append(":Long=");
            sb.append(val);
        } else if (val instanceof Double) {
            sb.append(":Double=");
            sb.append(val);
        } else if (val instanceof Iterable) {
            Iterable it = (Iterable) val;
            String scalar = null;
            for (Object o : it) {
                String ts;
                if (o instanceof String) {
                    ts = "String";
                } else if (o instanceof Long) {
                    ts = "Long";
                } else if (o instanceof Double) {
                    ts = "Double";
                } else if (o instanceof Version) {
                    ts = "Version";
                } else {
                    throw new IllegalArgumentException("Unsupported scalar type: " + o);
                }
                if (scalar == null) {
                    scalar = ts;
                } else if (!scalar.equals(ts)) {
                    throw new IllegalArgumentException("Unconsistent list type for attribute " + key);
                }
            }
            sb.append(":List<").append(scalar).append(">=");
            sb.append("\"");
            boolean first = true;
            for (Object o : it) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append(o.toString().replace("\"", "\\\"").replace(",", "\\,"));
            }
            sb.append("\"");
        } else {
            sb.append(attribute ? "=" : ":=");
            String s = val.toString();
            if (s.matches("[0-9a-zA-Z_\\-.]*")) {
                sb.append(s);
            } else {
                sb.append("\"").append(s.replace("\"", "\\\\")).append("\"");
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseClause)) {
            return false;
        }
        BaseClause that = (BaseClause) o;
        if (!getNamespace().equals(that.getNamespace())) {
            return false;
        }
        if (!getAttributes().equals(that.getAttributes())) {
            return false;
        }
        if (!getDirectives().equals(that.getDirectives())) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = getNamespace().hashCode();
        result = 31 * result + getAttributes().hashCode();
        result = 31 * result + getDirectives().hashCode();
        return result;
    }
}
