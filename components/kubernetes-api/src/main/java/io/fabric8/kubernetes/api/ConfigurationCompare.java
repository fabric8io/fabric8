/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.api;

import io.fabric8.utils.Objects;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper methods to compare the user configuration on entities
 */
public class ConfigurationCompare {
    public static boolean configEqual(Map<String, String> entity1, Map<String, String> entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null || entity2 == null) {
            return false;
        }
        int size1 = size(entity1);
        int size2 = size(entity2);
        if (size1 != size2) {
            return false;
        }
        Set<Map.Entry<String, String>> entries = entity1.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            String value = entry.getValue();
            String value2 = entity2.get(key);
            if (!configEqual(value, value2)) {
                return false;
            }
        }
        return true;
    }

    public static boolean configEqual(List v1, List v2) {
        int size1 = size(v1);
        int size2 = size(v2);
        if (size1 != size2) {
            return false;
        }
        int idx = 0;
        for (Object value : v1) {
            Object value2 = v2.get(idx++);
            if (!configEqual(value, value2)) {
                return false;
            }
        }
        return true;
    }


    protected static int size(Map map) {
        return (map == null) ? 0 : map.size();
    }

    protected static int size(Collection coll) {
        return (coll == null) ? 0 : coll.size();
    }

    public static boolean configEqual(Object entity1, Object entity2) {
        return Objects.equal(entity1, entity2);
    }
}
